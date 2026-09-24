package com.manna.bible.data.remote

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest

/**
 * Identifies Manna to Google APIs so the Gemini API key can be locked to this app.
 *
 * The key ships inside the APK (every Android client key does), so anyone can extract it.
 * Google Cloud lets a key be restricted to "Android apps" — requests are then accepted
 * only when they carry this app's package name and signing-certificate SHA-1 in the
 * `X-Android-Package` / `X-Android-Cert` headers. Google's client SDKs add those headers
 * automatically; Manna calls the REST API directly, so this interceptor adds them.
 *
 * With an unrestricted key the headers are simply ignored, so this is safe to ship before
 * the restriction is switched on in the Cloud console.
 */
class GoogleApiAppIdentityInterceptor(
    private val packageName: String,
    certificateSha1: () -> String?
) : Interceptor {

    /** Resolved once, on the first request (an OkHttp thread, never the main thread). */
    private val certSha1: String? by lazy(certificateSha1)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(HEADER_PACKAGE, packageName)
            .apply { certSha1?.let { header(HEADER_CERT, it) } }
            .build()
        return chain.proceed(request)
    }

    companion object {
        const val HEADER_PACKAGE = "X-Android-Package"
        const val HEADER_CERT = "X-Android-Cert"

        /** Builds the interceptor for the running app, reading its signing certificate. */
        fun forApp(context: Context): GoogleApiAppIdentityInterceptor {
            val appContext = context.applicationContext
            return GoogleApiAppIdentityInterceptor(appContext.packageName) {
                signingCertificateSha1(appContext)
            }
        }

        /**
         * Uppercase hex SHA-1 (no colons) of the current signing certificate — the format
         * Google expects in `X-Android-Cert`. Null if it can't be read.
         */
        @Suppress("DEPRECATION")
        private fun signingCertificateSha1(context: Context): String? = runCatching {
            val pm = context.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo?.apkContentsSigners
            } else {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
            }
            val cert = signatures?.firstOrNull() ?: return@runCatching null
            sha1Hex(cert.toByteArray())
        }.getOrNull()

        /** Uppercase hex SHA-1 digest of [bytes], without separators. */
        internal fun sha1Hex(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-1").digest(bytes)
                .joinToString(separator = "") { "%02X".format(it) }
    }
}
