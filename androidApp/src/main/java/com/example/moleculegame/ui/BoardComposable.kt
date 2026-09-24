package com.example.moleculegame.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.example.moleculegame.GameState
import com.example.moleculegame.Player
import com.example.moleculegame.Position
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

@Composable
fun BoardComposable(
    gameState: GameState,
    cellSizeDp: Dp,
    onCellClicked: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val baseCellSizePx = with(density) { cellSizeDp.toPx() }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    // Calcul précis du zoom centré sur les doigts pour éviter que la grille ne "fuit"
                    val newScale = (scale * zoom).coerceIn(0.1f, 10f)
                    val zoomFactor = newScale / scale
                    
                    val newOffsetX = centroid.x - (centroid.x - offset.x) * zoomFactor + pan.x
                    val newOffsetY = centroid.y - (centroid.y - offset.y) * zoomFactor + pan.y
                    
                    scale = newScale
                    offset = Offset(newOffsetX, newOffsetY)
                }
            }
            .pointerInput(gameState, scale, offset) {
                detectTapGestures { tapOffset ->
                    val cellSizePx = baseCellSizePx * scale
                    // On arrondit pour que le point se pose exactement sur l'intersection touchée
                    val x = round((tapOffset.x - offset.x) / cellSizePx).toInt()
                    val y = round((tapOffset.y - offset.y) / cellSizePx).toInt()

                    if (x in 0..gameState.boardSize && y in 0..gameState.boardSize) {
                        onCellClicked(Position(x, y))
                    }
                }
            }
    ) {
        val cellSizePx = baseCellSizePx * scale

        // Culling
        val startX = max(0, (-offset.x / cellSizePx).toInt())
        val endX = min(gameState.boardSize, ((size.width - offset.x) / cellSizePx).toInt() + 1)
        
        val startY = max(0, (-offset.y / cellSizePx).toInt())
        val endY = min(gameState.boardSize, ((size.height - offset.y) / cellSizePx).toInt() + 1)

        withTransform({
            translate(left = offset.x, top = offset.y)
        }) {
            // Dessin de la grille
            for (i in startX..endX) {
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(x = i * cellSizePx, y = startY * cellSizePx),
                    end = Offset(x = i * cellSizePx, y = endY * cellSizePx),
                    strokeWidth = 2f * scale
                )
            }
            for (i in startY..endY) {
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(x = startX * cellSizePx, y = i * cellSizePx),
                    end = Offset(x = endX * cellSizePx, y = i * cellSizePx),
                    strokeWidth = 2f * scale
                )
            }

            // Dessin des lignes de capture (polygone englobant l'adversaire)
            val drawnPolygons = mutableSetOf<List<Position>>()
            gameState.electrons.filter { it.isCaptured && it.capturerPolygon != null }.forEach { capturedElectron ->
                val polygon = capturedElectron.capturerPolygon!!
                if (polygon.size >= 2 && !drawnPolygons.contains(polygon)) {
                    drawnPolygons.add(polygon)
                    
                    val capturerOwner = if (capturedElectron.owner == Player.PLAYER1) Player.PLAYER2 else Player.PLAYER1
                    val lineColor = if (capturerOwner == Player.PLAYER1) Color.Blue else Color.Red

                    // Tracer les lignes entre tous les points de capture qui sont adjacents (y compris diagonales)
                    val boundaryList = polygon.toList()
                    for (i in 0 until boundaryList.size) {
                        for (j in i + 1 until boundaryList.size) {
                            val p1 = boundaryList[i]
                            val p2 = boundaryList[j]
                            
                            val dx = abs(p1.x - p2.x)
                            val dy = abs(p1.y - p2.y)
                            
                            // Si les deux points de la clôture se touchent (orthogonalement ou diagonalement)
                            if (dx <= 1 && dy <= 1) {
                                drawLine(
                                    color = lineColor.copy(alpha = 0.6f),
                                    start = Offset(p1.x * cellSizePx, p1.y * cellSizePx),
                                    end = Offset(p2.x * cellSizePx, p2.y * cellSizePx),
                                    strokeWidth = 6f * scale
                                )
                            }
                        }
                    }
                }
            }

            // Dessin des électrons EXACTEMENT sur les intersections de la grille
            gameState.electrons.filter { it.position.x in startX..endX && it.position.y in startY..endY }.forEach { electron ->
                val centerX = electron.position.x * cellSizePx
                val centerY = electron.position.y * cellSizePx

                val color = if (electron.owner == Player.PLAYER1) Color.Blue else Color.Red
                
                drawCircle(
                    color = color, // La couleur reste toujours la même, 100% opaque, quoi qu'il arrive
                    radius = cellSizePx / 4f,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = cellSizePx / 10f,
                    center = Offset(centerX - cellSizePx/15f, centerY - cellSizePx/15f)
                )
            }
        }
    }
}
