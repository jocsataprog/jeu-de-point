package com.example.moleculegame

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import kotlin.math.abs

internal fun computeCaptures(
    boardSize: Int,
    electrons: List<Electron>,
    lastMove: Position,
    player: Player
): Pair<Int, List<Electron>> {
    val simulatedElectrons = electrons.toMutableList()
    val opponent = if (player == Player.PLAYER1) Player.PLAYER2 else Player.PLAYER1
    val directions = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
    
    val minX = electrons.minOfOrNull { it.position.x } ?: 0
    val maxX = electrons.maxOfOrNull { it.position.x } ?: boardSize
    val minY = electrons.minOfOrNull { it.position.y } ?: 0
    val maxY = electrons.maxOfOrNull { it.position.y } ?: boardSize

    var totalCaptures = 0
    val totallyEnclosedVisited = mutableSetOf<Position>()
    val capturedGroups = mutableListOf<Pair<List<Electron>, List<Position>>>()

    for ((dx, dy) in directions) {
        val startPos = Position(lastMove.x + dx, lastMove.y + dy)
        if (startPos.x < 0 || startPos.x > boardSize || startPos.y < 0 || startPos.y > boardSize) continue
        if (totallyEnclosedVisited.contains(startPos)) continue
        
        val startElectron = simulatedElectrons.find { it.position == startPos && !it.isCaptured }
        if (startElectron != null && startElectron.owner == player) continue 

        val queue = ArrayDeque<Position>()
        val visited = mutableSetOf<Position>()
        val boundary = mutableSetOf<Position>()
        val enclosedOpponents = mutableListOf<Electron>()
        var escapes = false

        queue.add(startPos)
        visited.add(startPos)

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            val e = simulatedElectrons.find { it.position == curr && !it.isCaptured }
            if (e != null && e.owner == opponent) {
                enclosedOpponents.add(e)
            }

            for ((ndx, ndy) in directions) {
                val nPos = Position(curr.x + ndx, curr.y + ndy)
                
                if (nPos.x < 0 || nPos.x > boardSize || nPos.y < 0 || nPos.y > boardSize) {
                    escapes = true
                } else if (nPos.x < minX || nPos.x > maxX || nPos.y < minY || nPos.y > maxY) {
                    escapes = true
                } else if (!visited.contains(nPos)) {
                    val nElectron = simulatedElectrons.find { it.position == nPos && !it.isCaptured }
                    if (nElectron != null && nElectron.owner == player) {
                        boundary.add(nPos)
                    } else {
                        visited.add(nPos)
                        queue.add(nPos)
                    }
                }
            }
            if (escapes) break
        }

        // RÈGLE : Si la zone encerclée contient au moins un électron de la MÊME couleur (joueur captureur), la capture est impossible !
        val hasSameColorInside = simulatedElectrons.any { e ->
            !e.isCaptured && e.owner == player && e.position in visited && e.position !in boundary
        }

        if (!escapes && !hasSameColorInside) {
            totallyEnclosedVisited.addAll(visited)
            if (enclosedOpponents.isNotEmpty()) {
                boundary.add(lastMove) 
                capturedGroups.add(Pair(enclosedOpponents, boundary.toList()))
                totalCaptures += enclosedOpponents.size
            }
        }
    }

    for ((group, boundary) in capturedGroups) {
        for (e in group) {
            val idx = simulatedElectrons.indexOf(e)
            simulatedElectrons[idx] = e.copy(isCaptured = true, capturerPolygon = boundary)
        }
    }

    return Pair(totalCaptures, simulatedElectrons)
}

