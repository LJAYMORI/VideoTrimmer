package com.jonguk.videotrimmer

import android.content.Context
import android.net.Uri
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import kotlinx.android.synthetic.main.view_range_tirmmer.view.*

/**
 * Created by Jonguk on 2018. 1. 2..
 */
class RangeTrimView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        ConstraintLayout(context, attrs, defStyleAttr) {

    private val recyclerView by lazy { range_trimmer_thumbnails_recycler }
    private val startTimeView by lazy { range_trimmer_start_time_view }
    private val endTimeView by lazy { range_trimmer_end_time_view }
    private val startHandleView by lazy { range_trimmer_start_handle_view }
    private val endHandleView by lazy { range_trimmer_end_handle_view }
    private val currentHandleView by lazy { range_trimmer_current_handle_view }
    private val startGradientView by lazy { range_trimmer_start_gradient_view }
    private val endGradientView by lazy { range_trimmer_end_gradient_view }

    private var startHandleFirstEventX: Float = 0f
    private var endHandleFirstEventX: Float = 0f

    private var sidePadding: Int = 0

    var handleChangeListener: OnHandleChangeListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_range_tirmmer, this, true)

        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.RangeTrimView)
            sidePadding = a.getInt(R.styleable.RangeTrimView_range_trimmer_sidePadding,
                    resources.getDimensionPixelSize(R.dimen.range_trim_thumbnails_default_side_padding))
            a.recycle()
        }

        startHandleView.setOnTouchListener { handle, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    startHandleFirstEventX = event.rawX
                }
                MotionEvent.ACTION_MOVE -> {
                    val handleLeft = handle.x.toInt()
                    if (handleLeft >= sidePadding) {
                        handle.translationX = event.rawX - startHandleFirstEventX
                        startGradientView.layout(0, handle.top, handleLeft + handle.width, handle.bottom)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val handleLeft = Math.max(handle.x.toInt(), sidePadding)
                    val handleRight = handleLeft + handle.width
                    val handleTop = handle.top
                    val handleBottom = handle.bottom
                    handle.translationX = 0f
                    handle.layout(handleLeft, handleTop, handleRight, handleBottom)
                    startGradientView.layout(0, handleTop, handleRight, handleBottom)
                }
            }
            true
        }

        endHandleView.setOnTouchListener { handle, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    endHandleFirstEventX = event.rawX
                }
                MotionEvent.ACTION_MOVE -> {
                    val parentRight = this.right
                    val handleRightPadding = parentRight - (handle.x.toInt() + handle.width)
                    if (handleRightPadding >= sidePadding) {
                        handle.translationX = event.rawX - endHandleFirstEventX
                        endGradientView.layout(handle.x.toInt(), handle.top, parentRight, handle.bottom)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val parentRight = this.right
                    val handleWidth = handle.width
                    val handleLeft = Math.min(handle.x.toInt(), parentRight - sidePadding - handleWidth)
                    val handleRight = handleLeft + handleWidth
                    val handleTop = handle.top
                    val handleBottom = handle.bottom
                    handle.translationX = 0f
                    handle.layout(handleLeft, handleTop, handleRight, handleBottom)
                    endGradientView.layout(handleLeft, handleTop, parentRight, handleBottom)
                }
            }
            true
        }
    }

    var videoUri: Uri? = null

    interface OnHandleChangeListener {
        fun onStartHandleChanged(byUser: Boolean, position: Int)
        fun onEndHandleChanged(byUser: Boolean, position: Int)
    }

}