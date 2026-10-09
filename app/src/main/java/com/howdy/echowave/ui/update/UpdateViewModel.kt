package com.howdy.echowave.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.BuildConfig
import com.howdy.echowave.data.remote.update.GitHubReleaseChecker
import com.howdy.echowave.data.remote.update.LatestAppRelease
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface UpdateStatus {
    data object Checking : UpdateStatus
    data object Current : UpdateStatus
    data object Failed : UpdateStatus
    data class Available(val release: LatestAppRelease) : UpdateStatus
}

class UpdateViewModel : ViewModel() {
    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Checking)
    val status: StateFlow<UpdateStatus> = _status

    private var checkJob: Job? = null

    fun check(force: Boolean = false) {
        if (checkJob?.isActive == true) return
        val now = System.currentTimeMillis()
        if (!force && now - lastCheckAtMs < CHECK_INTERVAL_MS) return

        lastCheckAtMs = now
        _status.value = UpdateStatus.Checking
        checkJob = viewModelScope.launch {
            _status.value = try {
                val release = GitHubReleaseChecker.check(BuildConfig.VERSION_NAME)
                if (release == null) UpdateStatus.Current else UpdateStatus.Available(release)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                UpdateStatus.Failed
            }
        }
    }

    companion object {
        private var lastCheckAtMs = 0L
        private const val CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L
    }
}
