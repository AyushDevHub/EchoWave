package com.howdy.echowave.core.network

/** Production error taxonomy. Every layer maps to this; UI shows userMessage. */
sealed interface EchoWaveError {
    data class Network(val message: String) : EchoWaveError
    data class Source(val message: String) : EchoWaveError
    data class StreamUnavailable(val trackId: String) : EchoWaveError
    data class StreamExpired(val trackId: String) : EchoWaveError
    data class CipherUnsupported(val trackId: String) : EchoWaveError
    data class Unknown(val message: String) : EchoWaveError
}

fun EchoWaveError.userMessage(): String = when (this) {
    is EchoWaveError.Network -> "No internet connection. Check your network and retry."
    is EchoWaveError.Source -> "Couldn't load results. ${message.ifBlank { "Retry." }}"
    is EchoWaveError.StreamUnavailable -> "This track isn't currently available."
    is EchoWaveError.StreamExpired -> "Stream expired. Retrying…"
    is EchoWaveError.CipherUnsupported -> "Unable to play this track (ciphered stream)."
    is EchoWaveError.Unknown -> message.ifBlank { "Something went wrong. Retry." }
}
