package com.example.moleculegame

data class Atom(
    val symbol: String,
    val position: Position,
    val maxBonds: Int,
    val electrons: Int = 0,
    val bonds: MutableList<Bond> = mutableListOf()
)
