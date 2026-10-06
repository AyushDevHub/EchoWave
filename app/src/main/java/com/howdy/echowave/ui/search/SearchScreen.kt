package com.howdy.echowave.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.usecase.SearchTracksUseCase
import com.howdy.echowave.ui.components.TrackRow

@Composable
fun SearchScreen(
    onPlay: (List<Track>, Int) -> Unit,
    vm: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(
            SearchTracksUseCase((LocalContext.current.applicationContext as EchoWaveApp).container.musicRepo),
        ),
    ),
) {
    val ui by vm.ui.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TextField(value = ui.query, onValueChange = vm::onQuery, placeholder = { Text("Search songs") }, singleLine = true)
        when {
            ui.loading -> CircularProgressIndicator()
            ui.error != null -> Column {
                Text("Couldn't load results. ${ui.error}")
                Button(onClick = { vm.onQuery(ui.query) }) { Text("Retry") }
            }
            ui.results.isEmpty() && ui.query.isNotBlank() -> Text("No results")
            else -> LazyColumn {
                items(ui.results.indices.toList()) { i ->
                    TrackRow(ui.results[i], onClick = { onPlay(ui.results, i) })
                }
            }
        }
    }
}