internal fun checkPlacedInOpponentEnclosure(
    boardSize: Int,
    electrons: List<Electron>,
    placedElectron: Electron
): Pair<Boolean, List<Position>> {
    val player = placedElectron.owner
    val opponent = if (player == Player.PLAYER1) Player.PLAYER2 else Player.PLAYER1
    val startPos = placedElectron.position

    val minX = electrons.minOfOrNull { it.position.x } ?: 0
    val maxX = electrons.maxOfOrNull { it.position.x } ?: boardSize
    val minY = electrons.minOfOrNull { it.position.y } ?: 0
    val maxY = electrons.maxOfOrNull { it.position.y } ?: boardSize

    val directions = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
    val queue = ArrayDeque<Position>()
    val visited = mutableSetOf<Position>()
    val boundary = mutableSetOf<Position>()
    var escapes = false

    queue.add(startPos)
    visited.add(startPos)

    while (queue.isNotEmpty()) {
        val curr = queue.removeFirst()

        for ((ndx, ndy) in directions) {
            val nPos = Position(curr.x + ndx, curr.y + ndy)

            if (nPos.x < 0 || nPos.x > boardSize || nPos.y < 0 || nPos.y > boardSize) {
                escapes = true
            } else if (nPos.x < minX || nPos.x > maxX || nPos.y < minY || nPos.y > maxY) {
                escapes = true
            } else if (!visited.contains(nPos)) {
                val nElectron = electrons.find { it.position == nPos && !it.isCaptured }
                if (nElectron != null && nElectron.owner == opponent) {
                    boundary.add(nPos)
                } else {
                    visited.add(nPos)
                    queue.add(nPos)
                }
            }
        }
        if (escapes) break
    }

    if (!escapes) {
        val hasOpponentInside = electrons.any { e ->
            !e.isCaptured && e.owner == opponent && e.position in visited && e.position !in boundary
        }
        if (!hasOpponentInside) {
            return Pair(true, boundary.toList())
        }
    }

    return Pair(false, emptyList())
}

internal fun hasLiberties(boardSize: Int, electrons: List<Electron>, startPos: Position, player: Player): Boolean {
    val minX = electrons.minOfOrNull { it.position.x } ?: 0
    val maxX = electrons.maxOfOrNull { it.position.x } ?: boardSize
    val minY = electrons.minOfOrNull { it.position.y } ?: 0
    val maxY = electrons.maxOfOrNull { it.position.y } ?: boardSize

    val queue = ArrayDeque<Position>()
    val visited = mutableSetOf<Position>()
    val directions = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
    
    queue.add(startPos)
    visited.add(startPos)
    
    while(queue.isNotEmpty()) {
        val curr = queue.removeFirst()
        for ((dx, dy) in directions) {
            val nPos = Position(curr.x + dx, curr.y + dy)
            if (nPos.x < 0 || nPos.x > boardSize || nPos.y < 0 || nPos.y > boardSize) return true
            if (nPos.x < minX || nPos.x > maxX || nPos.y < minY || nPos.y > maxY) return true
            
            if (!visited.contains(nPos)) {
                val nElectron = electrons.find { it.position == nPos && !it.isCaptured }
                if (nElectron == null) {
                    return true // On a trouvé une liberté !
                } else if (nElectron.owner == player) {
                    visited.add(nPos)
                    queue.add(nPos)
                }
            }
        }
    }
    return false
}

internal fun reviveElectrons(boardSize: Int, electrons: List<Electron>, scores: MutableMap<Player, Int>): List<Electron> {
    // RÈGLE : Une fois un territoire encerclé, il le reste jusqu'à la fin du jeu.
    // Quand l'adversaire entoure un territoire déjà encerclé, les traits d'encerclement intérieurs sont conservés de façon permanente.
    return electrons
}

class GameEngine(private val initialLevel: Int = 1, private val initialBoardSize: Int = 50) {
    private val _gameState = MutableStateFlow(generateLevel(initialLevel, initialBoardSize))
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    fun resetGame(level: Int, boardSize: Int) {
        _gameState.value = generateLevel(level, boardSize)
    }

