package dev.busung.s25uroot

import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.busung.s25uroot.ui.theme.RootMyGalaxyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InstallActivity : ComponentActivity() {
    private val installViewModel by viewModels<InstallViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        val localPayloadUris = readLocalPayloadUris(intent)
        // -1 sentinel means the extra was not set; any other value is an explicit override.
        val rebootUserspaceExtra = intent.getIntExtra(EXTRA_REBOOT_USERSPACE, -1)
        val rebootUserspaceOverride: Boolean? = when (rebootUserspaceExtra) {
            0 -> false
            1 -> true
            else -> null
        }
        val startInstall = savedInstanceState == null && AppPreferences.consumeInstallRequest(
            this,
            intent.getStringExtra(EXTRA_INSTALL_REQUEST_ID),
        )
        intent.removeExtra(EXTRA_INSTALL_REQUEST_ID)
        setContent {
            RootMyGalaxyTheme(
                accentColor = AppPreferences.accentColor(this),
                themeMode = AppPreferences.themeMode(this),
            ) {
                val installState by installViewModel.state.collectAsStateWithLifecycle()
                BackHandler(enabled = installState.busy) {}
                LaunchedEffect(startInstall, profileId, localPayloadUris) {
                    if (startInstall) {
                        installViewModel.setLocalPayloadUris(localPayloadUris)
                        installViewModel.installByProfileId(profileId, rebootUserspaceOverride)
                    }
                }
                InstallScreen(
                    installState = installState,
                    onRetry = {
                        installViewModel.setLocalPayloadUris(localPayloadUris)
                        installViewModel.installByProfileId(profileId, rebootUserspaceOverride)
                    },
                    onClose = ::finish,
                )
            }
        }
    }

    private fun readLocalPayloadUris(intent: android.content.Intent): Map<String, android.net.Uri> = buildMap {
        listOf(InstallViewModel.PAYLOAD_EXPLOIT, InstallViewModel.PAYLOAD_KERNELSU).forEach { key ->
            intent.getStringExtra(EXTRA_LOCAL_PAYLOAD_PREFIX + key)
                ?.let { value -> android.net.Uri.parse(value) }
                ?.let { uri -> put(key, uri) }
        }
    }

    companion object {
        const val EXTRA_INSTALL_REQUEST_ID = "install_request_id"
        const val EXTRA_PROFILE_ID = "profile_id"
        const val EXTRA_LOCAL_PAYLOAD_PREFIX = "local_payload_"
        /**
         * Optional int extra: 1 = reboot userspace after install, 0 = do not reboot,
         * absent (default -1) = defer to the global [AppPreferences.rebootAfterInstall] pref.
         */
        const val EXTRA_REBOOT_USERSPACE = "reboot_userspace"
    }
}

@Composable
fun InstallScreen(
    installState: InstallUiState,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val phaseIcon: ImageVector = when (installState.phase) {
                    InstallPhase.Installed -> Icons.Rounded.Check
                    InstallPhase.Failed    -> Icons.Rounded.Error
                    InstallPhase.Downloading -> Icons.Rounded.CloudDownload
                    InstallPhase.Exploiting,
                    InstallPhase.LoadingKernelSu -> Icons.Rounded.Memory
                    else -> Icons.Rounded.Info
                }
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = when (installState.phase) {
                        InstallPhase.Installed -> MaterialTheme.colorScheme.primaryContainer
                        InstallPhase.Failed    -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (installState.busy) {
                            LoadingIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        } else {
                            Icon(
                                phaseIcon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (installState.phase) {
                            InstallPhase.Checking       -> stringResource(R.string.install_phase_checking)
                            InstallPhase.Downloading    -> stringResource(R.string.install_phase_downloading)
                            InstallPhase.Exploiting     -> stringResource(R.string.install_phase_exploiting)
                            InstallPhase.LoadingKernelSu -> stringResource(R.string.install_phase_loading_ksu)
                            InstallPhase.Installed      -> stringResource(R.string.install_phase_installed)
                            InstallPhase.Failed         -> stringResource(R.string.install_phase_failed)
                            InstallPhase.Ready          -> stringResource(R.string.install_phase_ready)
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (installState.message.isNotBlank()) {
                        Text(
                            text = installState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!installState.busy) {
                    androidx.compose.material3.IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }
            }

            // ── Progress bar while busy ────────────────────────────────────────
            if (installState.busy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // ── Log output ───────────────────────────────────────────────────
            if (installState.log.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                ) {
                    Text(
                        text = installState.log,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // ── Actions ──────────────────────────────────────────────────────
            when (installState.phase) {
                InstallPhase.Installed -> {
                    FilledTonalButton(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.action_close), modifier = Modifier.padding(start = 6.dp))
                    }
                }
                InstallPhase.Failed -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.action_close))
                        }
                        FilledTonalButton(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}
