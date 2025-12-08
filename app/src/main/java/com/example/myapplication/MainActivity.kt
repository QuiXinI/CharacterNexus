package com.example.myapplication

import android.net.Uri
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
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val characterRepository = CharacterRepository(applicationContext)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
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
                            onCharacterCreate = { newCharacter, tempImageUri ->
                                var finalCharacter = newCharacter
                                if (tempImageUri != null) {
                                    val permanentUri = characterRepository.saveImagePermanently(tempImageUri)
                                    finalCharacter = newCharacter.copy(imageUriString = permanentUri?.toString())
                                }
                                characters.add(finalCharacter)
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
                                characterToDelete.imageUriString?.let { uriString ->
                                    Uri.parse(uriString).path?.let { path ->
                                        File(path).delete()
                                    }
                                }
                                characters.remove(characterToDelete)
                                characterRepository.saveCharacters(characters)
                                navController.popBackStack()
                            },
                            onSaveChanges = { updatedCharacter, newImageUri ->
                                var finalCharacter = updatedCharacter
                                if (newImageUri != null) {
                                    finalCharacter.imageUriString?.let { uriString ->
                                        Uri.parse(uriString).path?.let { path -> File(path).delete() }
                                    }
                                    val permanentUri = characterRepository.saveImagePermanently(newImageUri)
                                    finalCharacter = finalCharacter.copy(imageUriString = permanentUri?.toString())
                                }
                                val index = characters.indexOfFirst { it.id == finalCharacter.id }
                                if (index != -1) {
                                    characters[index] = finalCharacter
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