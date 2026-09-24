package com.example.moleculegame

data class Position(val x: Int, val y: Int)

data class GameState(
    val boardSize: Int,
    val atoms: List<Atom>,
    val electrons: List<Electron>,
    val currentPlayer: Player,
    val scores: Map<Player, Int>,
    val level: Int
)
