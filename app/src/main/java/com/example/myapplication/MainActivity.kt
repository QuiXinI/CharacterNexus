package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val characterRepository = CharacterRepository(applicationContext)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = true) {
                val characters: SnapshotStateList<Character> = remember {
                    mutableStateListOf<Character>().apply {
                        addAll(characterRepository.loadCharacters())
                    }
                }

                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "menu") {

                    composable("menu") {
                        MenuWindow(
                            characters = characters,
                            onNavigateToCreate = {
                                navController.navigate("create")
                            },
                            onCharacterClick = { characterId ->
                                navController.navigate("detail/$characterId")
                            },
                            onImportCharacter = { importedCharacter ->
                                characters.add(importedCharacter)
                                characterRepository.saveCharacters(characters)
                            }
                        )
                    }

                    composable("create") {
                        CreateWindow(
                            onNavigateBack = { navController.popBackStack() },
                            onCharacterCreate = { newCharacter ->
                                characters.add(newCharacter)
                                characterRepository.saveCharacters(characters)
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(
                        route = "detail/{characterId}",
                        arguments = listOf(navArgument("characterId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val characterId = backStackEntry.arguments?.getInt("characterId")
                        val character = characters.find { it.id == characterId }

                        CharacterDetailWindow(
                            character = character,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onDeleteCharacter = { characterToDelete ->
                                characters.remove(characterToDelete)
                                characterRepository.saveCharacters(characters)
                                navController.popBackStack()
                            },
                            onSaveChanges = { updatedCharacter ->
                                val index = characters.indexOfFirst { it.id == updatedCharacter.id }
                                if (index != -1) {
                                    characters[index] = updatedCharacter
                                    characterRepository.saveCharacters(characters)
                                }
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}