package dev.busung.s25uroot

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// ---------------------------------------------------------------------------
// Activity
// ---------------------------------------------------------------------------

class MainActivity : ComponentActivity() {

    private val viewModel: InstallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle deep-link / notification intents that carry a profile id
        handleIntent(intent)

        setContent {
            val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            AppTheme(accentColor = accentColor, themeMode = themeMode) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val profileId = intent
            ?.getStringExtra(EXTRA_PROFILE_ID)
            ?: return
        viewModel.setPendingInstallRequest(profileId)
    }

    companion object {
        const val EXTRA_PROFILE_ID = "dev.busung.s25uroot.PROFILE_ID"
    }
}

// ---------------------------------------------------------------------------
// Root composable
// ---------------------------------------------------------------------------

@Composable
fun MainScreen(viewModel: InstallViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val installHistory by viewModel.installHistory.collectAsStateWithLifecycle()

    // Pending install-request confirmation dialog
    val pendingRequest = uiState.pendingInstallRequest
    if (pendingRequest != null) {
        val profile = uiState.selectedProfile
        InstallConfirmDialog(
            profileName = profile?.displayName ?: pendingRequest,
            source = profile?.exploit?.url ?: pendingRequest,
            onConfirm = {
                viewModel.consumePendingInstallRequest()
                val target = profile ?: return@InstallConfirmDialog
                viewModel.startRoot(target)
            },
            onDismiss = {
                viewModel.consumePendingInstallRequest()
            },
        )
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController = navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "overview",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("overview") {
                OverviewScreen(
                    uiState = uiState,
                    onStartRoot = { viewModel.startRoot() },
                    onStopSession = { viewModel.stopSession() },
                    onSelectProfile = { viewModel.selectProfile(it) },
                    onLoadCatalog = { viewModel.loadTargetCatalog() },
                    catalogState = viewModel.targetCatalog.collectAsStateWithLifecycle().value,
                )
            }
            composable("history") {
                HistoryScreen(
                    history = installHistory,
                    onDelete = { viewModel.deleteHistoryEntry(it.id) },
                    onDeleteAll = { viewModel.deleteAllHistoryEntries() },
                )
            }
            composable("settings") {
                val advancedMode by viewModel.advancedMode.collectAsStateWithLifecycle()
                val shizukuMode by viewModel.shizukuMode.collectAsStateWithLifecycle()
                val autoReroot by viewModel.autoReroot.collectAsStateWithLifecycle()
                val localPayloadMode by viewModel.localPayloadMode.collectAsStateWithLifecycle()
                val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()
                val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                SettingsScreen(
                    advancedMode = advancedMode,
                    onAdvancedModeChange = { viewModel.setAdvancedMode(it) },
                    shizukuMode = shizukuMode,
                    onShizukuModeChange = { viewModel.setShizukuMode(it) },
                    autoReroot = autoReroot,
                    onAutoRerootChange = { viewModel.setAutoReroot(it) },
                    localPayloadMode = localPayloadMode,
                    onLocalPayloadModeChange = { viewModel.setLocalPayloadMode(it) },
                    accentColor = accentColor,
                    onAccentColorChange = { viewModel.setAccentColor(it) },
                    themeMode = themeMode,
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom navigation
// ---------------------------------------------------------------------------

@Composable
fun BottomNavBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    NavigationBar {
        NavigationBarItem(
            selected = current == "overview",
            onClick = { navController.navigate("overview") { launchSingleTop = true } },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_overview)) },
        )
        NavigationBarItem(
            selected = current == "history",
            onClick = { navController.navigate("history") { launchSingleTop = true } },
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_history)) },
        )
        NavigationBarItem(
            selected = current == "settings",
            onClick = { navController.navigate("settings") { launchSingleTop = true } },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
        )
    }
}

// ---------------------------------------------------------------------------
// Overview screen
// ---------------------------------------------------------------------------

