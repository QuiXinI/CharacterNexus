package ru.quasaris.characters.master

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

                NavHost(navController = navController, startDestination = "menu") {
                    composable("menu") {
                        MenuWindow(
                            characters = characters,
                            onNavigateToCreate = { navController.navigate("empty_window") },
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

                    composable("empty_window") {
                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background,
                            topBar = {
                                CenterAlignedTopAppBar(
                                    title = { Text("Новый персонаж") },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        ) { padding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .clickable { navController.navigate("create") },
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier
                                        .size(300.dp, 400.dp)
                                        .shadow(8.dp, RoundedCornerShape(24.dp)),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(120.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        )
                                        Spacer(Modifier.height(24.dp))
                                        Text(
                                            "Нажмите, чтобы создать",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    composable("create") {
                        CreateWindow(
                            character = null,
                            onNavigateBack = {
                                navController.popBackStack("menu", inclusive = false)
                            },
                            onCharacterChange = { newCharacter ->
                                val index = characters.indexOfFirst { it.id == newCharacter.id }
                                if (index == -1) {
                                    characters.add(newCharacter)
                                } else {
                                    characters[index] = newCharacter
                                }
                                characterRepository.saveCharacters(characters)
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
