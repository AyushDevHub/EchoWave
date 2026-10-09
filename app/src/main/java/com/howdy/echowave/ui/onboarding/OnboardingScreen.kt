package com.howdy.echowave.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.ui.components.AppLogo
import com.howdy.echowave.ui.components.TrackArtwork

/**
 * First-start onboarding (Spotify-style): name (mandatory) -> genres ->
 * artists (live search). Reused from Settings as an editor (editMode).
 * Fresh flow blocks system back; every step has empty states and the
 * artist search has loading + error + retry.
 */
@Composable
fun OnboardingScreen(
    editMode: Boolean = false,
    initialName: String = "",
    onBack: () -> Unit = {},
    onComplete: () -> Unit = {},
    vm: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.factory(
            LocalContext.current.applicationContext as EchoWaveApp,
        ),
    ),
) {
    val ui by vm.ui.collectAsState()
    LaunchedEffect(initialName) { vm.prefillName(initialName) }
    if (!editMode) BackHandler(enabled = ui.step == 0) { /* name is mandatory: stay */ }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ui.step > 0 || editMode) {
                IconButton(onClick = { if (ui.step > 0) vm.back() else onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            AppLogo(size = 40.dp)
            Text(
                if (editMode) "Edit taste" else "Welcome to EchoWave",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            Text(
                "${ui.step + 1}/3",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
        when (ui.step) {
            0 -> NameStep(
                name = ui.name,
                onName = vm::onName,
                canContinue = ui.canContinue(),
                onNext = vm::next,
            )
            1 -> GenreStep(
                selected = ui.genres,
                onToggle = vm::toggleGenre,
                canContinue = ui.canContinue(),
                onNext = vm::next,
            )
            else -> ArtistStep(
                ui = ui,
                onQuery = vm::onArtistQuery,
                onToggle = vm::toggleArtist,
                isSelected = vm::isSelected,
                saving = ui.saving,
                onDone = { vm.complete(editMode, onComplete) },
            )
        }
    }
}

@Composable
private fun NameStep(
    name: String,
    onName: (String) -> Unit,
    canContinue: Boolean,
    onNext: () -> Unit,
) {
    Text("What should we call you?", style = MaterialTheme.typography.titleLarge)
    Text(
        "Your name shows on the home screen.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Your name") },
        placeholder = { Text("e.g. Asta") },
        shape = RoundedCornerShape(16.dp),
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = onNext, enabled = canContinue, modifier = Modifier.fillMaxWidth()) {
        Text("Continue")
    }
    if (!canContinue) {
        Text(
            "Please enter your name to continue.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun GenreStep(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    canContinue: Boolean,
    onNext: () -> Unit,
) {
    Text("What are you into?", style = MaterialTheme.typography.titleLarge)
    Text(
        "Pick at least one — we'll fill your home screen with it.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
    Spacer(Modifier.height(20.dp))
    FlowChips(
        items = ONBOARDING_GENRES,
        selected = { it in selected },
        onToggle = onToggle,
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = onNext, enabled = canContinue, modifier = Modifier.fillMaxWidth()) {
        Text("Continue")
    }
}

@Composable
private fun ArtistStep(
    ui: OnboardingUiState,
    onQuery: (String) -> Unit,
    onToggle: (com.howdy.echowave.domain.model.SearchItem.Artist) -> Unit,
    isSelected: (com.howdy.echowave.domain.model.SearchItem.Artist) -> Boolean,
    saving: Boolean,
    onDone: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
    Text("Who do you love?", style = MaterialTheme.typography.titleLarge)
    Text(
        "Search and pick artists${if (ui.artists.isNotEmpty()) " (${ui.artists.size} picked)" else ""} — or skip.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = ui.artistQuery,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        placeholder = { Text("Search artists") },
        shape = CircleShape,
    )
    Spacer(Modifier.height(12.dp))
    Box(Modifier.weight(1f).fillMaxWidth()) {
        when {
            ui.artistLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Searching artists…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ui.artistError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't load artists. ${ui.artistError}")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onQuery(ui.artistQuery) }) { Text("Retry") }
                }
            }
            ui.artistQuery.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (ui.artists.isEmpty()) "Type above to find artists."
                    else "Picked: ${ui.artists.joinToString { it.name }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            ui.artistResults.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No artists found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(ui.artistResults, key = { it.id }) { artist ->
                    val selected = isSelected(artist)
                    Row(
                        Modifier.fillMaxWidth().clickable { onToggle(artist) }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TrackArtwork(
                            artist.artworkUrl, "Artwork for ${artist.name}",
                            Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),
                        )
                        Text(
                            artist.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                        )
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Picked",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        TextButton(onClick = onDone, enabled = !saving, modifier = Modifier.weight(1f)) {
            Text("Skip")
        }
        Spacer(Modifier.size(12.dp))
        Button(onClick = onDone, enabled = !saving, modifier = Modifier.weight(1f)) {
            Text(if (saving) "Saving…" else "Done")
        }
    }
    }
}

@Composable
private fun FlowChips(
    items: List<String>,
    selected: (String) -> Boolean,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    FilterChip(
                        selected = selected(item),
                        onClick = { onToggle(item) },
                        label = { Text(item) },
                    )
                }
            }
        }
    }
}