    fun placeElectron(position: Position) {
        _gameState.update { currentState ->
            if (currentState.electrons.any { it.position == position }) {
                return@update currentState
            }
            if (position.x < 0 || position.x > currentState.boardSize || position.y < 0 || position.y > currentState.boardSize) {
                return@update currentState
            }

            val currentPlayer = currentState.currentPlayer
            val newElectron = Electron(
                id = UUID.randomUUID().toString(),
                owner = currentPlayer,
                position = position
            )

            var score1 = currentState.scores[Player.PLAYER1] ?: 0
            var score2 = currentState.scores[Player.PLAYER2] ?: 0

            // 1. Calcul des captures
            var (captures, newElectrons) = computeCaptures(
                boardSize = currentState.boardSize,
                electrons = currentState.electrons + newElectron,
                lastMove = position,
                player = currentPlayer
            )

            val opponent = if (currentPlayer == Player.PLAYER1) Player.PLAYER2 else Player.PLAYER1

            if (captures > 0) {
                if (currentPlayer == Player.PLAYER1) score1 += captures else score2 += captures
            } else {
                // RÈGLE : Si le joueur pose un électron dans un champ DÉJÀ encerclé par l'adversaire,
                // l'électron est immédiatement capturé et le trait d'encerclement s'affiche tout de suite !
                val placedElectronInList = newElectrons.find { it.id == newElectron.id } ?: newElectron
                if (!placedElectronInList.isCaptured) {
                    val (isInOpponentTerritory, oppBoundary) = checkPlacedInOpponentEnclosure(
                        boardSize = currentState.boardSize,
                        electrons = newElectrons,
                        placedElectron = placedElectronInList
                    )
                    if (isInOpponentTerritory) {
                        newElectrons = newElectrons.map { el ->
                            if (el.id == newElectron.id) {
                                el.copy(isCaptured = true, capturerPolygon = oppBoundary)
                            } else {
                                el
                            }
                        }
                        if (opponent == Player.PLAYER1) score1 += 1 else score2 += 1
                    }
                }
            }
            
            // 2. Libération des électrons si leurs ravisseurs ont été capturés
            val scoresMap = mutableMapOf(Player.PLAYER1 to score1, Player.PLAYER2 to score2)
            val finalElectrons = reviveElectrons(currentState.boardSize, newElectrons, scoresMap)

            val nextPlayer = if (currentPlayer == Player.PLAYER1) Player.PLAYER2 else Player.PLAYER1

            currentState.copy(
                electrons = finalElectrons,
                currentPlayer = nextPlayer,
                scores = scoresMap
            )
        }
    }

    private fun generateLevel(level: Int, boardSize: Int): GameState {
        return GameState(
            boardSize = boardSize,
            atoms = emptyList(),
            electrons = emptyList(),
            currentPlayer = Player.PLAYER1,
            scores = mapOf(Player.PLAYER1 to 0, Player.PLAYER2 to 0),
            level = level
        )
    }
}

class AIEngine(private val difficulty: Int, private val weights: AIWeights = AIWeights()) {
    private val transpositionTable = mutableMapOf<String, Double>()
    private class TimeoutException : Exception()

    fun calculateBestMove(state: GameState): Position? {
        val player = state.currentPlayer
        val opponent = if (player == Player.PLAYER1) Player.PLAYER2 else Player.PLAYER1

        val occupied = state.electrons.map { it.position }.toSet()
        if (occupied.isEmpty()) {
            return Position(state.boardSize / 2, state.boardSize / 2)
        }

        val adjacentPositions = mutableSetOf<Position>()
        val searchRadius = 1 // Toujours à 1 pour construire des murs solides et continus sans sauter de cases
        
        occupied.forEach { p ->
            for (dx in -searchRadius..searchRadius) {
                for (dy in -searchRadius..searchRadius) {
                    if (dx == 0 && dy == 0) continue
                    val adj = Position(p.x + dx, p.y + dy)
                    if (adj.x in 0..state.boardSize && adj.y in 0..state.boardSize && !occupied.contains(adj)) {
                        adjacentPositions.add(adj)
                    }
                }
            }
        }

        val candidates = adjacentPositions.toList()
        if (candidates.isEmpty()) return null

        when (difficulty) {
            1 -> return candidates.random()
            in 2..6 -> return fallbackHeuristicMove(state, candidates, player, opponent)
            else -> {
                // Niveaux 7 à 10 : Recherche Minimax avec profondeur et temps de réflexion progressifs
                val (timeLimitMs, maxDepthAllowed) = when (difficulty) {
                    7 -> Pair(800L, 2)
                    8 -> Pair(1200L, 4)
                    9 -> Pair(1800L, 5)
                    else -> Pair(2500L, 8)
                }

                val startTime = System.currentTimeMillis()
                transpositionTable.clear()

                var bestMove: Position? = null
                var targetDepth = 1

                while (targetDepth <= maxDepthAllowed && System.currentTimeMillis() - startTime < timeLimitMs) {
                    try {
                        val move = searchBestMoveAtDepth(
                            state = state,
                            candidates = candidates,
                            depth = targetDepth,
                            aiPlayer = player,
                            startTime = startTime,
                            timeLimitMs = timeLimitMs
                        )
                        if (move != null) {
                            bestMove = move
                        }
                        targetDepth++
                    } catch (e: TimeoutException) {
                        break
                    }
                }

                return bestMove ?: fallbackHeuristicMove(state, candidates, player, opponent)
            }
        }
    }

