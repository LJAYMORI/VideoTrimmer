package com.jonguk.videotrimmer.utils.trimmer

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import com.jonguk.videotrimmer.R
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_range_tirmmer.view.*

/**
 * Created by Jonguk on 2018. 1. 2..
 */
class RangeTrimView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        private val MIN_TIMELINE_FRAME = 5
        private val MAX_TIMELINE_FRAME = 100
        private val DEFAULT_INTERVAL_MS = 1000
    }

    private val startTimeView by lazy { range_trimmer_start_time_view }
    private val endTimeView by lazy { range_trimmer_end_time_view }
    private val startHandleView by lazy { range_trimmer_start_handle_view }
    private val endHandleView by lazy { range_trimmer_end_handle_view }
    private val currentHandleView by lazy { range_trimmer_current_handle_view }
    private val startGradientView by lazy { range_trimmer_start_gradient_view }
    private val endGradientView by lazy { range_trimmer_end_gradient_view }

    private val recyclerView by lazy { range_trimmer_thumbnails_recycler }
    private val thumbnailLayoutManager by lazy { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
    private val thumbnailAdapter: ThumbnailsAdapter

    private var thumbnailDisposable: Disposable? = null

    private var minWidthOfStartWithEnd: Int = 0
    private var maxWidthOfStartWithEnd: Int = 0
    private var thumbnailWidth: Int = 0
    private var thumbnailHeight: Int = 0
    private var trimmerSidePadding: Int = 0
    private var minThumbnailFrame: Int = MIN_TIMELINE_FRAME
    private var maxThumbnailFrame: Int = MAX_TIMELINE_FRAME
    private var intervalMs: Int = DEFAULT_INTERVAL_MS

    var handleChangeListener: OnHandleChangeListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_range_tirmmer, this, true)

        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.RangeTrimView)
            trimmerSidePadding = a.getInt(R.styleable.RangeTrimView_range_trimmer_sidePadding,
                    resources.getDimensionPixelSize(R.dimen.range_trim_thumbnails_default_side_padding))
            val trimmerHandleWidth = a.getInt(R.styleable.RangeTrimView_range_trimmer_handleWidth,
                    resources.getDimensionPixelSize(R.dimen.range_trim_trimmer_default_handle_width))
            minWidthOfStartWithEnd = a.getInt(R.styleable.RangeTrimView_range_trimmer_widthOfStartAndEnd,
                    resources.getDimensionPixelSize(R.dimen.range_trim_thumbnails_default_width_of_start_and_end))
            thumbnailWidth = a.getInt(R.styleable.RangeTrimView_range_trimmer_thumbnailWidth,
                    resources.getDimensionPixelSize(R.dimen.range_trim_thumbnails_default_width))
            thumbnailHeight = a.getInt(R.styleable.RangeTrimView_range_trimmer_thumbnailHeight,
                    resources.getDimensionPixelSize(R.dimen.range_trim_thumbnails_default_height))
            minThumbnailFrame = Math.max(MIN_TIMELINE_FRAME,
                    a.getInt(R.styleable.RangeTrimView_range_trimmer_minFrame, MIN_TIMELINE_FRAME))
            maxThumbnailFrame = Math.min(MAX_TIMELINE_FRAME,
                    a.getInt(R.styleable.RangeTrimView_range_trimmer_maxFrame, MAX_TIMELINE_FRAME))
            intervalMs = Math.max(1,
                    a.getInt(R.styleable.RangeTrimView_range_trimmer_interval_ms, DEFAULT_INTERVAL_MS))

            // TODO think..
            maxWidthOfStartWithEnd = minWidthOfStartWithEnd shl 2

            a.recycle()

            val recyclerPadding = trimmerHandleWidth + trimmerSidePadding
            recyclerView.setPadding(recyclerPadding, 0, recyclerPadding, 0)
        }

        thumbnailAdapter = ThumbnailsAdapter(thumbnailWidth)
        recyclerView.apply {
            layoutManager = thumbnailLayoutManager
            adapter = thumbnailAdapter
        }

        startHandleView.setOnTouchListener { handle, event ->
            when (event?.action) {
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_UP -> updateStartHandleLocation(handle, event.rawX.toInt())
            }
            true
        }

        endHandleView.setOnTouchListener { handle, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_UP -> updateEndHandleLocation(handle, event.rawX.toInt())
            }
            true
        }
    }

    var videoUri: Uri? = null
        set(value) {
            field = value
            thumbnailDisposable?.apply {
                if (!isDisposed) {
                    dispose()
                }
            }
            thumbnailDisposable = Single.just(value)
                    .observeOn(Schedulers.io())
                    .map {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, it)
                        val videoLengthMs = retriever.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()

                        val interval: Int = when (videoLengthMs / intervalMs) {
                            in 0 until minThumbnailFrame -> (videoLengthMs / minThumbnailFrame).toInt()
                            in minThumbnailFrame..maxThumbnailFrame -> intervalMs
                            else -> (videoLengthMs / maxThumbnailFrame).toInt()
                        }

                        val count = (videoLengthMs / interval).toInt()
                        val list = arrayListOf<Bitmap>()
                        (0 until count)
                                .map {
                                    retriever.getFrameAtTime((it * interval).toLong(),
                                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                }
                                .forEach {
                                    try {
                                        list.add(Bitmap.createScaledBitmap(
                                                it, thumbnailWidth, thumbnailHeight, false))
                                    } catch (ignore: Exception) { }
                                }
                        retriever.release()
                        list
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ thumbnailAdapter.setItems(it) }, {})
        }

    override fun onDetachedFromWindow() {
        thumbnailDisposable?.apply {
            if (!isDisposed) {
                dispose()
            }
        }
        super.onDetachedFromWindow()
    }

    private fun updateStartHandleLocation(handle: View, x: Int) {
        val parentRight = this.right
        val handleWidth = handle.width
        val handleTop = handle.top
        val handleBottom = handle.bottom
        val handleLeft = Math.min(
                Math.max(x, trimmerSidePadding),
                parentRight - trimmerSidePadding - minWidthOfStartWithEnd - handleWidth)
        val handleRight = handleLeft + handleWidth
        handle.layout(handleLeft, handleTop, handleRight, handleBottom)
        startGradientView.layout(0, handleTop, handleRight, handleBottom)

        // TODO Control thumbnails recycler-view scroll to left and expand width of start and end if needed
        val endHandleTop = endHandleView.top
        val endHandleBottom = endHandleView.bottom
        val widthOfStartAndEnd = endHandleView.left - handleLeft
        if (widthOfStartAndEnd > maxWidthOfStartWithEnd) {
            val endHandleLeft = handleLeft + maxWidthOfStartWithEnd
            endHandleView.layout(endHandleLeft, endHandleTop,
                    endHandleLeft + endHandleView.width, endHandleBottom)
            endGradientView.layout(endHandleLeft, endHandleTop, parentRight, endHandleBottom)

        } else if (widthOfStartAndEnd < minWidthOfStartWithEnd) {
            val endHandleLeft = handleLeft + minWidthOfStartWithEnd
            endHandleView.layout(endHandleLeft, endHandleTop,
                    endHandleLeft + endHandleView.width, endHandleBottom)
            endGradientView.layout(endHandleLeft, endHandleTop,
                    parentRight, endHandleBottom)
        }
    }

    private fun updateEndHandleLocation(handle: View, x: Int) {
        val parentRight = this.right
        val startHandleWidth = startHandleView.width
        val handleWidth = handle.width
        val handleTop = handle.top
        val handleBottom = handle.bottom
        val handleLeft = Math.min(
                Math.max(x, trimmerSidePadding + minWidthOfStartWithEnd + startHandleWidth),
                parentRight - trimmerSidePadding - handleWidth)
        val handleRight = handleLeft + handleWidth
        handle.layout(handleLeft, handleTop, handleRight, handleBottom)
        endGradientView.layout(handleLeft, handleTop, parentRight, handleBottom)

        // TODO Control thumbnails recycler-view scroll to right and expand width of start and end if needed
        val startHandleTop = startHandleView.top
        val startHandleBottom = startHandleView.bottom
        val widthOfStartAndEnd = handleLeft - startHandleView.left - startHandleWidth
        if (widthOfStartAndEnd > maxWidthOfStartWithEnd) {
            val startHandleLeft = handleLeft - startHandleWidth - maxWidthOfStartWithEnd
            val startHandleRight = startHandleLeft + startHandleWidth
            startHandleView.layout(startHandleLeft, startHandleTop, startHandleRight, startHandleBottom)
            startGradientView.layout(0, startHandleTop, startHandleRight, startHandleBottom)
        } else if (widthOfStartAndEnd < minWidthOfStartWithEnd) {
            val startHandleLeft = handleLeft - startHandleWidth - minWidthOfStartWithEnd
            val startHandleRight = startHandleLeft + startHandleWidth
            startHandleView.layout(startHandleLeft, startHandleTop, startHandleRight, startHandleBottom)
            startGradientView.layout(0, startHandleTop, startHandleRight, startHandleBottom)
        }
    }

    interface OnHandleChangeListener {
        fun onStartHandleChanged(byUser: Boolean, position: Int)
        fun onEndHandleChanged(byUser: Boolean, position: Int)
    }

}