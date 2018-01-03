package com.jonguk.videotrimmer

import android.content.Context
import android.net.Uri
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import kotlinx.android.synthetic.main.view_range_tirmmer.view.*

/**
 * Created by Jonguk on 2018. 1. 2..
 */
class RangeTrimView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        ConstraintLayout(context, attrs, defStyleAttr) {

    private val startTimeView by lazy { range_trimmer_start_time_view }
    private val endTimeView by lazy { range_trimmer_end_time_view }
    private val startHandleView by lazy { range_trimmer_start_handle_view }
    private val endHandleView by lazy { range_trimmer_end_handle_view }
    private val currentHandleView by lazy { range_trimmer_current_handle_view }
    private val startGradientView by lazy { range_trimmer_start_gradient_view }
    private val endGradientView by lazy { range_trimmer_end_gradient_view }

    private val recyclerView by lazy { range_trimmer_thumbnails_recycler }
    private val layoutManager by lazy { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }

    private var minWidthOfStartWithEnd: Int = 0
    private var maxWidthOfStartWithEnd: Int = 0

    private var trimmerSidePadding: Int = 0

    var handleChangeListener: OnHandleChangeListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_range_tirmmer, this, true)

        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.RangeTrimView)
            trimmerSidePadding = a.getInt(R.styleable.RangeTrimView_range_trimmer_sidePadding,
                    resources.getDimensionPixelSize(R.dimen.range_trim_thumbnails_default_side_padding))
            minWidthOfStartWithEnd = a.getInt(R.styleable.RangeTrimView_range_trimmer_widthOfStartAndEnd,
                    resources.getDimensionPixelSize(R.dimen.range_trim_thumbnails_default_width_of_start_and_end))

            // TODO think..
            maxWidthOfStartWithEnd = minWidthOfStartWithEnd shl 2

            a.recycle()
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

    private fun updateStartHandleLocation(handle: View, x: Int) {
        val parentRight = this.right
        val handleWidth = handle.width
        val handleTop = handle.top
        val handleBottom = handle.bottom
        val handleLeft = when {
            x < trimmerSidePadding -> trimmerSidePadding
            x > parentRight - trimmerSidePadding - minWidthOfStartWithEnd - handleWidth ->
                parentRight - trimmerSidePadding - minWidthOfStartWithEnd - handleWidth
            else -> x
        }
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
        val handleLeft = when {
            x < trimmerSidePadding + minWidthOfStartWithEnd + startHandleWidth ->
                trimmerSidePadding + minWidthOfStartWithEnd + startHandleWidth
            x > parentRight - trimmerSidePadding - handleWidth ->
                parentRight - trimmerSidePadding - handleWidth
            else -> x
        }
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