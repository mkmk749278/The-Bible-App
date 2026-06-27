package com.manna.bible.localization

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Filesystem + XML helpers shared by the localization inventory test and the AI-surface
 * source-scan guard. These read the real module sources directly from disk (no Robolectric)
 * so the checks reflect exactly what ships in `app/src/main/res` and `app/src/main/java`.
 */
object LocalizationTestSupport {

    /** The `app` module directory, resolved whether tests run from the module or repo root. */
    val moduleDir: File by lazy {
        val candidates = listOf(File("."), File("app"), File("../app"))
        candidates.firstOrNull { File(it, "src/main/res").isDirectory }
            ?.canonicalFile
            ?: error("Could not locate the app module (expected src/main/res under one of $candidates)")
    }

    private val resDir: File get() = File(moduleDir, "src/main/res")

    /** All `strings*.xml` files in a given values directory (e.g. "values", "values-ta"). */
    fun stringFiles(valuesDir: String): List<File> {
        val dir = File(resDir, valuesDir)
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles { f -> f.name.startsWith("strings") && f.name.endsWith(".xml") }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    /** The `<string name=...>` entries (name -> text) across every `strings*.xml` in [valuesDir]. */
    fun readStrings(valuesDir: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (file in stringFiles(valuesDir)) {
            out.putAll(readStringsFromFile(file))
        }
        return out
    }

    /** The `<string name=...>` entries (name -> text) from a single XML [file]. */
    fun readStringsFromFile(file: File): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance()
        val doc = factory.newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val out = LinkedHashMap<String, String>()
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as Element
            val name = el.getAttribute("name")
            if (name.isNotBlank()) out[name] = el.textContent
        }
        return out
    }

    /** Read a main-source Kotlin file under `src/main/java/com/manna/bible/...`. */
    fun readMainSource(relativePath: String): String {
        val file = File(moduleDir, "src/main/java/com/manna/bible/$relativePath")
        require(file.isFile) { "Expected source file not found: ${file.path}" }
        return file.readText()
    }
}
