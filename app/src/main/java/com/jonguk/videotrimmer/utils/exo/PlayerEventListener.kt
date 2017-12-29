package com.jonguk.videotrimmer.utils.exo

import android.support.annotation.StringRes
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.jonguk.videotrimmer.R

/**
 * Created by Jonguk on 2017. 12. 11..
 */
class PlayerEventListener private constructor() : Player.DefaultEventListener() {
    private lateinit var trackSelector: DefaultTrackSelector
    private var lastSeenTrackGroupArray: TrackGroupArray? = null

    private var inErrorState: Boolean = false
    private var updateResumePositionCallback: (() -> Unit)? = null
    private var showControlCallback: (() -> Unit)? = null
    private var updateButtonVisibilitiesCallback: (() -> Unit)? = null
    private var initializePlayerCallback: (() -> Unit)? = null
    private var clearResumePositionCallback: (() -> Unit)? = null
    private var isBehindLiveWindowCallback: ((ExoPlaybackException) -> Boolean)? = null
    private var messageCallback: ((Int, Array<Any>?) -> Unit)? = null

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            showControls()
        }
        updateResumePosition()
    }

    override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
        if (inErrorState) {
            // This will only occur if the user has performed a seek whilst in the error state. Update
            // the resume position so that if the user then retries, playback will resume from the
            // position to which they seeked.
            updateResumePosition()
        }
    }

    override fun onPlayerError(e: ExoPlaybackException?) {
        e?.let {
            if (it.type == ExoPlaybackException.TYPE_RENDERER) {
                val cause = e.rendererException
                if (cause is MediaCodecRenderer.DecoderInitializationException) {
                    // Special case for decoder initialization failures.
                    if (cause.decoderName == null) {
                        when {
                            cause.cause is MediaCodecUtil.DecoderQueryException ->
                                messageCallback(R.string.error_querying_decoders, null)
                            cause.secureDecoderRequired ->
                                messageCallback(R.string.error_no_secure_decoder, cause.mimeType)
                            else -> messageCallback(R.string.error_no_decoder, cause.mimeType)
                        }
                    } else {
                        messageCallback(R.string.error_instantiating_decoder, cause.decoderName)
                    }
                }
            }
        }

        inErrorState = true
        e?.let {
            if (isBehindLiveWindowCallback(it)) {
                clearResumePosition()
                initializePlayer()
            } else {
                updateResumePosition()
                updateButtonVisibilities()
                showControls()
            }
        }
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
        updateButtonVisibilities()
        if (trackGroups !== lastSeenTrackGroupArray) {
            val mappedTrackInfo = trackSelector.currentMappedTrackInfo
            mappedTrackInfo?.let {
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
                        == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    messageCallback(R.string.error_unsupported_video, null)
                }
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
                        == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    messageCallback(R.string.error_unsupported_audio, null)
                }
            }
            lastSeenTrackGroupArray = trackGroups
        }
    }

    private fun showControls() {
        showControlCallback?.invoke()
    }

    private fun updateResumePosition() {
        updateResumePositionCallback?.invoke()
    }

    private fun updateButtonVisibilities() {
        updateButtonVisibilitiesCallback?.invoke()
    }

    private fun messageCallback(@StringRes resId: Int, vararg arg: Any?) {
        messageCallback?.invoke(resId, arrayOf(arg))
    }

    private fun isBehindLiveWindowCallback(e: ExoPlaybackException): Boolean =
        isBehindLiveWindowCallback?.invoke(e) ?: false


    private fun clearResumePosition() {
        clearResumePositionCallback?.invoke()
    }

    private fun initializePlayer() {
        initializePlayerCallback?.invoke()
    }

    class Builder {
        private var trackSelector: DefaultTrackSelector? = null

        private var updateResumePositionCallback: (() -> Unit)? = null
        private var showControlCallback: (() -> Unit)? = null
        private var updateButtonVisibilitiesCallback: (() -> Unit)? = null
        private var initializePlayerCallback: (() -> Unit)? = null
        private var clearResumePositionCallback: (() -> Unit)? = null
        private var isBehindLiveWindowCallback: ((ExoPlaybackException) -> Boolean)? = null
        private var messageCallback: ((Int, Array<Any>?) -> Unit)? = null

        fun trackSelector(trackSelector: DefaultTrackSelector): Builder {
            this.trackSelector = trackSelector
            return this
        }

        fun updateResumePositionCallback(updateResumePositionCallback: () -> Unit): Builder {
            this.updateResumePositionCallback = updateResumePositionCallback
            return this
        }

        fun showControlCallback(showControlCallback: () -> Unit): Builder {
            this.showControlCallback = showControlCallback
            return this
        }

        fun updateButtonVisibilitiesCallback(updateButtonVisibilitiesCallback: () -> Unit): Builder {
            this.updateButtonVisibilitiesCallback = updateButtonVisibilitiesCallback
            return this
        }

        fun initializePlayerCallback(initializePlayerCallback: () -> Unit): Builder {
            this.initializePlayerCallback = initializePlayerCallback
            return this
        }

        fun clearResumePositionCallback(clearResumePositionCallback: () -> Unit): Builder {
            this.clearResumePositionCallback = clearResumePositionCallback
            return this
        }

        fun isBehindLiveWindowCallback(isBehindLiveWindowCallback: (ExoPlaybackException) -> Boolean): Builder {
            this.isBehindLiveWindowCallback = isBehindLiveWindowCallback
            return this
        }

        fun messageCallback(messageCallback: (Int, Array<Any>?) -> Unit): Builder {
            this.messageCallback = messageCallback
            return this
        }

        fun build(): PlayerEventListener {
            val listener = PlayerEventListener()
            listener.trackSelector = trackSelector!!
            listener.updateResumePositionCallback = updateResumePositionCallback
            listener.showControlCallback = showControlCallback
            listener.updateButtonVisibilitiesCallback = updateButtonVisibilitiesCallback
            listener.initializePlayerCallback = initializePlayerCallback
            listener.clearResumePositionCallback = clearResumePositionCallback
            listener.isBehindLiveWindowCallback = isBehindLiveWindowCallback
            listener.messageCallback = messageCallback
            return listener
        }
    }

}