package gr.geo.mediaplayer.playback

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Pauses the music after a chosen number of minutes. */
object SleepTimer {

    private val handler = Handler(Looper.getMainLooper())

    /** elapsedRealtime at which the music will stop, or 0 when off. */
    private val _endsAt = MutableStateFlow(0L)
    val endsAt: StateFlow<Long> = _endsAt

    private val stopRunnable = Runnable {
        PlaybackService.currentPlayer?.pause()
        _endsAt.value = 0L
    }

    fun start(minutes: Int) {
        handler.removeCallbacks(stopRunnable)
        val delay = minutes * 60_000L
        _endsAt.value = SystemClock.elapsedRealtime() + delay
        handler.postDelayed(stopRunnable, delay)
    }

    fun cancel() {
        handler.removeCallbacks(stopRunnable)
        _endsAt.value = 0L
    }

    fun remainingMs(): Long {
        val end = _endsAt.value
        return if (end == 0L) 0L else (end - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
    }
}
