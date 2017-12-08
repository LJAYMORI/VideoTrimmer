package com.jonguk.videotrimmer

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity

/**
 * Created by Jonguk on 2017. 12. 5..
 */
class VideoGalleryActivity : AppCompatActivity() {

    companion object {
        @JvmStatic fun newIntent(context: Context): Intent = Intent(context, VideoGalleryActivity::class.java)
    }

//    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.recyclerview) }
//    private val galleryAdapter by lazy { GalleryAdapter() }
//    private val loaderManagerCallback by lazy { MediaLoaderCallback() }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_gallery)
//
//        recyclerView?.apply { adapter = galleryAdapter }
//
//        loadVideo()
//    }
//
//    private fun loadVideo() {
//        loaderManager.initLoader(0, null, )
//    }
//
//    // Data
//    private inner class GalleryData
//    constructor(val thumbnail:String, val name: String, val path: String)
//
//    // Adapter
//    private inner class GalleryAdapter: RecyclerView.Adapter<GalleryViewHolder>() {
//        private val items = ArrayList<GalleryData>()
//
//        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): GalleryViewHolder =
//                GalleryViewHolder(LayoutInflater.from(parent?.context)
//                        .inflate(R.layout.item_gallery, parent, false))
//
//        override fun onBindViewHolder(holder: GalleryViewHolder?, position: Int) {
//            holder?.apply {
//                val data = items[position]
//                GlideApp.with(itemView.context)
//                        .load(data.thumbnail)
//                        .into(thumbnail)
//
//                name.text = data.name
//            }
//        }
//
//        override fun getItemCount(): Int = items.size
//    }
//
//    // ViewHolder
//    private inner class GalleryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val thumbnail = view.findViewById<ImageView>(R.id.gallery_item_thumbnail)!!
//        val name = view.findViewById<TextView>(R.id.gallery_item_name)!!
//    }
//
//    // Loader
//    private inner class MediaLoaderCallback : LoaderManager.LoaderCallbacks<Cursor> {
//
//
//        override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
//            val map = MediaListMap()
//            if (data?.moveToFirst() == true) {
//                val cr = context.getContentResolver()
//                val factory = LocalMedia.CursorFactory(cr, data)
//                val bucketIndex = data.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
//                do {
//                    val localMedia = factory.create()
//                    map.put("All Photos", localMedia)
//                    if (bucketIndex >= 0) {
//                        val bucket = data.getString(bucketIndex)
//                        map.put(bucket, localMedia)
//                    }
//                } while (data.moveToNext())
//            }
//            onLoadFinished(map)
//        }
//
//
//        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> =
//            CursorLoader(this@VideoGalleryActivity,
//                    MediaStore.Files.getContentUri("external"),
//                    getProjection(), SELECTION, null,
//                    MediaStore.Images.Media.DATE_MODIFIED + " DESC")
//
//        override fun onLoaderReset(loader: Loader<Cursor>?) {
//
//        }
//
//        private fun getProjection() =
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) PROJECTION
//                else PROJECTION_PRE_JB
//
//    }

}