@Composable
fun OverviewScreen(
    uiState: InstallUiState,
    onStartRoot: () -> Unit,
    onStopSession: () -> Unit,
    onSelectProfile: (TargetProfile) -> Unit,
    onLoadCatalog: () -> Unit,
    catalogState: TargetCatalogUiState,
) {
    var showProfileSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Device card
        val device = uiState.device
        if (device != null) {
            DeviceCard(
                device = device,
                androidVersion = uiState.androidVersion ?: "",
                securityPatch = uiState.securityPatch ?: "",
            )
        }

        // KernelSU status card
        KernelSuCard(
            isRooted = uiState.isRooted,
            kernelSuVersion = uiState.kernelSuVersion,
        )

        // Selected profile
        val selectedProfile = uiState.selectedProfile
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onLoadCatalog()
                showProfileSheet = true
            },
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.select_device_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = selectedProfile?.displayName
                            ?: stringResource(R.string.select_device_description),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (selectedProfile != null) {
                        Text(
                            text = selectedProfile.supportedModels,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        // Steps
        StepsCard(phase = uiState.phase)

        // Progress / status
        if (uiState.busy) {
            if (uiState.progress != null) {
                LinearProgressIndicator(
                    progress = { uiState.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Text(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Action button
        val phase = uiState.phase
        if (phase == InstallPhase.Done) {
            Button(
                onClick = { /* nothing to do */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            ) { Text(stringResource(R.string.action_done)) }
        } else if (uiState.busy) {
            OutlinedButton(
                onClick = onStopSession,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_cancel)) }
        } else {
            Button(
                onClick = onStartRoot,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.install_tap_start)) }
        }

        // Log output
        if (uiState.log.isNotBlank()) {
            LogCard(log = uiState.log)
        }
    }

    // Profile selection bottom sheet
    if (showProfileSheet) {
        ProfileSelectionSheet(
            catalogState = catalogState,
            onSelect = { profile ->
                onSelectProfile(profile)
                showProfileSheet = false
            },
            onDismiss = { showProfileSheet = false },
        )
    }
}

// ---------------------------------------------------------------------------
// Sub-composables: cards
// ---------------------------------------------------------------------------

@Composable
fun DeviceCard(device: DeviceSnapshot, androidVersion: String, securityPatch: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.device), style = MaterialTheme.typography.titleMedium)
            InfoRow(label = stringResource(R.string.firmware), value = device.model)
            InfoRow(label = stringResource(R.string.kernel), value = device.kernelVersion)
            if (androidVersion.isNotBlank()) {
                InfoRow(label = "Android", value = androidVersion)
            }
            if (securityPatch.isNotBlank()) {
                InfoRow(label = stringResource(R.string.firmware), value = securityPatch)
            }
            InfoRow(label = stringResource(R.string.system_abi), value = device.abi)
        }
    }
}

@Composable
fun KernelSuCard(isRooted: Boolean, kernelSuVersion: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (isRooted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isRooted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error,
            )
            Column {
                Text(
                    stringResource(R.string.kernelsu_card_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (isRooted && kernelSuVersion != null)
                               stringResource(R.string.version_format, kernelSuVersion)
                           else if (isRooted)
                               stringResource(R.string.phase_installed)
                           else
                               stringResource(R.string.status_not_installed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun StepsCard(phase: InstallPhase) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.how_it_works), style = MaterialTheme.typography.titleMedium)
            StepRow(
                title = stringResource(R.string.step_support_title),
                detail = stringResource(R.string.step_support_detail),
                active = phase == InstallPhase.Checking,
                done = phase.ordinal > InstallPhase.Checking.ordinal,
            )
            StepRow(
                title = stringResource(R.string.step_download_title),
                detail = stringResource(R.string.step_download_detail),
                active = phase == InstallPhase.Downloading,
                done = phase.ordinal > InstallPhase.Downloading.ordinal,
            )
            StepRow(
                title = stringResource(R.string.step_exploit_title),
                detail = stringResource(R.string.step_exploit_detail),
                active = phase == InstallPhase.Exploiting,
                done = phase.ordinal > InstallPhase.Exploiting.ordinal,
            )
            StepRow(
                title = stringResource(R.string.step_ksu_title),
                detail = stringResource(R.string.step_ksu_detail),
                active = phase == InstallPhase.LoadingKernelSu,
                done = phase == InstallPhase.Installed || phase == InstallPhase.Done,
            )
        }
    }
}

