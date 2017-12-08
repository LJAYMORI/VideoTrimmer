package com.jonguk.videotrimmer

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader

/**
 * Created by Jonguk on 2017. 12. 5..
 */
abstract class MediaLoaderCallback(private val context: Context) : LoaderManager.LoaderCallbacks<Cursor> {
    companion object {
        @JvmStatic val PROJECTION_PRE_JB = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Images.ImageColumns.ORIENTATION,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Video.VideoColumns.DURATION)

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
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
//        val map = MediaListMap()
//        if (data?.moveToFirst() == true) {
//            val cr = context.contentResolver
//            val factory = LocalMedia.CursorFactory(cr, data)
//            val bucketIndex = data.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
//            do {
//                val localMedia = factory.create()
//                map.put("All Photos", localMedia)
//                if (bucketIndex >= 0) {
//                    val bucket = data.getString(bucketIndex)
//                    map.put(bucket, localMedia)
//                }
//            } while (data.moveToNext())
//        }
//        onLoadFinished(map)
    }


    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> =
            CursorLoader(context,
                    MediaStore.Files.getContentUri("external"),
                    getProjection(), SELECTION, null,
                    MediaStore.Images.Media.DATE_MODIFIED + " DESC")

    override fun onLoaderReset(loader: Loader<Cursor>?) {

    }

    private fun getProjection() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) PROJECTION
            else PROJECTION_PRE_JB

    abstract fun onLoadFinished()

}