package com.example.moleculegame

enum class Player {
    PLAYER1, PLAYER2
}

data class Electron(
    val id: String,
    val owner: Player,
    val position: Position,
    val isCaptured: Boolean = false,
    val capturerPolygon: List<Position>? = null
)
