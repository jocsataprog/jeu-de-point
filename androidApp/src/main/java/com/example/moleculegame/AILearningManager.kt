package com.example.moleculegame

import android.content.Context
import java.io.File

data class AIWeights(
    var captureWeight: Double = 15000.0,
    var defenseWeight: Double = 1000.0, // Réduit pour éviter que l'IA ne soit trop passive/frileuse
    var centerWeight: Double = 3.0,
    var mobilityWeight: Double = 1.5,
    var gamesPlayed: Int = 0,
    var gamesWon: Int = 0
)

class AILearningManager(context: Context) {
    private val file = File(context.filesDir, "ai_learning_weights_v2.txt") // Fichier v2 pour repartir sur des bases agressives

    fun loadWeights(): AIWeights {
        try {
            if (file.exists()) {
                val parts = file.readText().trim().split(",")
                if (parts.size >= 6) {
                    return AIWeights(
                        captureWeight = parts[0].toDouble(),
                        defenseWeight = parts[1].toDouble(),
                        centerWeight = parts[2].toDouble(),
                        mobilityWeight = parts[3].toDouble(),
                        gamesPlayed = parts[4].toInt(),
                        gamesWon = parts[5].toInt()
                    )
                }
            }
        } catch (_: Exception) {}
        return AIWeights()
    }

    fun saveWeights(w: AIWeights) {
        try {
            file.writeText("${w.captureWeight},${w.defenseWeight},${w.centerWeight},${w.mobilityWeight},${w.gamesPlayed},${w.gamesWon}")
        } catch (_: Exception) {}
    }

    fun recordResult(aiWon: Boolean) {
        val w = loadWeights()
        w.gamesPlayed++
        if (aiWon) {
            w.gamesWon++
            w.captureWeight *= 1.02
        } else {
            // En cas de défaite, l'IA devient plus OFFENSIVE et agressive (cherche à attaquer au lieu de fuir)
            w.captureWeight *= 1.08
            w.defenseWeight *= 0.90 // Baisse la défense pour foncer dans le tas
            w.centerWeight *= 1.05
        }
        saveWeights(w)
    }
}
