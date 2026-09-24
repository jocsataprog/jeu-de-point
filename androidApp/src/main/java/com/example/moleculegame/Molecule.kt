package com.example.moleculegame

enum class BondType(val electronsNeeded: Int) {
    SINGLE(1),
    DOUBLE(2),
    TRIPLE(3)
}

data class Bond(
    val atoms: Pair<Atom, Atom>,
    val type: BondType,
    val electrons: List<Electron>
)

data class Molecule(
    val atoms: List<Atom>,
    val isStable: Boolean,
    val formula: String
)