@Composable
fun StepRow(title: String, detail: String, active: Boolean, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = when {
                done -> Icons.Default.CheckCircle
                active -> Icons.Default.RadioButtonChecked
                else -> Icons.Default.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = when {
                done -> MaterialTheme.colorScheme.primary
                active -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        )
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun LogCard(log: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = log,
            modifier = Modifier.padding(12.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

// ---------------------------------------------------------------------------
// Profile selection sheet
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSelectionSheet(
    catalogState: TargetCatalogUiState,
    onSelect: (TargetProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.select_device_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when {
                catalogState.loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                catalogState.error != null -> {
                    Text(
                        text = catalogState.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                catalogState.profiles.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.no_matching_devices),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyColumn {
                        items(catalogState.profiles) { profile ->
                            ListItem(
                                headlineContent = { Text(profile.displayName) },
                                supportingContent = {
                                    Text(
                                        profile.supportedModels,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                modifier = Modifier.clickable { onSelect(profile) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Install confirmation dialog
// ---------------------------------------------------------------------------

@Composable
fun InstallConfirmDialog(
    profileName: String,
    source: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.install_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.install_confirm_body))
                Text(
                    stringResource(R.string.install_confirm_source, source),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

// ---------------------------------------------------------------------------
// History screen
// ---------------------------------------------------------------------------

@Composable
fun HistoryScreen(
    history: List<InstallHistoryEntry>,
    onDelete: (InstallHistoryEntry) -> Unit,
    onDeleteAll: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleLarge)
            if (history.isNotEmpty()) {
                TextButton(onClick = onDeleteAll) {
                    Text(stringResource(R.string.history_delete_selected))
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        stringResource(R.string.history_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.history_empty_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(history) { entry ->
                    HistoryEntryItem(entry = entry, onDelete = { onDelete(entry) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun HistoryEntryItem(entry: InstallHistoryEntry, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.profileId ?: stringResource(R.string.history_payload),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                val resultLabel = when (entry.result) {
                    InstallRunResult.Succeeded -> stringResource(R.string.history_succeeded)
                    InstallRunResult.Failed -> stringResource(R.string.history_failed)
                    null -> if (entry.completedAtMillis == null)
                        stringResource(R.string.history_running)
                    else
                        stringResource(R.string.history_completed)
                }
                Text(
                    text = resultLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (entry.result) {
                        InstallRunResult.Succeeded -> MaterialTheme.colorScheme.primary
                        InstallRunResult.Failed -> MaterialTheme.colorScheme.error
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.history_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (expanded && entry.log.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.history_log),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = entry.log,
                    modifier = Modifier.padding(8.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Settings screen
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(
    advancedMode: Boolean,
    onAdvancedModeChange: (Boolean) -> Unit,
    shizukuMode: Boolean,
    onShizukuModeChange: (Boolean) -> Unit,
    autoReroot: Boolean,
    onAutoRerootChange: (Boolean) -> Unit,
    localPayloadMode: Boolean,
    onLocalPayloadModeChange: (Boolean) -> Unit,
    accentColor: String,
    onAccentColorChange: (String) -> Unit,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)

        // Advanced section
        SectionHeader(stringResource(R.string.advanced))
        SwitchPreference(
            title = stringResource(R.string.advanced_mode),
            subtitle = stringResource(R.string.advanced_mode_description),
            checked = advancedMode,
            onCheckedChange = onAdvancedModeChange,
        )
        SwitchPreference(
            title = stringResource(R.string.shizuku_mode),
            subtitle = stringResource(R.string.shizuku_mode_description),
            checked = shizukuMode,
            onCheckedChange = onShizukuModeChange,
        )
        SwitchPreference(
            title = stringResource(R.string.settings_reboot_after_install),
            subtitle = stringResource(R.string.settings_reboot_after_install_description),
            checked = autoReroot,
            onCheckedChange = onAutoRerootChange,
        )

        // Local payload section
        SectionHeader(stringResource(R.string.local_payload_card_title))
        SwitchPreference(
            title = stringResource(R.string.local_payload_mode),
            subtitle = stringResource(R.string.local_payload_mode_description),
            checked = localPayloadMode,
            onCheckedChange = onLocalPayloadModeChange,
        )

        // Appearance section
        SectionHeader(stringResource(R.string.appearance))
        Text(
            text = stringResource(R.string.material_color),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(R.string.material_color_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ColorSelector(
            selected = accentColor,
            onSelect = onAccentColorChange,
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Theme",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        ThemeSelector(
            selected = themeMode,
            onSelect = onThemeModeChange,
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
fun SwitchPreference(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ColorSelector(selected: String, onSelect: (String) -> Unit) {
    val colors = listOf(
        "dynamic" to stringResource(R.string.color_dynamic),
        "blue" to stringResource(R.string.color_blue),
        "green" to stringResource(R.string.color_green),
        "orange" to stringResource(R.string.color_orange),
        "violet" to stringResource(R.string.color_violet),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { (key, label) ->
            FilterChip(
                selected = selected == key,
                onClick = { onSelect(key) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
fun ThemeSelector(selected: String, onSelect: (String) -> Unit) {
    val themes = listOf(
        "system" to stringResource(R.string.theme_system),
        "light" to stringResource(R.string.theme_light),
        "dark" to stringResource(R.string.theme_dark),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        themes.forEach { (key, label) ->
            FilterChip(
                selected = selected == key,
                onClick = { onSelect(key) },
                label = { Text(label) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// App theme wrapper — delegates to whatever theme the app already has
// ---------------------------------------------------------------------------

@Composable
fun AppTheme(
    accentColor: String,
    themeMode: String,
    content: @Composable () -> Unit,
) {
    // Use Material3 dynamic color / color scheme based on settings.
    // isDark is derived from themeMode; for "system" we use isSystemInDarkTheme().
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (isDark) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
