package com.howdy.echowave.ui.listeningsummary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howdy.echowave.data.local.SettingsRepository
import com.howdy.echowave.domain.model.ListeningSummary
import com.howdy.echowave.domain.model.ListeningSummaryItem
import com.howdy.echowave.domain.model.ListeningSummaryPeriod
import com.howdy.echowave.domain.repository.DnaRepository
import com.howdy.echowave.domain.dna.MusicDnaProfile
import com.howdy.echowave.domain.repository.ListeningSummaryRepository

@Composable
fun ListeningSummaryScreen(
    onBack: () -> Unit,
    summaryRepository: ListeningSummaryRepository,
    dnaRepository: DnaRepository,
    settingsRepository: SettingsRepository,
    vm: ListeningSummaryViewModel = viewModel(
        factory = ListeningSummaryViewModel.Factory(summaryRepository, dnaRepository, settingsRepository),
    ),
) {
    val state by vm.state.collectAsState()
    val dayPartsEnabled by vm.dayPartsEnabled.collectAsState()
    val dnaProfile by dnaRepository.observeProfile().collectAsState(initial = MusicDnaProfile.empty())
    val dnaEnabled by dnaRepository.observeEnabled().collectAsState(initial = true)
    var confirmClear by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) {
        vm.refresh()
        while (true) {
            kotlinx.coroutines.delay(10_000L)
            vm.refresh()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text("Listening", style = MaterialTheme.typography.headlineSmall)
            }
            IconButton(onClick = { showInfo = true }) {
                Icon(Icons.Default.Info, contentDescription = "About listening summary",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            ListeningSummaryPeriod.entries.forEach { period ->
                FilterChip(
                    selected = state.period == period,
                    onClick = { vm.selectPeriod(period) },
                    label = { Text(period.shortLabel(), maxLines = 1) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        when {
            state.loading -> Column(
                Modifier.fillMaxWidth().padding(vertical = 42.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text("Loading your listening summary…", modifier = Modifier.padding(top = 12.dp))
            }
            state.error != null -> SummaryError(state.error.orEmpty(), vm::refresh)
            state.summary?.hasListeningEvents != true -> SummaryEmpty(state.period)
            state.summary != null -> SummaryContent(
                summary = state.summary!!,
                dayPartsEnabled = dayPartsEnabled,
                clearing = state.clearing,
                error = state.error,
                onDayPartsChanged = vm::setDayPartsEnabled,
                onClear = { confirmClear = true },
            )
        }

        MusicDnaCard(
            profile = dnaProfile,
            enabled = dnaEnabled,
            showDayParts = dayPartsEnabled,
        )

        Spacer(Modifier.height(24.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear listening history?") },
            text = {
                Text("This deletes the local events used by Listening Summary and Music DNA, and resets the Music DNA profile. Favorites and playlists stay untouched.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearHistory()
                }) { Text("Clear history") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("About your listening") },
            text = { Text("A play is a completed track or 30 seconds listened. Listening time uses saved sessions. History stays on this device and covers up to 12 months.") },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Done") } },
        )
    }
}

@Composable
private fun MusicDnaCard(
    profile: MusicDnaProfile,
    enabled: Boolean,
    showDayParts: Boolean,
) {
    SummaryCard {
        Text("Your Music DNA", style = MaterialTheme.typography.titleMedium)
        Text(
            when {
                !enabled -> "Personalization is paused. Turn Music DNA back on in Settings to build this profile."
                profile.totalEvents == 0 -> "Your profile is empty so far. Keep listening with Music DNA enabled to build it."
                profile.totalEvents < 5 -> "Your profile is still taking shape from ${profile.totalEvents} local signals."
                else -> "Built from ${profile.totalEvents} local listening signals. It updates as your listening changes."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (enabled && profile.totalEvents > 0) {
            val vibe = when {
                profile.totalEvents < 5 -> "Early profile"
                !showDayParts && profile.currentVibe == "Late-night listener" -> null
                else -> profile.currentVibe
            }
            if (vibe != null) {
                Text("Listening pattern · $vibe", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp))
            }
            DnaItems("Artists in your profile", profile.topArtists.map { it.label to it.count })
            DnaItems("Detected language signals", profile.languages.map { it.label to it.count })
            if (showDayParts && profile.bestListeningTime != null) {
                Text("Most active listening period · ${profile.bestListeningTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp))
            }
            if (profile.topGenres.isEmpty()) {
                Text("Genre preferences aren't shown because verified genre metadata isn't available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp))
            }
        }
        Text("Calculated and stored on this device. Reset or pause it in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun DnaItems(title: String, items: List<Pair<String, Int>>) {
    Text(title, style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 12.dp))
    if (items.isEmpty()) {
        Text("No reliable signals yet.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp))
    } else {
            items.take(3).forEach { (label, count) ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1)
                Text(count.toString(), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SummaryContent(
    summary: ListeningSummary,
    dayPartsEnabled: Boolean,
    clearing: Boolean,
    error: String?,
    onDayPartsChanged: (Boolean) -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("LISTENING TIME", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatDuration(summary.listeningMs),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text("${summary.period.label} · ${summary.plays} ${if (summary.plays == 1) "play" else "plays"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
        SummaryMetric("Completed", summary.completedTracks.toString(), Modifier.weight(1f))
        SummaryMetric("Tracks", summary.uniqueTracks.toString(), Modifier.weight(1f))
        SummaryMetric("Artists", summary.uniqueArtists.toString(), Modifier.weight(1f))
    }

    SummaryList("Top artists", summary.topArtists)
    SummaryList("Top tracks", summary.topTracks)
    SummaryList("On repeat", summary.repeatedTracks, emptyMessage = "Nothing on repeat yet.")

    SummaryCard {
        Text("Time of day", style = MaterialTheme.typography.titleMedium)
        SwitchRow(
            title = "Show time of day",
            checked = dayPartsEnabled,
            onCheckedChange = onDayPartsChanged,
        )
        if (dayPartsEnabled) {
            if (summary.listeningByDayPart.isEmpty()) {
                Text("No time of day data for this period.", style = MaterialTheme.typography.bodySmall)
            } else {
                val total = summary.listeningByDayPart.sumOf { it.listeningMs }.coerceAtLeast(1L)
                summary.listeningByDayPart.forEach { part ->
                    val percent = (part.listeningMs * 100 / total).toInt()
                    Text("${part.label} · ${formatDuration(part.listeningMs)} · $percent%",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }

    SummaryCard {
        Text("Listening history", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onClear, enabled = !clearing,
            modifier = Modifier.padding(top = 4.dp)) {
            Text(if (clearing) "Clearing history…" else "Clear listening history")
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SummaryMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(title, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryList(
    title: String,
    items: List<ListeningSummaryItem>,
    note: String? = null,
    emptyMessage: String = "No listening data in this period yet.",
) {
    SummaryCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (note != null) Text(note, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp))
        if (items.isEmpty()) {
            Text(emptyMessage, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp))
        } else {
            items.take(3).forEachIndexed { index, item ->
                Row(Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium)
                    Text(item.label, Modifier.weight(1f).padding(horizontal = 10.dp),
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(item.count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SummaryEmpty(period: ListeningSummaryPeriod) {
    SummaryCard {
        Text("No listening data for ${period.label.lowercase()} yet.",
            style = MaterialTheme.typography.titleMedium)
        Text("Play music with Music DNA enabled and your private summary will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun SummaryError(message: String, onRetry: () -> Unit) {
    SummaryCard {
        Text(message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("Retry") }
    }
}

@Composable
private fun SummaryCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun ListeningSummaryPeriod.shortLabel(): String = when (this) {
    ListeningSummaryPeriod.TODAY -> "Today"
    ListeningSummaryPeriod.THIS_WEEK -> "Week"
    ListeningSummaryPeriod.THIS_MONTH -> "Month"
    ListeningSummaryPeriod.ALL_RECORDED -> "Recorded"
}

private fun formatDuration(milliseconds: Long): String {
    val minutes = milliseconds.coerceAtLeast(0L) / 60_000L
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        minutes == 0L && milliseconds > 0 -> "< 1 min"
        hours > 0 -> "${hours}h ${remainingMinutes}m"
        else -> "${minutes} min"
    }
}
