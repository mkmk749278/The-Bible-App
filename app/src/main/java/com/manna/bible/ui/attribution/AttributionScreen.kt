package com.manna.bible.ui.attribution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.domain.attribution.TranslationLicense
import com.manna.bible.ui.theme.MannaColors

private val MinTouchTarget = 48.dp

/**
 * Attribution & about surface (Requirement 12). Shows the active translation's
 * attribution notice (Req 12.1, 12.2, 12.4) and an always-present acknowledgement
 * of the Free Use Bible API (MIT) regardless of which translations are in use
 * (Req 12.3).
 *
 * @param onBack returns to the reader.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributionScreen(
    modifier: Modifier = Modifier,
    viewModel: AttributionViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.attribution_back)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.attribution_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(MinTouchTarget)
                            .semantics { contentDescription = backDescription }
                    ) {
                        Text(text = "‹", fontSize = 26.sp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AttributionSection(
                title = stringResource(R.string.attribution_active_translation),
                body = activeTranslationNotice(
                    name = state.attribution.translationName,
                    license = state.attribution.license
                )
            )
            AttributionSection(
                title = stringResource(R.string.attribution_api_title),
                body = stringResource(R.string.attribution_api_body)
            )
            AttributionSection(
                title = stringResource(R.string.attribution_app_title),
                body = stringResource(R.string.attribution_app_body)
            )
        }
    }
}

/** Localizes the active-translation attribution line (Req 12.2, 12.4). */
@Composable
private fun activeTranslationNotice(name: String?, license: TranslationLicense?): String {
    if (name == null || license == null) {
        return stringResource(R.string.attribution_no_translation)
    }
    return when (license) {
        TranslationLicense.PUBLIC_DOMAIN -> stringResource(R.string.attribution_public_domain, name)
        TranslationLicense.SOURCE_PROVIDED -> stringResource(R.string.attribution_source_provided, name)
    }
}

@Composable
private fun AttributionSection(title: String, body: String) {
    Surface(
        color = MannaColors.card,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MannaColors.gold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MannaColors.cream
            )
        }
    }
}
