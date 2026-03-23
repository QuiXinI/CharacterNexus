package ru.quasaris.characters.master

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.quasaris.characters.master.ui.theme.quasarisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ВРЕМЕННО ОТКЛЮЧЕНО ДЛЯ ТЕСТИРОВАНИЯ ДИЗАЙНА
        // val characterRepository = CharacterRepository(applicationContext)

        enableEdgeToEdge()
        setContent {
            quasarisTheme(dynamicColor = true) {
                // Прямой запуск окна создания для проверки дизайна
                CreateWindow(
                    onNavigateBack = { /* Временно ничего не делает */ },
                    onCharacterCreate = { _ -> /* Временно ничего не делает */ }
                )

                /* 
                // ОРИГИНАЛЬНАЯ ЛОГИКА НАВИГАЦИИ
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
                        CharacterDetailWindow(...)
                    }
                }
                */
            }
        }
    }
}
