package com.notes.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.notes.app.ui.screens.NoteDetailScreen
import com.notes.app.ui.screens.NoteListScreen
import com.notes.app.ui.screens.SyncScreen

@Composable
fun NotesNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.NoteList.route
    ) {
        composable(Screen.NoteList.route) {
            NoteListScreen(
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                },
                onSettingsClick = {
                    // TODO: Settings screen
                },
                onSyncClick = {
                    navController.navigate(Screen.Sync.route)
                }
            )
        }

        composable(Screen.NoteDetail.route) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
                ?: return@composable

            NoteDetailScreen(
                noteId = noteId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNoteDeleted = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Sync.route) {
            SyncScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    data object NoteList : Screen("notes")
    data object NoteDetail : Screen("note/{noteId}") {
        fun createRoute(noteId: String) = "note/$noteId"
    }
    data object Sync : Screen("sync")
    data object Settings : Screen("settings")
}
