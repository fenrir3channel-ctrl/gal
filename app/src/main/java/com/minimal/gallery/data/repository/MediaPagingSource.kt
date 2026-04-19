package com.minimal.gallery.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.minimal.gallery.domain.model.MediaItem
import com.minimal.gallery.domain.model.MediaType

class MediaPagingSource(
    private val context: Context,
    private val excludedFolders: Set<String>
) : PagingSource<Long, MediaItem>() {

    companion object {
        private const val STARTING_PAGE_INDEX = 0L
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, MediaItem> {
        return try {
            val position = params.key ?: STARTING_PAGE_INDEX
            val limit = params.loadSize.toLong()

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DURATION
            )

            // Build exclusion filter
            val selectionBuilder = StringBuilder("${MediaStore.Images.Media.MIME_TYPE} LIKE 'image/%' OR ${MediaStore.Images.Media.MIME_TYPE} LIKE 'video/%'")
            
            if (excludedFolders.isNotEmpty()) {
                val exclusionClause = excludedFolders.joinToString(" AND ", prefix = " AND (") { 
                    "${MediaStore.Images.Media.DATA} NOT LIKE ?" 
                } + ")"
                selectionBuilder.append(exclusionClause)
            }

            val selectionArgs = if (excludedFolders.isNotEmpty()) {
                excludedFolders.map { "%$it%" }.toTypedArray()
            } else {
                emptyArray()
            }

            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

            val uri = MediaStore.Files.getContentUri("external")
            
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selectionBuilder.toString(),
                selectionArgs,
                sortOrder
            )

            val mediaItems = mutableListOf<MediaItem>()
            
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val bucketColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val displayName = it.getString(nameColumn)
                    val dateModified = it.getLong(dateColumn) * 1000 // Convert to milliseconds
                    val path = it.getString(dataColumn)
                    val folderPath = path.substringBeforeLast("/")
                    val folderName = it.getString(bucketColumn)
                    val duration = if (it.isNull(durationColumn)) null else it.getLong(durationColumn)

                    // Skip excluded folders
                    if (excludedFolders.any { excluded -> path.contains(excluded) }) {
                        continue
                    }

                    val mimeType = context.contentResolver.getType(
                        ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                    )
                    
                    val type = if (mimeType != null && mimeType.startsWith("video/")) {
                        MediaType.VIDEO
                    } else {
                        MediaType.IMAGE
                    }

                    mediaItems.add(
                        MediaItem(
                            id = id,
                            uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id),
                            type = type,
                            dateModified = dateModified,
                            folderName = folderName,
                            folderPath = folderPath,
                            displayName = displayName,
                            duration = duration
                        )
                    )
                }
            }

            LoadResult.Page(
                data = mediaItems,
                prevKey = if (position == STARTING_PAGE_INDEX) null else position - 1,
                nextKey = if (mediaItems.isEmpty()) null else position + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, MediaItem>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
