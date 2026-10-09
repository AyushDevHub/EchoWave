package com.howdy.echowave.data.remote.innertube

/**
 * visitorData holder. Every InnerTube response echoes it in
 * responseContext.visitorData; every request must send it back as
 * X-Goog-Visitor-Id + context.client.visitorData (donor: required —
 * some clients answer UNPLAYABLE with zero formats without it, and
 * pot-bound fetches 403 when the binding can't resolve).
 * Value is never logged.
 */
class VisitorStore(
    private val load: () -> String?,
    private val saveFn: (String) -> Unit,
) {
    private val lock = Any()

    fun current(): String? = synchronized(lock) { load() }

    /** Returns true when a genuinely new id arrived (callers reset token state). */
    fun offer(id: String?): Boolean {
        if (id.isNullOrBlank()) return false
        synchronized(lock) {
            if (id == load()) return false
            saveFn(id)
            return true
        }
    }

    companion object {
        fun sharedPrefs(context: android.content.Context): VisitorStore {
            val prefs = context.applicationContext.getSharedPreferences("echowave_visitor", 0)
            return VisitorStore(
                load = { prefs.getString("visitor_data", null) },
                saveFn = { prefs.edit().putString("visitor_data", it).apply() },
            )
        }

        fun inMemory(): VisitorStore {
            var box: String? = null
            val lock = Any()
            return VisitorStore(
                load = { synchronized(lock) { box } },
                saveFn = { synchronized(lock) { box = it } },
            )
        }
    }
}
