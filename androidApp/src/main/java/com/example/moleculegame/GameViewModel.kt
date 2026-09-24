package com.example.moleculegame

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val learningManager = AILearningManager(application)
    private val engine = GameEngine(initialLevel = 1, initialBoardSize = 50)
    val gameState: StateFlow<GameState> = engine.gameState
    private var aiEngine = AIEngine(difficulty = 5, weights = learningManager.loadWeights())

    fun resetGame(level: Int, boardSize: Int) {
        // Enregistrer le résultat de la partie précédente pour que l'IA "apprenne"
        val currentScores = gameState.value.scores
        val p1Score = currentScores[Player.PLAYER1] ?: 0
        val p2Score = currentScores[Player.PLAYER2] ?: 0
        if (p1Score > 0 || p2Score > 0) {
            val aiWon = p2Score >= p1Score
            learningManager.recordResult(aiWon)
        }

        val updatedWeights = learningManager.loadWeights()
        aiEngine = AIEngine(difficulty = level, weights = updatedWeights)
        engine.resetGame(level, boardSize)
    }

    fun getLearningStats(): String {
        val w = learningManager.loadWeights()
        val winRate = if (w.gamesPlayed > 0) (w.gamesWon * 100) / w.gamesPlayed else 0
        return "🧠 Expérience IA : ${w.gamesPlayed} parties | Victoires : $winRate%"
    }

    fun onCellClicked(position: Position) {
        val currentState = gameState.value
        if (currentState.currentPlayer == Player.PLAYER1) {
            engine.placeElectron(position)
            
            if (engine.gameState.value.currentPlayer == Player.PLAYER2) {
                playAITurn()
            }
        }
    }

    private fun playAITurn() {
        viewModelScope.launch {
            delay(100) 
            val aiMove = aiEngine.calculateBestMove(engine.gameState.value)
            if (aiMove != null) {
                engine.placeElectron(aiMove)
            }
        }
    }
}
