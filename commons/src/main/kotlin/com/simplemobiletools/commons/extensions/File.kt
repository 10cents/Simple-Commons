package com.simplemobiletools.commons.extensions

import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.MediaMetadataRetriever
import java.io.File

fun File.isImageVideoGif() = absolutePath.isImageFast() || absolutePath.isVideoFast() || absolutePath.isGif()
fun File.isGif() = absolutePath.endsWith(".gif", true)
fun File.isVideoFast() = absolutePath.videoExtensions.any { absolutePath.endsWith(it, true) }
fun File.isImageFast() = absolutePath.photoExtensions.any { absolutePath.endsWith(it, true) }

fun File.isImageSlow() = absolutePath.isImageFast() || getMimeType().startsWith("image")
fun File.isVideoSlow() = absolutePath.isVideoFast() || getMimeType().startsWith("video")
fun File.isAudioSlow() = getMimeType().startsWith("audio")

fun File.getMimeType(default: String = getDefaultMimeType()): String {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
    } catch (ignored: Exception) {
        default
    }
}

fun File.getDefaultMimeType() = if (isVideoFast()) "video/*" else if (isImageFast()) "image/*" else ""

fun File.getDuration(): String {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    val timeInMs = java.lang.Long.parseLong(time)
    return (timeInMs / 1000).toInt().getFormattedDuration()
}

fun File.getArtist(): String? {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
}

fun File.getAlbum(): String? {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
}

fun File.getResolution(): Point {
    return if (isImageFast() || isImageSlow()) {
        getImageResolution()
    } else if (isVideoFast() || isVideoSlow()) {
        getVideoResolution()
    } else {
        return Point(0, 0)
    }
}

fun File.getVideoResolution(): Point {
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
        return Point(width, height)
    } catch (ignored: Exception) {

    }
    return Point(0, 0)
}

fun File.getImageResolution(): Point {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(path, options)
    return Point(options.outWidth, options.outHeight)
}