    private fun searchBestMoveAtDepth(
        state: GameState,
        candidates: List<Position>,
        depth: Int,
        aiPlayer: Player,
        startTime: Long,
        timeLimitMs: Long
    ): Position? {
        val prioritizedCandidates = prioritizeMoves(state, candidates, aiPlayer)
        var bestScore = -Double.MAX_VALUE
        var bestMove: Position? = null
        var alpha = -Double.MAX_VALUE
        val beta = Double.MAX_VALUE

        for (move in prioritizedCandidates) {
            if (System.currentTimeMillis() - startTime > timeLimitMs) {
                throw TimeoutException()
            }

            val nextState = applySimulatedMove(state, move, aiPlayer) ?: continue
            val score = minimaxWithAlphaBeta(
                state = nextState,
                depth = depth - 1,
                isMaximizing = false,
                aiPlayer = aiPlayer,
                alpha = alpha,
                beta = beta,
                startTime = startTime,
                timeLimitMs = timeLimitMs
            )

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            alpha = maxOf(alpha, score)
        }

        return bestMove
    }

    private fun minimaxWithAlphaBeta(
        state: GameState,
        depth: Int,
        isMaximizing: Boolean,
        aiPlayer: Player,
        alpha: Double,
        beta: Double,
        startTime: Long,
        timeLimitMs: Long
    ): Double {
        if (System.currentTimeMillis() - startTime > timeLimitMs) {
            throw TimeoutException()
        }

        if (depth == 0) {
            return evaluateBoard(state, aiPlayer)
        }

        val stateKey = getStateKey(state, isMaximizing)
        if (transpositionTable.containsKey(stateKey)) {
            return transpositionTable[stateKey]!!
        }

        val currentMover = if (isMaximizing) aiPlayer else (if (aiPlayer == Player.PLAYER1) Player.PLAYER2 else Player.PLAYER1)
        val occupied = state.electrons.map { it.position }.toSet()
        val adjacentPositions = mutableSetOf<Position>()
        occupied.forEach { p ->
            listOf(Pair(1,0), Pair(-1,0), Pair(0,1), Pair(0,-1)).forEach { (dx, dy) ->
                val adj = Position(p.x + dx, p.y + dy)
                if (adj.x in 0..state.boardSize && adj.y in 0..state.boardSize && !occupied.contains(adj)) {
                    adjacentPositions.add(adj)
                }
            }
        }

        val candidates = adjacentPositions.toList()
        if (candidates.isEmpty()) {
            return evaluateBoard(state, aiPlayer)
        }

        // Largeur de recherche augmentée au niveau 9+ pour anticiper les pièges profonds
        val branchLimit = if (difficulty >= 9) 12 else 6
        val prioritizedCandidates = prioritizeMoves(state, candidates, currentMover).take(branchLimit)

        var varAlpha = alpha
        var varBeta = beta

        if (isMaximizing) {
            var maxEval = -Double.MAX_VALUE
            for (move in prioritizedCandidates) {
                val nextState = applySimulatedMove(state, move, currentMover) ?: continue
                val eval = minimaxWithAlphaBeta(nextState, depth - 1, false, aiPlayer, varAlpha, varBeta, startTime, timeLimitMs)
                maxEval = maxOf(maxEval, eval)
                varAlpha = maxOf(varAlpha, eval)
                if (varBeta <= varAlpha) break // Élagage Alpha-Beta
            }
            transpositionTable[stateKey] = maxEval
            return maxEval
        } else {
            var minEval = Double.MAX_VALUE
            for (move in prioritizedCandidates) {
                val nextState = applySimulatedMove(state, move, currentMover) ?: continue
                val eval = minimaxWithAlphaBeta(nextState, depth - 1, true, aiPlayer, varAlpha, varBeta, startTime, timeLimitMs)
                minEval = minOf(minEval, eval)
                varBeta = minOf(varBeta, eval)
                if (varBeta <= varAlpha) break // Élagage Alpha-Beta
            }
            transpositionTable[stateKey] = minEval
            return minEval
        }
    }

