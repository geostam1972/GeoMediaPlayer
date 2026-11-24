package gr.geo.mediaplayer.video

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Rational
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import gr.geo.mediaplayer.R
import gr.geo.mediaplayer.data.LibraryStore
import kotlin.math.abs

/**
 * Full-screen video player:
 *  - double-tap left/right side → -10 / +10 seconds
 *  - swipe up/down on the left → brightness, on the right → volume
 *  - pinch-free "fit / fill / zoom" button, rotate button, speed in ⚙
 *  - Picture-in-Picture when you press Home while a video plays
 */
@OptIn(UnstableApi::class)
class VideoPlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_INDEX = "index"
        /** Set by the Videos screen before opening the player. */
        var playlist: List<Pair<Uri, String>> = emptyList()
    }

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var titleView: TextView
    private lateinit var topBar: LinearLayout
    private lateinit var hud: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var audio: AudioManager

    private var resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    private var gestureMode = 0 // 0 none, 1 brightness, 2 volume
    private var startBrightness = 0.5f
    private var startVolume = 0
    private var accumulatedDy = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LibraryStore.init(this)
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                true // pauses the music player while a video plays
            )
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()

        buildUi()
        loadFromIntent(intent)

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                titleView.text = mediaItem?.mediaMetadata?.title ?: ""
                restorePosition()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                updatePipParams()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePipParams()
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        savePosition()
        loadFromIntent(intent)
    }

    private fun loadFromIntent(intent: Intent) {
        val data = intent.data
        val items: List<MediaItem>
        val start: Int
        if (data != null) {
            val name = runCatching {
                contentResolver.query(data, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
            }.getOrNull() ?: data.lastPathSegment ?: "Βίντεο"
            items = listOf(mediaItem(data, name.substringBeforeLast('.')))
            start = 0
        } else {
            items = playlist.map { (uri, title) -> mediaItem(uri, title) }
            start = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, (items.size - 1).coerceAtLeast(0))
        }
        if (items.isEmpty()) { finish(); return }
        player.setMediaItems(items, start, savedPositionFor(items[start]))
        player.prepare()
        player.play()
        titleView.text = items[start].mediaMetadata.title ?: ""
    }

    private fun mediaItem(uri: Uri, title: String) = MediaItem.Builder()
        .setUri(uri)
        .setMediaId(uri.toString())
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
        .build()

    // ---------- resume where you left off ----------
    private fun savedPositionFor(item: MediaItem): Long =
        LibraryStore.getString("vpos_" + item.mediaId)?.toLongOrNull() ?: 0L

    private fun restorePosition() {
        val item = player.currentMediaItem ?: return
        val pos = savedPositionFor(item)
        if (pos > 0) player.seekTo(pos)
    }

    private fun savePosition() {
        val item = player.currentMediaItem ?: return
        val pos = player.currentPosition
        val dur = player.duration
        val keep = pos > 10_000 && dur > 0 && pos < dur - 15_000
        LibraryStore.putString("vpos_" + item.mediaId, if (keep) pos.toString() else null)
    }

    // ---------- UI ----------
    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun buildUi() {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        playerView = PlayerView(this).apply {
            player = this@VideoPlayerActivity.player
            useController = true
            setShowNextButton(true)
            setShowPreviousButton(true)
            setShowSubtitleButton(true)
            controllerShowTimeoutMs = 3500
            setFullscreenButtonClickListener { toggleOrientation() }
            setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                topBar.visibility = visibility
            })
        }
        root.addView(playerView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // top bar: back, title, resize, PiP
        topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(28), dp(8), dp(8))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xAA000000.toInt(), 0x00000000)
            )
        }
        fun iconButton(res: Int, desc: String, onClick: () -> Unit) = ImageButton(this).apply {
            setImageResource(res)
            setColorFilter(Color.WHITE)
            background = null
            contentDescription = desc
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { onClick() }
        }
        topBar.addView(iconButton(R.drawable.ic_arrow_back, "Πίσω") { finish() })
        titleView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 17f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        topBar.addView(titleView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        topBar.addView(iconButton(R.drawable.ic_aspect, "Μέγεθος εικόνας") { cycleResize() })
        topBar.addView(iconButton(R.drawable.ic_pip, "Μικρό παράθυρο") { enterPip() })
        root.addView(topBar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))

        // HUD for brightness/volume/seek feedback
        hud = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 20f
            setPadding(dp(20), dp(12), dp(20), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(0xAA000000.toInt())
            }
            visibility = View.GONE
        }
        root.addView(hud, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        setContentView(root)
        setupGestures()
    }

    private fun showHud(text: String) {
        hud.text = text
        hud.visibility = View.VISIBLE
        handler.removeCallbacksAndMessages("hud")
        handler.postAtTime({ hud.visibility = View.GONE }, "hud", android.os.SystemClock.uptimeMillis() + 900)
    }

    private fun setupGestures() {
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                gestureMode = 0
                accumulatedDy = 0f
                startBrightness = window.attributes.screenBrightness.takeIf { it >= 0 } ?: 0.5f
                startVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                return false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val w = playerView.width
                when {
                    e.x < w * 0.4f -> { player.seekBack(); showHud("⏪  -10 δευτ.") }
                    e.x > w * 0.6f -> { player.seekForward(); showHud("+10 δευτ.  ⏩") }
                    else -> if (player.isPlaying) player.pause() else player.play()
                }
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
                val start = e1 ?: return false
                if (gestureMode == 0) {
                    if (abs(e2.y - start.y) < dp(20) || abs(dy) < abs(dx)) return false
                    gestureMode = if (start.x < playerView.width / 2f) 1 else 2
                }
                accumulatedDy += dy
                val fraction = accumulatedDy / (playerView.height * 0.7f)
                if (gestureMode == 1) {
                    val b = (startBrightness + fraction).coerceIn(0.01f, 1f)
                    window.attributes = window.attributes.apply { screenBrightness = b }
                    showHud("☀  ${(b * 100).toInt()}%")
                } else {
                    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val v = (startVolume + fraction * max).toInt().coerceIn(0, max)
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
                    showHud("🔊  ${v * 100 / max}%")
                }
                return true
            }
        })
        playerView.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            val consume = gestureMode != 0
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                gestureMode = 0
            }
            consume
        }
    }

    private fun cycleResize() {
        resizeMode = when (resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        playerView.resizeMode = resizeMode
        showHud(
            when (resizeMode) {
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Γέμισμα οθόνης"
                AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Τέντωμα"
                else -> "Προσαρμογή"
            }
        )
    }

    private fun toggleOrientation() {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    private fun hideSystemBars() {
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    // ---------- Picture in Picture ----------
    private fun pipParams(): PictureInPictureParams {
        val size = player.videoSize
        val ratio = if (size.width > 0 && size.height > 0) {
            val r = size.width.toFloat() / size.height
            when {
                r > 2.39f -> Rational(239, 100)
                r < 1 / 2.39f -> Rational(100, 239)
                else -> Rational(size.width, size.height)
            }
        } else Rational(16, 9)
        val b = PictureInPictureParams.Builder().setAspectRatio(ratio)
        if (Build.VERSION.SDK_INT >= 31) {
            b.setAutoEnterEnabled(player.isPlaying)
            b.setSeamlessResizeEnabled(true)
        }
        return b.build()
    }

    private fun updatePipParams() {
        runCatching { setPictureInPictureParams(pipParams()) }
    }

    private fun enterPip() {
        runCatching { enterPictureInPictureMode(pipParams()) }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < 31 && player.isPlaying) enterPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        playerView.useController = !isInPictureInPictureMode
        topBar.visibility = if (isInPictureInPictureMode) View.GONE else topBar.visibility
    }

    override fun onStop() {
        super.onStop()
        savePosition()
        // When the PiP window is closed, or the app is left without PiP, pause the video.
        if (!isInPictureInPictureMode) player.pause()
    }

    override fun onDestroy() {
        savePosition()
        handler.removeCallbacksAndMessages(null)
        player.release()
        super.onDestroy()
    }
}
