package ru.quasaris.characters.master

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.quasaris.characters.master.MainWindow.CreateWindow
import ru.quasaris.characters.master.ui.theme.quasarisTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val characterRepository = CharacterRepository(applicationContext)

        enableEdgeToEdge()
        setContent {
            quasarisTheme(dynamicColor = true) {
                val characters: SnapshotStateList<Character> = remember {
                    mutableStateListOf<Character>().apply {
                        addAll(characterRepository.loadCharacters())
                    }
                }

                val navController = rememberNavController()

                val animDuration = 550
                val navHostOffsetSpec = tween<IntOffset>(durationMillis = animDuration, easing = FastOutSlowInEasing)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "menu",
                        enterTransition = {
                            slideInHorizontally(initialOffsetX = { it }, animationSpec = navHostOffsetSpec)
                        },
                        exitTransition = {
                            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = navHostOffsetSpec)
                        },
                        popEnterTransition = {
                            slideInHorizontally(initialOffsetX = { -it }, animationSpec = navHostOffsetSpec)
                        },
                        popExitTransition = {
                            slideOutHorizontally(targetOffsetX = { it }, animationSpec = navHostOffsetSpec)
                        }
                    ) {
                        composable(
                            "menu"
                        ) {
                            MenuWindow(
                                characters = characters,
                                onNavigateToCreate = { navController.navigate("create_setup") },
                                onCharacterClick = { characterId ->
                                    navController.navigate("edit/$characterId")
                                },
                                onImportCharacter = { importedCharacter ->
                                    characters.add(importedCharacter)
                                    characterRepository.saveCharacters(characters)
                                },
                                onDeleteCharacters = { idsToDelete ->
                                    characters.removeAll { it.id in idsToDelete }
                                    characterRepository.saveCharacters(characters)
                                }
                            )
                        }

                        composable(
                            "create_setup"
                        ) {
                            CharacterCreationWindow(
                                onNavigateBack = { navController.popBackStack() },
                                onCharacterCreate = { newChar ->
                                    characters.add(newChar)
                                    characterRepository.saveCharacters(characters)
                                    navController.navigate("edit/${newChar.id}") {
                                        popUpTo("menu")
                                    }
                                }
                            )
                        }

                        composable(
                            route = "edit/{characterId}",
                            arguments = listOf(navArgument("characterId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val characterId = backStackEntry.arguments?.getInt("characterId")
                            val character = characters.find { it.id == characterId }

                            CreateWindow(
                                character = character,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onCharacterChange = { updatedCharacter ->
                                    val index = characters.indexOfFirst { it.id == updatedCharacter.id }
                                    if (index != -1) {
                                        characters[index] = updatedCharacter
                                        characterRepository.saveCharacters(characters)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