    private fun prioritizeMoves(state: GameState, moves: List<Position>, mover: Player): List<Position> {
        val opponent = if (mover == Player.PLAYER1) Player.PLAYER2 else Player.PLAYER1
        val isAggressiveAI = difficulty >= 9
        
        return moves.sortedByDescending { move ->
            val nextState = applySimulatedMove(state, move, mover)
            if (nextState == null) {
                -1000000.0 // Suicide
            } else {
                val myGain = (nextState.scores[mover] ?: 0) - (state.scores[mover] ?: 0)
                val oppNextState = applySimulatedMove(state, move, opponent)
                val oppGain = if (oppNextState != null) (oppNextState.scores[opponent] ?: 0) - (state.scores[opponent] ?: 0) else 0
                
                // Pression agressive au contact direct de l'ennemi pour l'encercler
                val enemyPressure = listOf(Pair(1,0), Pair(-1,0), Pair(0,1), Pair(0,-1)).count { (dx, dy) ->
                    val adj = Position(move.x + dx, move.y + dy)
                    state.electrons.any { it.position == adj && it.owner == opponent && !it.isCaptured }
                }

                val friendlySim = nextState.electrons.filter { it.owner == mover && !it.isCaptured }.map { it.position }.toSet()
                val hasN = friendlySim.contains(Position(move.x, move.y - 1))
                val hasS = friendlySim.contains(Position(move.x, move.y + 1))
                val hasE = friendlySim.contains(Position(move.x + 1, move.y))
                val hasW = friendlySim.contains(Position(move.x - 1, move.y))
                var cornerBonus = 0.0
                if ((hasN || hasS) && (hasE || hasW)) {
                    cornerBonus = 500.0
                }
                
                val attackMult = if (isAggressiveAI) 2500.0 else 1000.0
                val defenseMult = if (isAggressiveAI) 600.0 else 1500.0 // Moins frileux, plus offensif !

                myGain * attackMult + oppGain * defenseMult + enemyPressure * 300.0 + cornerBonus
            }
        }
    }

    private fun applySimulatedMove(state: GameState, move: Position, mover: Player): GameState? {
        val sim = state.electrons.toMutableList()
        sim.add(Electron("sim", mover, move))
        val (captures, postCap) = computeCaptures(state.boardSize, sim, move, mover)
        
        var s1 = state.scores[Player.PLAYER1] ?: 0
        var s2 = state.scores[Player.PLAYER2] ?: 0
        if (captures > 0) {
            if (mover == Player.PLAYER1) s1 += captures else s2 += captures
        }

        val dummyScores = mutableMapOf(
            Player.PLAYER1 to s1,
            Player.PLAYER2 to s2
        )
        val finalSim = reviveElectrons(state.boardSize, postCap, dummyScores)
        
        if (captures <= 0 && !hasLiberties(state.boardSize, finalSim, move, mover)) {
            return null // Mouvement suicidaire
        }

        val nextPlayer = if (mover == Player.PLAYER1) Player.PLAYER2 else Player.PLAYER1
        return state.copy(
            electrons = finalSim,
            currentPlayer = nextPlayer,
            scores = dummyScores
        )
    }

    private fun evaluateBoard(state: GameState, aiPlayer: Player): Double {
        val opponent = if (aiPlayer == Player.PLAYER1) Player.PLAYER2 else Player.PLAYER1
        var score = 0.0

        // 1. Équilibre des forces (Score de capture)
        val aiScore = state.scores[aiPlayer] ?: 0
        val oppScore = state.scores[opponent] ?: 0
        score += (aiScore - oppScore) * weights.captureWeight

        // 2. ANALYSE PROFONDE DE CHAQUE ÉLECTRON ENNEMI (Étranglement & Pression constante)
        val activeElectrons = state.electrons.filter { !it.isCaptured }
        val activePositions = activeElectrons.map { it.position }.toSet()
        val oppElectrons = activeElectrons.filter { it.owner == opponent }
        val aiElectrons = activeElectrons.filter { it.owner == aiPlayer }

        var oppPressureScore = 0.0
        for (opp in oppElectrons) {
            var oppLiberties = 0
            listOf(Pair(1,0), Pair(-1,0), Pair(0,1), Pair(0,-1)).forEach { (dx, dy) ->
                val adj = Position(opp.position.x + dx, opp.position.y + dy)
                if (adj.x in 0..state.boardSize && adj.y in 0..state.boardSize) {
                    if (!activePositions.contains(adj)) {
                        oppLiberties++
                    }
                }
            }
            // Réduire les libertés de l'ennemi = pression maximale et anticipation d'encerclement
            if (oppLiberties == 1) {
                oppPressureScore += 3000.0 // Mettre l'ennemi en danger critique (Atari)
            } else if (oppLiberties == 0) {
                oppPressureScore += 8000.0 // Capture imminente
            } else {
                oppPressureScore += (4 - oppLiberties) * 250.0
            }
        }
        score += oppPressureScore

        // 3. Contrôle du centre
        val cx = state.boardSize / 2.0
        val cy = state.boardSize / 2.0
        for (e in aiElectrons) {
            val dist = abs(e.position.x - cx.toInt()) + abs(e.position.y - cy.toInt())
            val positionalValue = maxOf(0.0, state.boardSize.toDouble() - dist)
            score += positionalValue * weights.centerWeight
        }

        // 4. Bonus de structure (Angles / Virages) pour former des boucles
        val aiPositions = aiElectrons.map { it.position }.toSet()
        var structureBonus = 0.0
        for (p in aiPositions) {
            val hasN = aiPositions.contains(Position(p.x, p.y - 1))
            val hasS = aiPositions.contains(Position(p.x, p.y + 1))
            val hasE = aiPositions.contains(Position(p.x + 1, p.y))
            val hasW = aiPositions.contains(Position(p.x - 1, p.y))
            if ((hasN || hasS) && (hasE || hasW)) {
                structureBonus += 200.0
            }
        }
        score += structureBonus

        return score
    }

