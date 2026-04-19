package com.minimal.gallery.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.minimal.gallery.domain.model.FolderInfo
import com.minimal.gallery.domain.model.MediaItem
import com.minimal.gallery.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    fun getMediaStream(excludedFolders: Set<String>): Flow<PagingData<MediaItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                enablePlaceholders = false,
                prefetchDistance = 20
            ),
            pagingSourceFactory = {
                MediaPagingSource(context, excludedFolders)
            }
        ).flow.flowOn(Dispatchers.IO)
    }

    suspend fun getAllFolders(): List<FolderInfo> = withContext(Dispatchers.IO) {
        val folders = mutableMapOf<String, FolderInfo>()
        
        val projection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        
        val imagesQuery = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )
        
        imagesQuery?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                val folderPath = path.substringBeforeLast("/")
                val folderName = cursor.getString(bucketColumn)
                
                if (!folders.containsKey(folderPath)) {
                    folders[folderPath] = FolderInfo(
                        path = folderPath,
                        name = folderName,
                        isExcluded = false,
                        mediaCount = 0
                    )
                }
                folders[folderPath] = folders[folderPath]!!.copy(
                    mediaCount = folders[folderPath]!!.mediaCount + 1
                )
            }
        }
        
        // Also check videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
        
        val videosQuery = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            null,
            null,
            null
        )
        
        videosQuery?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                val folderPath = path.substringBeforeLast("/")
                val folderName = cursor.getString(bucketColumn)
                
                if (!folders.containsKey(folderPath)) {
                    folders[folderPath] = FolderInfo(
                        path = folderPath,
                        name = folderName,
                        isExcluded = false,
                        mediaCount = 0
                    )
                }
                folders[folderPath] = folders[folderPath]!!.copy(
                    mediaCount = folders[folderPath]!!.mediaCount + 1
                )
            }
        }
        
        folders.values.toList().sortedBy { it.name.lowercase() }
    }

    suspend fun getMediaType(uri: android.net.Uri): MediaType = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != null && mimeType.startsWith("video/")) {
            MediaType.VIDEO
        } else {
            MediaType.IMAGE
        }
    }
}
