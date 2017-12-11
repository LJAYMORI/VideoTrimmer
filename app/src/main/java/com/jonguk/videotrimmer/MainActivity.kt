package com.jonguk.videotrimmer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.jonguk.videotrimmer.exo.EventLogger
import com.jonguk.videotrimmer.exo.PlayerEventListener
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

class MainActivity : AppCompatActivity() {
    private val loadView by lazy { findViewById<View>(R.id.load_button) }
    private val videoView by lazy { findViewById<SimpleExoPlayerView>(R.id.exo_player_view) }
    private val mediaControlView by lazy { findViewById<MediaControllerView>(R.id.media_controller_view) }

    private var mainHandler: Handler? = null
    private var eventLogger: EventLogger? = null
    private var player: SimpleExoPlayer? = null
    private var mediaDataSourceFactory: DataSource.Factory? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var lastSeenTrackGroupArray: TrackGroupArray? = null

    private var shouldAutoPlay: Boolean = false
    private var resumeWindow: Int? = 0
    private var resumePosition: Long? = 0L

    companion object {
        @JvmStatic val REQ_CODE_MAIN = 123

        private val defaultCookieManager: CookieManager by lazy { CookieManager() }
        private val BANDWIDTH_METER = DefaultBandwidthMeter()
    }

    init {
        defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        shouldAutoPlay = true
        clearResumePosition()
        mediaDataSourceFactory = DefaultDataSourceFactory(this, BANDWIDTH_METER,
                DefaultHttpDataSourceFactory(Util.getUserAgent(this, application.packageName),
                        BANDWIDTH_METER))
        mainHandler = Handler()
        val currentHandler = CookieHandler.getDefault()
        if (currentHandler != defaultCookieManager) {
            CookieHandler.setDefault(defaultCookieManager)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoView.requestFocus()
//        val disposable = RxView.clicks(loadView)
//                .compose(RxPermissions(this).ensure(Manifest.permission.READ_EXTERNAL_STORAGE))
//                .subscribe({
//                    startActivityForResult(VideoGalleryActivity.newIntent(this), REQ_CODE_MAIN)
//                }, {})
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            releasePlayer()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        releasePlayer()
        shouldAutoPlay = true
        clearResumePosition()
        setIntent(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQ_CODE_MAIN -> {
                    val uriPath = data.extras?.getString(Constant.VIDEO_URI)
                    uriPath?.let {
                        player?.prepare(buildMediaSource(Uri.parse(it)), true, false)
                    }
                }
            }
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource = ExtractorMediaSource(uri,
            mediaDataSourceFactory, DefaultExtractorsFactory(), mainHandler, eventLogger)

    private fun initializePlayer() {
        // TODO local video
//        var intent: Intent? = intent

        var playerEventListener: PlayerEventListener? = null
        val needNewPlayer = player == null
        if (needNewPlayer) {
            val adaptiveTrackSelection: TrackSelection.Factory =
                    AdaptiveTrackSelection.Factory(BANDWIDTH_METER)
            trackSelector = DefaultTrackSelector(adaptiveTrackSelection)
            lastSeenTrackGroupArray = null
            eventLogger = EventLogger(trackSelector)


//            player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector)
            playerEventListener = PlayerEventListener.Builder()
                    .trackSelector(trackSelector)
                    .lastSeenTrackGroupArray(lastSeenTrackGroupArray)
                    .updateResumePositionCallback { updateResumePosition() }
                    .showControlCallback { showControls() }
                    .updateButtonVisibilitiesCallback { updateButtonVisibilities() }
                    .initializePlayerCallback { initializePlayer() }
                    .clearResumePositionCallback { clearResumePosition() }
                    .isBehindLiveWindowCallback { isBehindLiveWindow(it) }
                    .messageCallback { resId, arg -> showToast(resId, arg) }
                    .build()
            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
            player?.addListener(playerEventListener)
            player?.addListener(eventLogger)
            player?.addMetadataOutput(eventLogger)
            player?.setAudioDebugListener(eventLogger)
            player?.setVideoDebugListener(eventLogger)

            videoView.player = player
            player?.playWhenReady = shouldAutoPlay

        }

        val haveResumePosition = resumeWindow != C.INDEX_UNSET
        if (haveResumePosition) {
            player?.seekTo(resumeWindow ?: 0, resumePosition ?: 0)
        }

        val uri = Uri.parse("http://www.sample-videos.com/video/mp4/480/big_buck_bunny_480p_1mb.mp4")
        player?.prepare(buildMediaSource(uri), !haveResumePosition, false)
        playerEventListener?.setErrorState(false)
        updateButtonVisibilities()
    }

    private fun releasePlayer() {
        player?.let {
            shouldAutoPlay = it.playWhenReady
            updateResumePosition()
            it.release()
            player = null
            trackSelector = null
            eventLogger = null
        }
    }

    private fun updateResumePosition() {
        resumeWindow = player?.currentWindowIndex
        resumePosition = Math.max(0L, player?.contentPosition ?: 0L)
    }

    private fun showControls() {
        mediaControlView?.visibility = View.VISIBLE
    }

    private fun updateButtonVisibilities() {
        player?.let {
            val mappedTrackInfo = trackSelector?.currentMappedTrackInfo
            mappedTrackInfo?.let {
                for (i in 0 until it.length) {
                    val groups = it.getTrackGroups(i)
                    if (groups.length != 0) {
                        val label: Int = when (player?.getRendererType(i)) {
                            C.TRACK_TYPE_AUDIO -> R.string.audio
                            C.TRACK_TYPE_VIDEO -> R.string.video
                            C.TRACK_TYPE_TEXT -> R.string.text
                            else -> 0
                        }
                        if (label != 0) {
                            // TODO something..
                        }
                    }
                }
            }
        }
    }

    private fun clearResumePosition() {
        resumeWindow = C.INDEX_UNSET
        resumePosition = C.TIME_UNSET
    }

    private fun isBehindLiveWindow(e: ExoPlaybackException?) : Boolean {
        if (e?.type != ExoPlaybackException.TYPE_SOURCE) {
            return false
        }
        var cause: Throwable? = e.sourceException
        while (cause != null) {
            if (cause is BehindLiveWindowException) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    private fun showToast(resId: Int, arg: Any?) {
        if (arg != null) {
            Toast.makeText(this, getString(resId, arg), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
        }
    }

}
