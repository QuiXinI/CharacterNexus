package ru.quasaris.characters.master

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

                val springSpec = spring<IntOffset>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )

                NavHost(navController = navController, startDestination = "menu") {
                    composable(
                        "menu",
                        exitTransition = {
                            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = springSpec)
                        },
                        popEnterTransition = {
                            if (initialState.destination.route == "create_setup") {
                                slideInVertically(initialOffsetY = { -it }, animationSpec = springSpec)
                            } else {
                                slideInHorizontally(initialOffsetX = { -it }, animationSpec = springSpec)
                            }
                        }
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
                        "create_setup",
                        enterTransition = {
                            slideInVertically(initialOffsetY = { it }, animationSpec = springSpec)
                        },
                        exitTransition = {
                            slideOutVertically(targetOffsetY = { it }, animationSpec = springSpec)
                        },
                        popExitTransition = {
                            slideOutVertically(targetOffsetY = { it }, animationSpec = springSpec)
                        }
                    ) {
                        CharacterCreationWindow(
                            onNavigateBack = { navController.popBackStack() },
                            onCharacterCreate = { newChar ->
                                characters.add(newChar)
                                characterRepository.saveCharacters(characters)
                                navController.navigate("character_sheet/${newChar.id}") {
                                    popUpTo("menu")
                                }
                            }
                        )
                    }

                    composable(
                        route = "character_sheet/{characterId}",
                        arguments = listOf(navArgument("characterId") { type = NavType.IntType }),
                        enterTransition = {
                            slideInVertically(initialOffsetY = { it }, animationSpec = springSpec)
                        },
                        exitTransition = {
                            slideOutHorizontally(targetOffsetX = { it }, animationSpec = springSpec)
                        },
                        popExitTransition = {
                            slideOutHorizontally(targetOffsetX = { it }, animationSpec = springSpec)
                        }
                    ) { backStackEntry ->
                        val characterId = backStackEntry.arguments?.getInt("characterId")
                        val character = characters.find { it.id == characterId }
                        
                        CreateWindow(
                            character = character,
                            onNavigateBack = { navController.popBackStack("menu", false) },
                            onCharacterChange = { updated ->
                                val index = characters.indexOfFirst { it.id == updated.id }
                                if (index != -1) {
                                    characters[index] = updated
                                    characterRepository.saveCharacters(characters)
                                }
                            }
                        )
                    }

                    composable(
                        route = "edit/{characterId}",
                        arguments = listOf(navArgument("characterId") { type = NavType.IntType }),
                        enterTransition = {
                            slideInHorizontally(initialOffsetX = { it }, animationSpec = springSpec)
                        },
                        exitTransition = {
                            slideOutHorizontally(targetOffsetX = { it }, animationSpec = springSpec)
                        },
                        popExitTransition = {
                            slideOutHorizontally(targetOffsetX = { it }, animationSpec = springSpec)
                        }
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
