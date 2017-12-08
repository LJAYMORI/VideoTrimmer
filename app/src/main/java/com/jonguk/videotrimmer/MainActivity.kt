package com.jonguk.videotrimmer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceView
import android.view.View
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.jakewharton.rxbinding2.view.RxView
import com.tbruyelle.rxpermissions2.RxPermissions

class MainActivity : AppCompatActivity() {

    companion object {
        @JvmStatic val REQ_CODE_MAIN = 123
    }

    private val loadView by lazy { findViewById<View>(R.id.load_button) }
    private val videoLayout by lazy { findViewById<AspectRatioFrameLayout>(R.id.video_layout) }
    private val videoSurfaceView by lazy { findViewById<SurfaceView>(R.id.video_surface_view) }
    private val mediaControlView by lazy { findViewById<MediaControllerView>(R.id.media_controller_view) }

    private val player: SimpleExoPlayer =
            ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(this),
                    DefaultTrackSelector(), DefaultLoadControl())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializePlayer()

        val disposable = RxView.clicks(loadView)
                .compose(RxPermissions(this).ensure(Manifest.permission.READ_EXTERNAL_STORAGE))
                .subscribe({ granted ->
                    if (granted) {
//                        startActivityForResult(VideoGalleryActivity.newIntent(this), REQ_CODE_MAIN)
                    } else {
                        // TODO
                    }
                }, {})
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQ_CODE_MAIN -> {
                    val uriPath = data.extras?.getString(Constant.VIDEO_URI)
                    if (uriPath != null) {
                        player.prepare(buildMediaSource(Uri.parse(uriPath)), true, false)
                    }
                }
            }
        }
    }

    private fun initializePlayer() {
        player.playWhenReady = true
        player.seekTo(0)

        val uri = Uri.parse("http://www.sample-videos.com/video/mp4/480/big_buck_bunny_480p_1mb.mp4")
        player.prepare(buildMediaSource(uri), true, false)
    }

    private fun releasePlayer() {
        player.let { player.release() }
//        if (player != null) {
//            playbackPosition = player.getCurrentPosition();
//            currentWindow = player.getCurrentWindowIndex();
//            playWhenReady = player.getPlayWhenReady();
//            player.release();
//            player = null;
//        }
    }

    private fun hideSystemUi() {
        var visibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            visibility = (visibility or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    private fun buildMediaSource(uri: Uri) = ExtractorMediaSource(
            uri, DefaultHttpDataSourceFactory("ua"),
            DefaultExtractorsFactory(),
            null, null)

}
