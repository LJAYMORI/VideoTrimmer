package com.jonguk.videotrimmer.exo;

import android.support.annotation.StringRes;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.jonguk.videotrimmer.R;

import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Function;


public class PlayerEventListener extends Player.DefaultEventListener {

    private DefaultTrackSelector trackSelector;
    private TrackGroupArray lastSeenTrackGroupArray;

    private boolean inErrorState;
    private Runnable updateResumePositionCallback;
    private Runnable showControlCallback;
    private Runnable updateButtonVisibilitiesCallback;
    private Runnable initializePlayerCallback;
    private Runnable clearResumePositionCallback;
    private Function<ExoPlaybackException, Boolean> isBehindLiveWindowCallback;
    private BiConsumer<Integer, Object> messageCallback;

    private PlayerEventListener() {}

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == Player.STATE_ENDED) {
            showControls();
        }

        updateResumePosition();
    }

    @Override
    public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
        if (inErrorState) {
            // This will only occur if the user has performed a seek whilst in the error state. Update
            // the resume position so that if the user then retries, playback will resume from the
            // position to which they seeked.
            updateResumePosition();
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {
        if (e.type == ExoPlaybackException.TYPE_RENDERER) {
            Exception cause = e.getRendererException();
            if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                // Special case for decoder initialization failures.
                MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
                        (MediaCodecRenderer.DecoderInitializationException) cause;
                if (decoderInitializationException.decoderName == null) {
                    if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                        messageCallback(R.string.error_querying_decoders, null);
                    } else if (decoderInitializationException.secureDecoderRequired) {
                        messageCallback(R.string.error_no_secure_decoder,
                                decoderInitializationException.mimeType);
                    } else {
                        messageCallback(R.string.error_no_decoder,
                                decoderInitializationException.mimeType);
                    }
                } else {
                    messageCallback(R.string.error_instantiating_decoder,
                            decoderInitializationException.decoderName);
                }
            }
        }

        inErrorState = true;
        if (isBehindLiveWindowCallback(e)) {
            clearResumePosition();
            initializePlayer();
        } else {
            updateResumePosition();
            updateButtonVisibilities();
            showControls();
        }
    }

    @Override
    @SuppressWarnings("ReferenceEquality")
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        updateButtonVisibilities();
        if (trackGroups != lastSeenTrackGroupArray) {
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            if (mappedTrackInfo != null) {
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
                        == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    messageCallback(R.string.error_unsupported_video, null);
                }
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
                        == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    messageCallback(R.string.error_unsupported_audio, null);
                }
            }
            lastSeenTrackGroupArray = trackGroups;
        }
    }

    public void setErrorState(boolean state) {
        inErrorState = state;
    }

    public boolean isInErrorState() {
        return inErrorState;
    }

    private void showControls() {
        if (showControlCallback != null) {
            showControlCallback.run();
        }
    }

    private void updateResumePosition() {
        if (updateResumePositionCallback != null) {
            updateResumePositionCallback.run();
        }
    }

    private void updateButtonVisibilities() {
        if (updateButtonVisibilitiesCallback != null) {
            updateButtonVisibilitiesCallback.run();
        }
    }

    private void messageCallback(@StringRes int resId, Object args) {
        if (messageCallback != null) {
            try {
                messageCallback.accept(resId, args);
            } catch (Exception ignore) {}
        }
    }

    private boolean isBehindLiveWindowCallback(ExoPlaybackException e) {
        if (isBehindLiveWindowCallback != null) {
            try {
                return isBehindLiveWindowCallback.apply(e);
            } catch (Exception ignore) {}
        }
        return false;
    }

    private void clearResumePosition() {
        if (clearResumePositionCallback != null) {
            clearResumePositionCallback.run();
        }
    }

    private void initializePlayer() {
        if (initializePlayerCallback != null) {
            initializePlayerCallback.run();
        }
    }

    public static class Builder {
        private DefaultTrackSelector trackSelector;

        private Runnable updateResumePositionCallback;
        private Runnable showControlCallback;
        private Runnable updateButtonVisibilitiesCallback;
        private Runnable initializePlayerCallback;
        private Runnable clearResumePositionCallback;
        private Function<ExoPlaybackException, Boolean> isBehindLiveWindowCallback;
        private BiConsumer<Integer, Object> messageCallback;

        public Builder trackSelector(DefaultTrackSelector trackSelector) {
            this.trackSelector = trackSelector;
            return this;
        }

        public Builder updateResumePositionCallback(Runnable updateResumePositionCallback) {
            this.updateResumePositionCallback = updateResumePositionCallback;
            return this;
        }

        public Builder showControlCallback(Runnable showControlCallback) {
            this.showControlCallback = showControlCallback;
            return this;
        }

        public Builder updateButtonVisibilitiesCallback(Runnable updateButtonVisibilitiesCallback) {
            this.updateButtonVisibilitiesCallback = updateButtonVisibilitiesCallback;
            return this;
        }

        public Builder initializePlayerCallback(Runnable initializePlayerCallback) {
            this.initializePlayerCallback = initializePlayerCallback;
            return this;
        }

        public Builder clearResumePositionCallback(Runnable clearResumePositionCallback) {
            this.clearResumePositionCallback = clearResumePositionCallback;
            return this;
        }

        public Builder isBehindLiveWindowCallback(Function<ExoPlaybackException, Boolean> isBehindLiveWindowCallback) {
            this.isBehindLiveWindowCallback = isBehindLiveWindowCallback;
            return this;
        }

        public Builder messageCallback(BiConsumer<Integer, Object> messageCallback) {
            this.messageCallback = messageCallback;
            return this;
        }

        public PlayerEventListener build() {
            if (trackSelector == null) {
                throw new IllegalArgumentException("trackSelector should be initialized");
            }

            PlayerEventListener listener = new PlayerEventListener();
            listener.trackSelector = trackSelector;
            listener.updateResumePositionCallback = updateResumePositionCallback;
            listener.showControlCallback = showControlCallback;
            listener.updateButtonVisibilitiesCallback = updateButtonVisibilitiesCallback;
            listener.initializePlayerCallback = initializePlayerCallback;
            listener.clearResumePositionCallback = clearResumePositionCallback;
            listener.isBehindLiveWindowCallback = isBehindLiveWindowCallback;
            listener.messageCallback = messageCallback;
            return listener;
        }
    }

}