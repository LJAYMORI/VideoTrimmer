package com.jonguk.videotrimmer.utils.exo

import android.os.SystemClock
import android.util.Log
import android.view.Surface
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.decoder.DecoderCounters
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.metadata.emsg.EventMessage
import com.google.android.exoplayer2.metadata.id3.*
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.video.VideoRendererEventListener
import java.io.IOException
import java.text.NumberFormat
import java.util.*

/**
 * Created by Jonguk on 2017. 12. 11..
 */
class EventLogger(private val trackSelector: MappingTrackSelector) : Player.EventListener, MetadataOutput,
        AudioRendererEventListener, VideoRendererEventListener, AdaptiveMediaSourceEventListener,
        ExtractorMediaSource.EventListener, DefaultDrmSessionManager.EventListener {

    companion object {
        private val TAG: String = "EventLogger"
        private val MAX_TIMELINE_ITEM_LINES = 3
        private val TIME_FORMAT by lazy { NumberFormat.getInstance(Locale.KOREA) }

        private fun getTimeString(timeMs: Long): String {
            return if (timeMs == C.TIME_UNSET) "?" else TIME_FORMAT.format((timeMs / 1000f).toDouble())
        }

        private fun getStateString(state: Int): String =
                when (state) {
                    Player.STATE_BUFFERING -> "B"
                    Player.STATE_ENDED -> "E"
                    Player.STATE_IDLE -> "I"
                    Player.STATE_READY -> "R"
                    else -> "?"
                }

        private fun getFormatSupportString(formatSupport: Int): String =
                when (formatSupport) {
                    RendererCapabilities.FORMAT_HANDLED -> "YES"
                    RendererCapabilities.FORMAT_EXCEEDS_CAPABILITIES -> "NO_EXCEEDS_CAPABILITIES"
                    RendererCapabilities.FORMAT_UNSUPPORTED_DRM -> "NO_UNSUPPORTED_DRM"
                    RendererCapabilities.FORMAT_UNSUPPORTED_SUBTYPE -> "NO_UNSUPPORTED_TYPE"
                    RendererCapabilities.FORMAT_UNSUPPORTED_TYPE -> "NO"
                    else -> "?"
                }

        private fun getAdaptiveSupportString(trackCount: Int, adaptiveSupport: Int): String =
                if (trackCount < 2) {
                    "N/A"
                } else {
                    when (adaptiveSupport) {
                        RendererCapabilities.ADAPTIVE_SEAMLESS -> "YES"
                        RendererCapabilities.ADAPTIVE_NOT_SEAMLESS -> "YES_NOT_SEAMLESS"
                        RendererCapabilities.ADAPTIVE_NOT_SUPPORTED -> "NO"
                        else -> "?"
                    }
                }

        private fun getTrackStatusString(enabled: Boolean): String {
            return if (enabled) "[X]" else "[ ]"
        }

        private fun getTrackStatusString(selection: TrackSelection?, group: TrackGroup, trackIndex: Int): String {
            return getTrackStatusString(selection != null && selection.trackGroup == group &&
                    selection.indexOf(trackIndex) != C.INDEX_UNSET)
        }

        private fun getRepeatModeString(@Player.RepeatMode repeatMode: Int): String =
                when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> "OFF"
                    Player.REPEAT_MODE_ONE -> "ONE"
                    Player.REPEAT_MODE_ALL -> "ALL"
                    else -> "?"
                }

        private fun getDiscontinuityReasonString(@Player.DiscontinuityReason reason: Int): String =
                when (reason) {
                    Player.DISCONTINUITY_REASON_PERIOD_TRANSITION -> "PERIOD_TRANSITION"
                    Player.DISCONTINUITY_REASON_SEEK -> "SEEK"
                    Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> "SEEK_ADJUSTMENT"
                    Player.DISCONTINUITY_REASON_INTERNAL -> "INTERNAL"
                    else -> "?"
                }
    }

    init {
        TIME_FORMAT.minimumFractionDigits = 2
        TIME_FORMAT.maximumFractionDigits = 2
        TIME_FORMAT.isGroupingUsed = false
    }

    private val window: Timeline.Window by lazy { Timeline.Window() }
    private val period: Timeline.Period by lazy { Timeline.Period() }
    private val startTimeMs: Long by lazy { SystemClock.elapsedRealtime() }

    // Player.EventListener

    override fun onLoadingChanged(isLoading: Boolean) {
        Log.d(TAG, "loading [$isLoading]")
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, state: Int) {
        Log.d(TAG, "state [${getSessionTimeString()}, $playWhenReady, ${getStateString(state)}]")
    }

    override fun onRepeatModeChanged(@Player.RepeatMode repeatMode: Int) {
        Log.d(TAG, "repeatMode [${getRepeatModeString(repeatMode)}]")
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        Log.d(TAG, "shuffleModeEnabled [$shuffleModeEnabled]")
    }

    override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
        Log.d(TAG, "positionDiscontinuity [${getDiscontinuityReasonString(reason)}]")
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        Log.d(TAG, "playbackParameters [speed=${playbackParameters.speed.format(2)}, " +
                "pitch=${playbackParameters.pitch.format(2)}]")
    }

    override fun onTimelineChanged(timeline: Timeline, manifest: Any?) {
        val periodCount = timeline.periodCount
        val windowCount = timeline.windowCount
        Log.d(TAG, "sourceInfo [periodCount=$periodCount, windowCount=$windowCount")
        for (i in 0 until Math.min(periodCount, MAX_TIMELINE_ITEM_LINES)) {
            timeline.getPeriod(i, period)
            Log.d(TAG, "  " + "period [" + getTimeString(period.durationMs) + "]")
        }
        if (periodCount > MAX_TIMELINE_ITEM_LINES) {
            Log.d(TAG, "  ...")
        }
        for (i in 0 until Math.min(windowCount, MAX_TIMELINE_ITEM_LINES)) {
            timeline.getWindow(i, window)
            Log.d(TAG, "  window [${getTimeString(window.durationMs)}, ${window.isSeekable}, ${window.isDynamic}]")
        }
        if (windowCount > MAX_TIMELINE_ITEM_LINES) {
            Log.d(TAG, "  ...")
        }
        Log.d(TAG, "]")
    }

    override fun onPlayerError(e: ExoPlaybackException) {
        Log.e(TAG, "playerFailed [${getSessionTimeString()}]", e)
    }

    override fun onTracksChanged(ignored: TrackGroupArray, trackSelections: TrackSelectionArray) {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        if (mappedTrackInfo == null) {
            Log.d(TAG, "Tracks []")
            return
        }
        Log.d(TAG, "Tracks [")
        // Log tracks associated to renderers.
        for (rendererIndex in 0 until mappedTrackInfo.length) {
            val rendererTrackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
            val trackSelection = trackSelections.get(rendererIndex)
            if (rendererTrackGroups.length > 0) {
                Log.d(TAG, "  Renderer:$rendererIndex [")
                for (groupIndex in 0 until rendererTrackGroups.length) {
                    val trackGroup = rendererTrackGroups.get(groupIndex)
                    val adaptiveSupport = getAdaptiveSupportString(trackGroup.length,
                            mappedTrackInfo.getAdaptiveSupport(rendererIndex, groupIndex, false))
                    Log.d(TAG, "    Group:$groupIndex, adaptive_supported=$adaptiveSupport [")
                    for (trackIndex in 0 until trackGroup.length) {
                        val status = getTrackStatusString(trackSelection, trackGroup, trackIndex)
                        val formatSupport = getFormatSupportString(
                                mappedTrackInfo.getTrackFormatSupport(rendererIndex, groupIndex, trackIndex))
                        Log.d(TAG, "      " + status + " Track:" + trackIndex + ", "
                                + Format.toLogString(trackGroup.getFormat(trackIndex))
                                + ", supported=" + formatSupport)
                    }
                    Log.d(TAG, "    ]")
                }
                // Log metadata for at most one of the tracks selected for the renderer.
                if (trackSelection != null) {
                    for (selectionIndex in 0 until trackSelection.length()) {
                        val metadata = trackSelection.getFormat(selectionIndex).metadata
                        if (metadata != null) {
                            Log.d(TAG, "    Metadata [")
                            printMetadata(metadata, "      ")
                            Log.d(TAG, "    ]")
                            break
                        }
                    }
                }
                Log.d(TAG, "  ]")
            }
        }
        // Log tracks not associated with a renderer.
        val unassociatedTrackGroups = mappedTrackInfo.unassociatedTrackGroups
        if (unassociatedTrackGroups.length > 0) {
            Log.d(TAG, "  Renderer:None [")
            for (groupIndex in 0 until unassociatedTrackGroups.length) {
                Log.d(TAG, "    Group:$groupIndex [")
                val trackGroup = unassociatedTrackGroups.get(groupIndex)
                for (trackIndex in 0 until trackGroup.length) {
                    val status = getTrackStatusString(false)
                    val formatSupport = getFormatSupportString(
                            RendererCapabilities.FORMAT_UNSUPPORTED_TYPE)
                    Log.d(TAG, "      " + status + " Track:" + trackIndex + ", "
                            + Format.toLogString(trackGroup.getFormat(trackIndex))
                            + ", supported=" + formatSupport)
                }
                Log.d(TAG, "    ]")
            }
            Log.d(TAG, "  ]")
        }
        Log.d(TAG, "]")
    }

    override fun onSeekProcessed() {
        Log.d(TAG, "seekProcessed")
    }

    // MetadataOutput

    override fun onMetadata(metadata: Metadata) {
        Log.d(TAG, "onMetadata [")
        printMetadata(metadata, "  ")
        Log.d(TAG, "]")
    }

    // AudioRendererEventListener

    override fun onAudioEnabled(counters: DecoderCounters) {
        Log.d(TAG, "audioEnabled [${getSessionTimeString()}]")
    }

    override fun onAudioSessionId(audioSessionId: Int) {
        Log.d(TAG, "audioSessionId [$audioSessionId]")
    }

    override fun onAudioDecoderInitialized(decoderName: String, elapsedRealtimeMs: Long,
                                           initializationDurationMs: Long) {
        Log.d(TAG, "audioDecoderInitialized [${getSessionTimeString()}, $decoderName]")
    }

    override fun onAudioInputFormatChanged(format: Format) {
        Log.d(TAG, "audioFormatChanged [${getSessionTimeString()}, ${Format.toLogString(format)}]")
    }

    override fun onAudioDisabled(counters: DecoderCounters) {
        Log.d(TAG, "audioDisabled [${getSessionTimeString()}]")
    }

    override fun onAudioSinkUnderrun(bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
        printInternalError("audioTrackUnderrun [$bufferSize, $bufferSizeMs, $elapsedSinceLastFeedMs]", null)
    }

    // VideoRendererEventListener

    override fun onVideoEnabled(counters: DecoderCounters) {
        Log.d(TAG, "videoEnabled [${getSessionTimeString()}]")
    }

    override fun onVideoDecoderInitialized(decoderName: String, elapsedRealtimeMs: Long,
                                           initializationDurationMs: Long) {
        Log.d(TAG, "videoDecoderInitialized [${getSessionTimeString()}, $decoderName]")
    }

    override fun onVideoInputFormatChanged(format: Format) {
        Log.d(TAG, "videoFormatChanged [${getSessionTimeString()}, ${Format.toLogString(format)}]")
    }

    override fun onVideoDisabled(counters: DecoderCounters) {
        Log.d(TAG, "videoDisabled [${getSessionTimeString()}]")
    }

    override fun onDroppedFrames(count: Int, elapsed: Long) {
        Log.d(TAG, "droppedFrames [${getSessionTimeString()}, $count]")
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
                                    pixelWidthHeightRatio: Float) {
        Log.d(TAG, "videoSizeChanged [$width, $height]")
    }

    override fun onRenderedFirstFrame(surface: Surface) {
        Log.d(TAG, "renderedFirstFrame [$surface]")
    }

    // DefaultDrmSessionManager.EventListener

    override fun onDrmSessionManagerError(e: Exception) {
        printInternalError("drmSessionManagerError", e)
    }

    override fun onDrmKeysRestored() {
        Log.d(TAG, "drmKeysRestored [${getSessionTimeString()}]")
    }

    override fun onDrmKeysRemoved() {
        Log.d(TAG, "drmKeysRemoved [${getSessionTimeString()}]")
    }

    override fun onDrmKeysLoaded() {
        Log.d(TAG, "drmKeysLoaded [${getSessionTimeString()}]")
    }

    // ExtractorMediaSource.EventListener

    override fun onLoadError(error: IOException) {
        printInternalError("loadError", error)
    }

    // AdaptiveMediaSourceEventListener

    override fun onLoadStarted(dataSpec: DataSpec, dataType: Int, trackType: Int,
                               trackFormat: Format, trackSelectionReason: Int,
                               trackSelectionData: Any, mediaStartTimeMs: Long,
                               mediaEndTimeMs: Long, elapsedRealtimeMs: Long) {
        // Do nothing.
    }

    override fun onLoadError(dataSpec: DataSpec, dataType: Int, trackType: Int,
                             trackFormat: Format, trackSelectionReason: Int,
                             trackSelectionData: Any, mediaStartTimeMs: Long,
                             mediaEndTimeMs: Long, elapsedRealtimeMs: Long,
                             loadDurationMs: Long, bytesLoaded: Long,
                             error: IOException, wasCanceled: Boolean) {
        printInternalError("loadError", error)
    }

    override fun onLoadCanceled(dataSpec: DataSpec, dataType: Int, trackType: Int,
                                trackFormat: Format, trackSelectionReason: Int,
                                trackSelectionData: Any, mediaStartTimeMs: Long,
                                mediaEndTimeMs: Long, elapsedRealtimeMs: Long,
                                loadDurationMs: Long, bytesLoaded: Long) {
        // Do nothing.
    }

    override fun onLoadCompleted(dataSpec: DataSpec, dataType: Int, trackType: Int,
                                 trackFormat: Format, trackSelectionReason: Int,
                                 trackSelectionData: Any, mediaStartTimeMs: Long,
                                 mediaEndTimeMs: Long, elapsedRealtimeMs: Long,
                                 loadDurationMs: Long, bytesLoaded: Long) {
        // Do nothing.
    }

    override fun onUpstreamDiscarded(trackType: Int,
                                     mediaStartTimeMs: Long,
                                     mediaEndTimeMs: Long) {
        // Do nothing.
    }

    override fun onDownstreamFormatChanged(trackType: Int, trackFormat: Format,
                                           trackSelectionReason: Int,
                                           trackSelectionData: Any, mediaTimeMs: Long) {
        // Do nothing.
    }

    // Internal methods

    private fun printInternalError(type: String, e: Exception?) {
        Log.e(TAG, "internalError [${getSessionTimeString()}, $type]", e)
    }

    private fun printMetadata(metadata: Metadata, prefix: String) = (0 until metadata.length())
            .map { metadata.get(it) }
            .forEach {
                when (it) {
                    is TextInformationFrame -> Log.d(TAG, "$prefix ${it.id}: value=${it.value}")
                    is UrlLinkFrame -> Log.d(TAG, "$prefix ${it.id}: url=${it.url}")
                    is PrivFrame -> Log.d(TAG, "$prefix ${it.id}: owner=${it.owner}")
                    is GeobFrame -> Log.d(TAG, "$prefix ${it.id}: mimeType=${it.mimeType}, " +
                            "filename=${it.filename}, description=${it.description}")
                    is ApicFrame -> Log.d(TAG, "$prefix ${it.id}: mimeType=${it.mimeType}, " +
                            "description=${it.description}")
                    is CommentFrame -> Log.d(TAG, "$prefix ${it.id}: language=${it.language}, " +
                            "description=${it.description}")
                    is Id3Frame -> Log.d(TAG, "$prefix ${it.id}")
                    is EventMessage -> Log.d(TAG, "$prefix EMSG: scheme=${it.schemeIdUri}, id=${it.id}, " +
                            "value=${it.value}")
                }

            }

    private fun getSessionTimeString(): String {
        return getTimeString(SystemClock.elapsedRealtime() - startTimeMs)
    }

    private fun Float.format(digits: Int) = String.format("%.${digits}f", this)

}