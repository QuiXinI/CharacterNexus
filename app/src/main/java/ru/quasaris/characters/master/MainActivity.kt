package ru.quasaris.characters.master

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.quasaris.characters.master.ui.theme.quasarisTheme

class MainActivity : ComponentActivity() {
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

                NavHost(navController = navController, startDestination = "menu") {
                    composable("menu") {
                        MenuWindow(
                            characters = characters,
                            onNavigateToCreate = { navController.navigate("create") },
                            onCharacterClick = { characterId -> navController.navigate("detail/$characterId") },
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
                            onNavigateBack = { navController.popBackStack() },
                            onDeleteCharacter = { charToDelete ->
                                characters.removeIf { it.id == charToDelete.id }
                                characterRepository.saveCharacters(characters)
                                navController.popBackStack()
                            },
                            onSaveChanges = { updatedChar ->
                                val index = characters.indexOfFirst { it.id == updatedChar.id }
                                if (index != -1) {
                                    characters[index] = updatedChar
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
