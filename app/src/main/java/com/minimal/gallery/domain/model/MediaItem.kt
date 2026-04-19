package com.minimal.gallery.domain.model

import android.net.Uri

/**
 * Represents a media item (image or video) in the gallery
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val type: MediaType,
    val dateModified: Long,
    val folderName: String,
    val folderPath: String,
    val displayName: String,
    val duration: Long? = null // for videos, in milliseconds
)

/**
 * Type of media item
 */
enum class MediaType {
    IMAGE,
    VIDEO
}

/**
 * Represents a folder that can be excluded from gallery
 */
data class FolderInfo(
    val path: String,
    val name: String,
    val isExcluded: Boolean,
    val mediaCount: Int = 0
)
