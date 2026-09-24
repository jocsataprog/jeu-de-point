package com.example.moleculegame.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moleculegame.GameViewModel
import com.example.moleculegame.Player

@Composable
fun BoardScreen(gameViewModel: GameViewModel = viewModel()) {
    val gameState by gameViewModel.gameState.collectAsState()
    
    // Paramètres configurables par le joueur
    var selectedLevel by remember { mutableFloatStateOf(gameState.level.toFloat()) }
    var selectedBoardSize by remember { mutableFloatStateOf(gameState.boardSize.toFloat()) }
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Configuration de la Partie") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Difficulté IA: ${selectedLevel.toInt()}", fontWeight = FontWeight.Bold)
                        Text("Grille: ${selectedBoardSize.toInt()}x${selectedBoardSize.toInt()}", fontWeight = FontWeight.Bold)
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("IA", modifier = Modifier.weight(0.2f))
                        Slider(
                            value = selectedLevel,
                            onValueChange = { selectedLevel = it },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Taille", modifier = Modifier.weight(0.2f))
                        Slider(
                            value = selectedBoardSize,
                            onValueChange = { selectedBoardSize = it },
                            valueRange = 50f..500f,
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = gameViewModel.getLearningStats(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    gameViewModel.resetGame(selectedLevel.toInt(), selectedBoardSize.toInt())
                    showSettings = false
                }) {
                    Text("Nouvelle Partie")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Button(onClick = { showSettings = true }) {
                Text("⚙️ Configurer")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White) // Le jeu prend tout l'espace
        ) {
            BoardComposable(
                gameState = gameState,
                cellSizeDp = 60.dp, // Taille de base, modifiable via le pinch-to-zoom
                onCellClicked = { position ->
                    gameViewModel.onCellClicked(position)
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Surcouche pour les scores (En haut de l'écran)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.Center
            ) {
                ScoreCard("Vous (Bleu)", gameState.scores[Player.PLAYER1] ?: 0, Color.Blue, gameState.currentPlayer == Player.PLAYER1)
                Spacer(modifier = Modifier.width(16.dp))
                ScoreCard("IA (Rouge)", gameState.scores[Player.PLAYER2] ?: 0, Color.Red, gameState.currentPlayer == Player.PLAYER2)
            }
        }
    }
}

@Composable
fun ScoreCard(name: String, score: Int, color: Color, isCurrentTurn: Boolean) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTurn) color.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = name, fontWeight = FontWeight.Bold, color = if (isCurrentTurn) Color.White else color)
            Text(text = "$score", style = MaterialTheme.typography.titleLarge, color = if (isCurrentTurn) Color.White else Color.Black)
        }
    }
}
