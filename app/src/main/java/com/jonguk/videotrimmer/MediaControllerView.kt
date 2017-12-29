package com.jonguk.videotrimmer

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Created by Jonguk on 2017. 12. 8..
 */
class MediaControllerView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        LinearLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_media_controller, this, true)
    }

    private val currentTimeView by lazy { findViewById<TextView>(R.id.current_time_view) }
    private val allTimeView by lazy { findViewById<TextView>(R.id.all_time_view) }
    private val expandView by lazy { findViewById<View>(R.id.expand_view) }



}