package com.howdy.echowave.data.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

/** Sends network streams through the existing cache and local files through Android's local readers. */
@UnstableApi
class LocalAwareDataSource(
    private val remote: DataSource,
    private val local: DataSource,
) : DataSource {
    private val transferListeners = mutableListOf<TransferListener>()
    private val remoteListeners = mutableSetOf<TransferListener>()
    private val localListeners = mutableSetOf<TransferListener>()
    private var active: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        if (transferListener !in transferListeners) transferListeners += transferListener
        attachListener(active, transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val selected = when (dataSpec.uri.scheme?.lowercase()) {
            "http", "https" -> remote
            else -> local
        }
        active = selected
        transferListeners.forEach { attachListener(selected, it) }
        return selected.open(dataSpec)
    }

    private fun attachListener(source: DataSource?, listener: TransferListener) {
        val attached = when (source) {
            remote -> remoteListeners
            local -> localListeners
            else -> return
        }
        if (attached.add(listener)) source.addTransferListener(listener)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        checkNotNull(active) { "Data source is not open" }.read(buffer, offset, length)

    override fun getUri() = active?.uri

    override fun getResponseHeaders() = active?.responseHeaders.orEmpty()

    override fun close() {
        try {
            active?.close()
        } finally {
            active = null
        }
    }

    @UnstableApi
    class Factory(
        private val remoteFactory: DataSource.Factory,
        private val localFactory: DataSource.Factory,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource =
            LocalAwareDataSource(remoteFactory.createDataSource(), localFactory.createDataSource())
    }
}