    private fun getStateKey(state: GameState, isMaximizing: Boolean): String {
        val activeElectrons = state.electrons.filter { !it.isCaptured }
            .sortedWith(compareBy({ it.position.x }, { it.position.y }))
            .joinToString(",") { "${it.position.x}:${it.position.y}:${it.owner}" }
        val scores = "${state.scores[Player.PLAYER1]}:${state.scores[Player.PLAYER2]}"
        return "$activeElectrons|$scores|$isMaximizing"
    }

    private fun fallbackHeuristicMove(
        state: GameState,
        candidates: List<Position>,
        player: Player,
        opponent: Player
    ): Position {
        fun evaluateMove(move: Position, mover: Player): Long {
            val initialSim = state.electrons.toMutableList()
            initialSim.add(Electron("sim", mover, move))
            
            val (captures, postCaptureElectrons) = computeCaptures(state.boardSize, initialSim, move, mover)
            val netCaptures = captures
            
            if (netCaptures <= 0 && !hasLiberties(state.boardSize, postCaptureElectrons, move, mover)) {
                return -1000000000L // Suicide
            }
            
            var score = 0L
            score += netCaptures * 1000000L

            if (difficulty >= 4) {
                var immediateLiberties = 0
                val directions = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
                for ((dx, dy) in directions) {
                    val adj = Position(move.x + dx, move.y + dy)
                    if (adj.x in 0..state.boardSize && adj.y in 0..state.boardSize) {
                        if (postCaptureElectrons.none { it.position == adj && !it.isCaptured }) {
                            immediateLiberties++
                        }
                    }
                }
                score += immediateLiberties * 5000L
            }

            if (difficulty >= 5) {
                val cx = state.boardSize / 2
                val cy = state.boardSize / 2
                val distToCenter = abs(move.x - cx) + abs(move.y - cy)
                score -= distToCenter * 100L
            }

            return score
        }

        val scoredMoves = candidates.map { pos ->
            val myScore = evaluateMove(pos, player)
            if (myScore < -500000000L) {
                return@map Pair(pos, myScore)
            }

            val oppScoreIfPlayedHere = evaluateMove(pos, opponent)
            var totalScore = myScore
            
            if (oppScoreIfPlayedHere > 0) {
                val blockWeight = if (difficulty >= 5) 1.2 else 0.8
                totalScore += (oppScoreIfPlayedHere * blockWeight).toLong()
            }

            if (difficulty < 6) {
                totalScore += (0..100).random()
            }

            Pair(pos, totalScore)
        }

        val validMoves = scoredMoves.filter { it.second > -500000000L }
        if (validMoves.isEmpty()) return candidates.random()

        val sorted = validMoves.sortedByDescending { it.second }

        return when (difficulty) {
            2 -> {
                val topChunk = sorted.take(maxOf(1, (sorted.size * 0.70).toInt()))
                topChunk.random().first
            }
            3 -> {
                val topChunk = sorted.take(maxOf(1, (sorted.size * 0.40).toInt()))
                topChunk.random().first
            }
            4 -> {
                sorted.take(3).random().first
            }
            5 -> {
                if ((0..100).random() < 15 && sorted.size > 1) {
                    sorted[1].first
                } else {
                    sorted.first().first
                }
            }
            else -> {
                sorted.first().first
            }
        }
    }
}
