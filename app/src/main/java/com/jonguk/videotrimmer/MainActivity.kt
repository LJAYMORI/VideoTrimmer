package com.jonguk.videotrimmer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
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
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.jakewharton.rxbinding2.view.RxView
import com.jonguk.videotrimmer.gallery.VideoGalleryActivity
import com.jonguk.videotrimmer.utils.Constant
import com.jonguk.videotrimmer.utils.exo.EventLogger
import com.jonguk.videotrimmer.utils.exo.PlayerEventListener
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

class MainActivity : AppCompatActivity() {
    private val loadView by lazy { load_button }
    private val muteView by lazy { mute_button }
    private val videoView by lazy { exo_player_view }
    private val playOrPauseView by lazy { play_or_pause_view }
    private val rangeTrimView by lazy { range_trim_view }

    private var mainHandler: Handler? = null
    private var eventLogger: EventLogger? = null
    private var player: SimpleExoPlayer? = null
    private var mediaDataSourceFactory: DataSource.Factory? = null
    private var trackSelector: DefaultTrackSelector? = null

    private var shouldAutoPlay: Boolean = false
    private var resumeWindow: Int? = 0
    private var resumePosition: Long? = 0L

    private var audioRendIndex = -1
    private var audioMute: Boolean = false
    private var videoRenderIndex = -1
    private var videoTurnOff: Boolean = false

    private val disposable = CompositeDisposable()

    companion object {
        private val defaultCookieManager: CookieManager by lazy { CookieManager() }
        private val BANDWIDTH_METER = DefaultBandwidthMeter()
    }

    init {
        defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
    }

    private var videoUri: Uri? = null
        private set(value) {
            field = value
            playOrPauseView.visibility = if (value == null) View.GONE else View.VISIBLE
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        clearResumePosition()
        mediaDataSourceFactory = buildDataSourceFactory()
        mainHandler = Handler()
        val currentHandler = CookieHandler.getDefault()
        if (currentHandler != defaultCookieManager) {
            CookieHandler.setDefault(defaultCookieManager)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        muteView.setOnClickListener{
            if (audioRendIndex >= 0) {
                audioMute = !audioMute
                trackSelector?.setRendererDisabled(audioRendIndex, audioMute)
            }
        }

        disposable.add(RxView.clicks(playOrPauseView)
                .map { !playOrPauseView.isChecked }
                .subscribe({
                    if (it) {
                        playOrPauseView.setText(R.string.main_video_pause)
                        player?.playWhenReady = true
                    } else {
                        playOrPauseView.setText(R.string.main_video_play)
                        player?.playWhenReady = false
                    }
                    playOrPauseView.toggle()
                }, {}))

        disposable.add(RxView.clicks(loadView)
                .switchMap { (RxPermissions(this).request(Manifest.permission.READ_EXTERNAL_STORAGE)) }
                .filter { hasPermission -> hasPermission }
                .subscribe({
                    startActivityForResult(VideoGalleryActivity.newIntent(this),
                            VideoGalleryActivity.REQ_CODE_GALLERY)
                }, {}))
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M || player == null) {
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

    override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
    }

    override fun onNewIntent(intent: Intent?) {
        releasePlayer()
        clearResumePosition()
        setIntent(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                VideoGalleryActivity.REQ_CODE_GALLERY -> {
                    val uriPath = data.extras?.getString(Constant.VIDEO_URI)
                    uriPath?.let {
                        videoUri = Uri.parse(it)
                        rangeTrimView.videoUri = videoUri
                    }

                }
            }
        }
    }

    private fun buildMediaSource(uri: Uri, overrideExtension: String?): MediaSource {
        @C.ContentType val type = if (TextUtils.isEmpty(overrideExtension))
            Util.inferContentType(uri)
        else
            Util.inferContentType("." + overrideExtension)

        return when(type) {
            C.TYPE_SS -> SsMediaSource(uri, buildDataSourceFactory(),
                    DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger)
            C.TYPE_DASH -> DashMediaSource(uri, buildDataSourceFactory(),
                    DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger)
            C.TYPE_HLS -> HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger)
            C.TYPE_OTHER -> ExtractorMediaSource(uri,
                    mediaDataSourceFactory, DefaultExtractorsFactory(), mainHandler, eventLogger)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    private fun buildDataSourceFactory() = DefaultDataSourceFactory(this, BANDWIDTH_METER,
            DefaultHttpDataSourceFactory(Util.getUserAgent(this, application.packageName),
                    BANDWIDTH_METER))

    private fun initializePlayer() {
        val needNewPlayer = player == null
        if (needNewPlayer) {
            val adaptiveTrackSelection: TrackSelection.Factory =
                    AdaptiveTrackSelection.Factory(BANDWIDTH_METER)
            trackSelector = DefaultTrackSelector(adaptiveTrackSelection)
            eventLogger = EventLogger(trackSelector!!)

            val playerEventListener = PlayerEventListener.Builder()
                    .trackSelector(trackSelector!!)
                    .updateResumePositionCallback { updateResumePosition() }
                    .showControlCallback { showControls() }
                    .updateButtonVisibilitiesCallback { updateRenderIndices() }
                    .initializePlayerCallback { initializePlayer() }
                    .clearResumePositionCallback { clearResumePosition() }
                    .isBehindLiveWindowCallback { e -> isBehindLiveWindow(e) }
                    .messageCallback { resId, arg -> showToast(resId, arg) }
                    .build()

            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector).apply {
                addListener(playerEventListener)
                addListener(eventLogger)
                addMetadataOutput(eventLogger)
                setAudioDebugListener(eventLogger)
                setVideoDebugListener(eventLogger)
            }

            videoView.player = player
            player?.playWhenReady = shouldAutoPlay
        }

        val haveResumePosition = resumeWindow != C.INDEX_UNSET
        if (haveResumePosition) {
            player?.seekTo(resumeWindow ?: 0, resumePosition ?: 0)
        }

        videoUri?.let {
            player?.prepare(buildMediaSource(it, null), !haveResumePosition, false)
        }
        updateRenderIndices()
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
//        mediaControlView?.visibility = View.VISIBLE
    }

    private fun updateRenderIndices() {
        player?.let {
            trackSelector?.currentMappedTrackInfo?.let {
                for (i in 0 until it.length) {
                    val groups = it.getTrackGroups(i)
                    if (groups.length != 0) {
                        when (player?.getRendererType(i)) {
                            C.TRACK_TYPE_AUDIO -> audioRendIndex = i
                            C.TRACK_TYPE_VIDEO -> videoRenderIndex = i
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
