package com.jonguk.videotrimmer.gallery

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.jonguk.videotrimmer.R
import com.jonguk.videotrimmer.utils.Constant
import kotlinx.android.synthetic.main.activity_gallery.*

/**
 * Created by Jonguk on 2017. 12. 5..
 */
class VideoGalleryActivity : AppCompatActivity() {

    companion object {
        @JvmStatic val REQ_CODE_GALLERY = 123
        @JvmStatic fun newIntent(context: Context): Intent = Intent(context, VideoGalleryActivity::class.java)

        @JvmStatic val PROJECTION = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.WIDTH,
                MediaStore.Files.FileColumns.HEIGHT,
                MediaStore.Images.ImageColumns.ORIENTATION,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Video.VideoColumns.DURATION)

        @JvmStatic val SELECTION = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
    }

    private val recyclerView by lazy { gallery_recyclerview }
    private val loadView by lazy { gallery_load_view }
    private val galleryAdapter by lazy { GalleryAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        recyclerView.apply { adapter = galleryAdapter }
        loadView.setOnClickListener {
            galleryAdapter.getSelectedUriPath()?.let {
                val data = Intent()
                data.putExtra(Constant.VIDEO_URI, it)
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }

        loadVideo()
    }

    private fun loadVideo() {
        supportLoaderManager?.initLoader(0, null, object : MediaLoaderCallback() {
            override fun onLoadFinished(list: ArrayList<GalleryData>) {
                galleryAdapter.items = list
            }
        })
    }

    // Data
    private inner class GalleryData
    constructor(val uriPath: String)

    // Adapter
    private inner class GalleryAdapter: RecyclerView.Adapter<GalleryViewHolder>() {
        var items = ArrayList<GalleryData>()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        private var selectedPosition: Int = -1

        fun getSelectedUriPath() : String? {
            if (selectedPosition in 0..(items.size - 1)) {
                return items[selectedPosition].uriPath
            }
            return null
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): GalleryViewHolder {
            val viewHolder = GalleryViewHolder(LayoutInflater.from(parent?.context)
                    .inflate(R.layout.item_gallery, parent, false))
            viewHolder.itemView.setOnClickListener { selectedPosition = viewHolder.adapterPosition }
            return viewHolder
        }

        override fun onBindViewHolder(holder: GalleryViewHolder?, position: Int) {
            holder?.apply {
                val data = items[position]
                name.text = data.uriPath
            }
        }

        override fun getItemCount(): Int = items.size
    }

    // ViewHolder
    private inner class GalleryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail = view.findViewById<ImageView>(R.id.gallery_item_thumbnail)!!
        val name = view.findViewById<TextView>(R.id.gallery_item_name)!!
    }

    // Loader
    private abstract inner class MediaLoaderCallback : LoaderManager.LoaderCallbacks<Cursor> {

        override fun onLoaderReset(loader: Loader<Cursor>?) {}

        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> =
                CursorLoader(this@VideoGalleryActivity,
                        MediaStore.Files.getContentUri("external"),
                        PROJECTION, SELECTION, null,
                        MediaStore.Images.Media.DATE_MODIFIED + " DESC")

        override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
            data?.let {
                if (it.moveToFirst()) {
                    val colIdxId = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val colIdxData = it.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    val colIdxMimeType = it.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                    val colIdMediaType = it.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val colIdxWidth = it.getColumnIndex(MediaStore.Files.FileColumns.WIDTH)
                    val colIdxHeight = it.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT)
                    val colIdxOrientation = it.getColumnIndex(MediaStore.Images.Media.ORIENTATION)
                    val colIdxDuration = it.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)

                    val cr = contentResolver

                    val list: ArrayList<GalleryData> = arrayListOf()
                    do {
                        when (data.getInt(colIdMediaType)) {
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                                val id = data.getLong(colIdxId)
                                val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                                list.add(GalleryData(uri.toString()))
                            }
                        }

                    } while (it.moveToNext())
                    onLoadFinished(list)
                }
            }
        }

        abstract fun onLoadFinished(list: ArrayList<GalleryData>)

    }

}