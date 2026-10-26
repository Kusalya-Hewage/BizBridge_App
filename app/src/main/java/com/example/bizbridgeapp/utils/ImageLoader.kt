package com.example.bizbridgeapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.LruCache
import android.widget.ImageView
import com.example.bizbridgeapp.R
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object ImageLoader {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newFixedThreadPool(4)

    // Memory Cache (up to 1/8th of available memory)
    private val memoryCache: LruCache<String, Bitmap> = run {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    /**
     * Loads an image from a URL, local file path, content URI, or Base64 string into an ImageView.
     */
    fun loadImage(
        imageView: ImageView,
        source: String?,
        fallbackResId: Int = R.drawable.img_salon_interior
    ) {
        if (source.isNullOrBlank()) {
            imageView.tag = null
            imageView.setImageResource(fallbackResId)
            return
        }

        val trimmed = source.trim()
        imageView.tag = trimmed

        // Check in-memory cache first
        val cached = memoryCache.get(trimmed)
        if (cached != null && !cached.isRecycled) {
            imageView.setImageBitmap(cached)
            return
        }

        // Set fallback/placeholder while loading
        imageView.setImageResource(fallbackResId)

        // Load asynchronously in background
        executor.execute {
            val bitmap = tryLoadBitmap(imageView.context, trimmed)
            if (bitmap != null) {
                memoryCache.put(trimmed, bitmap)
                mainHandler.post {
                    if (imageView.tag == trimmed) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } else {
                mainHandler.post {
                    if (imageView.tag == trimmed) {
                        imageView.setImageResource(fallbackResId)
                    }
                }
            }
        }
    }

    /**
     * Attempts to decode a bitmap from web URL, content URI, file path, or Base64.
     */
    private fun tryLoadBitmap(context: Context, source: String): Bitmap? {
        return try {
            when {
                source.startsWith("http://") || source.startsWith("https://") -> {
                    loadFromUrl(source)
                }
                source.startsWith("content://") -> {
                    val uri = Uri.parse(source)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        decodeSampledBitmap(stream)
                    }
                }
                source.startsWith("file://") -> {
                    val path = source.removePrefix("file://")
                    decodeFileSampled(File(path))
                }
                File(source).exists() -> {
                    decodeFileSampled(File(source))
                }
                source.startsWith("data:image") -> {
                    val base64Data = source.substringAfter(",")
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                else -> {
                    // Try parsing as URI directly
                    try {
                        val uri = Uri.parse(source)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            decodeSampledBitmap(stream)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun loadFromUrl(urlString: String): Bitmap? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.instanceFollowRedirects = true
            conn.doInput = true
            conn.connect()

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedInputStream(conn.inputStream).use { bis ->
                    decodeSampledBitmap(bis)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun decodeFileSampled(file: File, maxDim: Int = 1024): Bitmap? {
        if (!file.exists() || !file.canRead()) return null
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.inSampleSize = calculateInSampleSize(options, maxDim, maxDim)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeSampledBitmap(inputStream: InputStream, maxDim: Int = 1024): Bitmap? {
        return try {
            val bytes = inputStream.readBytes()
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            options.inSampleSize = calculateInSampleSize(options, maxDim, maxDim)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Copies a picked image Uri into app's persistent internal storage and returns the local file path.
     */
    fun saveImageLocally(context: Context, sourceUri: Uri, businessId: String): String? {
        return try {
            val dir = File(context.filesDir, "business_covers")
            if (!dir.exists()) dir.mkdirs()
            val safeId = businessId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val targetFile = File(dir, "${safeId}_cover.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Copies a picked profile image Uri into app's persistent internal storage and returns the local file path.
     */
    fun saveProfileImageLocally(context: Context, sourceUri: Uri, uid: String): String? {
        return try {
            val dir = File(context.filesDir, "profile_images")
            if (!dir.exists()) dir.mkdirs()
            val safeId = uid.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val targetFile = File(dir, "${safeId}_profile.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
