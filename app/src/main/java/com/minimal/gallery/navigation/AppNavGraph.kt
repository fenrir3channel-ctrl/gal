package com.minimal.gallery.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.minimal.gallery.data.SettingsRepository
import com.minimal.gallery.data.repository.MediaRepository
import com.minimal.gallery.domain.model.FolderInfo
import com.minimal.gallery.domain.model.MediaItem
import com.minimal.gallery.ui.screens.home.HomeScreen
import com.minimal.gallery.ui.screens.settings.SettingsScreen
import com.minimal.gallery.ui.screens.viewer.ViewerScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Viewer : Screen("viewer/{mediaItemsJson}/{initialIndex}")
    object Settings : Screen("settings")
}

data class MediaItemList(val items: List<MediaItem>)

@kotlinx.serialization.Serializable
data class SerializableMediaItem(
    val id: Long,
    val uriString: String,
    val type: String,
    val dateModified: Long,
    val folderName: String,
    val folderPath: String,
    val displayName: String,
    val duration: Long?
)

object MediaItemListSerializer : kotlinx.serialization.KSerializer<MediaItemList> {
    override val descriptor = kotlinx.serialization.builtins.ListSerializer(
        kotlinx.serialization.builtins.serializer<SerializableMediaItem>()
    ).descriptor

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: MediaItemList) {
        val serializableList = value.items.map { item ->
            SerializableMediaItem(
                id = item.id,
                uriString = item.uri.toString(),
                type = item.type.name,
                dateModified = item.dateModified,
                folderName = item.folderName,
                folderPath = item.folderPath,
                displayName = item.displayName,
                duration = item.duration
            )
        }
        encoder.encodeSerializableValue(
            kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer<SerializableMediaItem>()), 
            serializableList
        )
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): MediaItemList {
        val serializableList = decoder.decodeSerializableValue(
            kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer<SerializableMediaItem>())
        )
        val items = serializableList.map { item ->
            MediaItem(
                id = item.id,
                uri = android.net.Uri.parse(item.uriString),
                type = com.minimal.gallery.domain.model.MediaType.valueOf(item.type),
                dateModified = item.dateModified,
                folderName = item.folderName,
                folderPath = item.folderPath,
                displayName = item.displayName,
                duration = item.duration
            )
        }
        return MediaItemList(items)
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    mediaRepository: MediaRepository,
    settingsRepository: SettingsRepository,
    onThemeChange: (com.minimal.gallery.ui.theme.AppTheme) -> Unit
) {
    var folders by remember { mutableStateOf<List<FolderInfo>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    // Load folders once
    LaunchedEffect(Unit) {
        folders = mediaRepository.getAllFolders()
    }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                mediaRepository = mediaRepository,
                settingsRepository = settingsRepository,
                onThemeChange = onThemeChange,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onMediaClick = { mediaItem ->
                    // For MVP, pass single item - in production load surrounding context
                    val itemList = MediaItemList(listOf(mediaItem))
                    val json = Json.encodeToString(MediaItemListSerializer(), itemList)
                    navController.navigate("viewer/${json}/0")
                }
            )
        }
        
        composable(
            route = "viewer/{mediaItemsJson}/{initialIndex}",
            arguments = listOf(
                navArgument("mediaItemsJson") { type = NavType.StringType },
                navArgument("initialIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val json = backStackEntry.arguments?.getString("mediaItemsJson") ?: ""
            val initialIndex = backStackEntry.arguments?.getInt("initialIndex") ?: 0
            
            try {
                val mediaItemList = Json.decodeFromString(MediaItemListSerializer(), json)
                ViewerScreen(
                    mediaItems = mediaItemList.items,
                    initialIndex = initialIndex,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            } catch (e: Exception) {
                ViewerScreen(
                    mediaItems = emptyList(),
                    initialIndex = 0,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                settingsRepository = settingsRepository,
                folders = folders,
                onFolderToggle = { path, isExcluded ->
                    scope.launch {
                        settingsRepository.toggleFolderExclusion(path, isExcluded)
                        // Refresh folders list with updated exclusion status
                        folders = mediaRepository.getAllFolders().map { folder ->
                            folder.copy(isExcluded = settingsRepository.excludedFoldersFlow.first().contains(folder.path))
                        }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
