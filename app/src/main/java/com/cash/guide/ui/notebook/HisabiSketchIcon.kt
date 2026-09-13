package com.cash.guide.ui.notebook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

enum class HisabiSymbol {
    Back,
    More,
    Plus,
    Trash,
    ChevronUp,
    ChevronDown,
    Backspace,
    Exclamation,
    Close,
    Check,
    Wallet,
    Page,
    Home,
    Search,
    Calculator,
    Clock,
    Gear,
    Lightbulb,
    Pencil,
    Copy,
    Calendar,
    Pin,
    PinFilled,
    Smile,
    Folder,
    Share,
    Undo,
    Table,
    Lock,
    Fingerprint,
    Bell,
    VolumeHigh,
    VolumeMute,
    Coin,
    Banknote
}

@Composable
fun HisabiSketchIcon(
    symbol: HisabiSymbol,
    contentDescription: String?,
    tint: Color = Ink,
    modifier: Modifier = Modifier,
    size: Dp = HisabiMetrics.IconSize
) {
    val semanticsModifier = if (contentDescription == null) modifier else {
        modifier.semantics { this.contentDescription = contentDescription }
    }
    Canvas(modifier = semanticsModifier.size(size)) {
        val scale = min(this.size.width, this.size.height) / 24f
        fun u(value: Float) = value * scale
        fun point(x: Float, y: Float) = Offset(u(x), u(y))
        val pen = Stroke(u(1.45f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val fine = Stroke(u(1.15f), cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (symbol) {
            HisabiSymbol.Back -> {
                val isRtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
                val xStart = if (isRtl) 7f else 16.5f
                val xPoint = if (isRtl) 16.5f else 7f
                drawLine(tint, point(xStart, 4.5f), point(xPoint, 12f), u(1.45f), StrokeCap.Round)
                drawLine(tint, point(xPoint, 12f), point(xStart, 19.5f), u(1.45f), StrokeCap.Round)
            }
            HisabiSymbol.More -> {
                drawCircle(tint, u(1.15f), point(12f, 5f))
                drawCircle(tint, u(1.15f), point(12f, 12f))
                drawCircle(tint, u(1.15f), point(12f, 19f))
            }
            HisabiSymbol.Plus -> {
                drawLine(tint, point(12f, 4.5f), point(12f, 19.5f), u(1.45f), StrokeCap.Round)
                drawLine(tint, point(4.5f, 12f), point(19.5f, 12f), u(1.45f), StrokeCap.Round)
            }
            HisabiSymbol.Trash -> {
                // Bin body
                val bin = Path().apply {
                    moveTo(u(6f), u(8.5f))
                    lineTo(u(7.2f), u(20f))
                    lineTo(u(16.8f), u(20f))
                    lineTo(u(18f), u(8.5f))
                }
                drawPath(bin, tint, style = fine)
                // Lid line
                drawLine(tint, point(4.5f, 8.5f), point(19.5f, 8.5f), u(1.35f), StrokeCap.Round)
                // Lid handle
                val lid = Path().apply {
                    moveTo(u(9f), u(8.5f))
                    lineTo(u(9f), u(5f))
                    lineTo(u(15f), u(5f))
                    lineTo(u(15f), u(8.5f))
                }
                drawPath(lid, tint, style = fine)
                // Inner vertical slits
                drawLine(tint, point(10f, 11.5f), point(10f, 17f), u(1.1f), StrokeCap.Round)
                drawLine(tint, point(14f, 11.5f), point(14f, 17f), u(1.1f), StrokeCap.Round)
            }
            HisabiSymbol.ChevronUp -> {
                val chevron = Path().apply {
                    moveTo(u(4.5f), u(15f))
                    lineTo(u(12f), u(8.5f))
                    lineTo(u(19.5f), u(15f))
                }
                drawPath(chevron, tint, style = pen)
            }
            HisabiSymbol.ChevronDown -> {
                val chevron = Path().apply {
                    moveTo(u(4.5f), u(9f))
                    lineTo(u(12f), u(15.5f))
                    lineTo(u(19.5f), u(9f))
                }
                drawPath(chevron, tint, style = pen)
            }
            HisabiSymbol.Backspace -> {
                val tag = Path().apply {
                    moveTo(u(3.8f), u(12f))
                    lineTo(u(8.8f), u(5.5f))
                    lineTo(u(20.5f), u(5.5f))
                    lineTo(u(20.5f), u(18.5f))
                    lineTo(u(8.8f), u(18.5f))
                    close()
                }
                drawPath(tag, tint, style = pen)
                // Cross inside
                drawLine(tint, point(11.5f, 9f), point(17.5f, 15f), u(1.35f), StrokeCap.Round)
                drawLine(tint, point(17.5f, 9f), point(11.5f, 15f), u(1.35f), StrokeCap.Round)
            }
            HisabiSymbol.Exclamation -> {
                drawLine(tint, point(12f, 4.5f), point(12f, 14.5f), u(1.6f), StrokeCap.Round)
                drawCircle(tint, u(1.15f), point(12f, 18.5f))
            }
            HisabiSymbol.Close -> {
                drawLine(tint, point(5.5f, 5.5f), point(18.5f, 18.5f), u(1.45f), StrokeCap.Round)
                drawLine(tint, point(18.5f, 5.5f), point(5.5f, 18.5f), u(1.45f), StrokeCap.Round)
            }
            HisabiSymbol.Check -> {
                drawLine(tint, point(4f, 13f), point(9f, 18f), u(1.5f), StrokeCap.Round)
                drawLine(tint, point(9f, 18f), point(20f, 5f), u(1.5f), StrokeCap.Round)
            }
            HisabiSymbol.Wallet -> {
                drawRoundRect(tint, topLeft = point(3.5f, 6f), size = Size(u(17f), u(13f)), cornerRadius = CornerRadius(u(2.2f)), style = pen)
                drawRoundRect(tint, topLeft = point(12.8f, 10f), size = Size(u(8.2f), u(5.5f)), cornerRadius = CornerRadius(u(1.5f)), style = pen)
                drawCircle(tint, u(.9f), point(16.2f, 12.8f))
                drawLine(tint, point(5f, 6f), point(16f, 4f), u(1.2f), StrokeCap.Round)
            }
            HisabiSymbol.Page -> {
                val page = Path().apply {
                    moveTo(u(5f), u(3f)); lineTo(u(15f), u(3f)); lineTo(u(19f), u(7f))
                    lineTo(u(19f), u(21f)); lineTo(u(5f), u(21f)); close()
                }
                drawPath(page, tint, style = pen)
                drawLine(tint, point(15f, 3f), point(15f, 7f), u(1.2f), StrokeCap.Round)
                drawLine(tint, point(15f, 7f), point(19f, 7f), u(1.2f), StrokeCap.Round)
                drawLine(tint, point(8f, 11f), point(16f, 11f), u(1.1f), StrokeCap.Round)
                drawLine(tint, point(8f, 15f), point(16f, 15f), u(1.1f), StrokeCap.Round)
                drawLine(tint, point(8f, 19f), point(13f, 19f), u(1.1f), StrokeCap.Round)
            }
            HisabiSymbol.Home -> {
                // Sleek sketched architectural home with roof, chimney, walls, and arched door
                // Roof
                val roof = Path().apply {
                    moveTo(u(3f), u(11f))
                    lineTo(u(12f), u(4.2f))
                    lineTo(u(21f), u(11f))
                }
                drawPath(roof, tint, style = pen)
                // Chimney
                drawLine(tint, point(16.5f, 7.5f), point(16.5f, 4.8f), u(1.3f), StrokeCap.Round)
                drawLine(tint, point(15.5f, 4.8f), point(17.5f, 4.8f), u(1.2f), StrokeCap.Round)
                // House body
                val walls = Path().apply {
                    moveTo(u(5.5f), u(10.5f))
                    lineTo(u(5.5f), u(19f))
                    lineTo(u(18.5f), u(19f))
                    lineTo(u(18.5f), u(10.5f))
                }
                drawPath(walls, tint, style = fine)
                // Arched door
                val door = Path().apply {
                    moveTo(u(9.5f), u(19f))
                    lineTo(u(9.5f), u(14.5f))
                    quadraticBezierTo(u(12f), u(12.5f), u(14.5f), u(14.5f))
                    lineTo(u(14.5f), u(19f))
                }
                drawPath(door, tint, style = fine)
            }
            HisabiSymbol.Search -> {
                drawCircle(tint, u(6.5f), point(10f, 10f), style = pen)
                drawLine(tint, point(14.8f, 14.8f), point(20f, 20f), u(1.55f), StrokeCap.Round)
            }
            HisabiSymbol.Calculator -> {
                drawRoundRect(
                    tint,
                    topLeft = point(4.5f, 3f),
                    size = Size(u(15f), u(18f)),
                    cornerRadius = CornerRadius(u(2.5f)),
                    style = pen
                )
                drawRoundRect(
                    tint,
                    topLeft = point(7f, 5.5f),
                    size = Size(u(10f), u(3.5f)),
                    cornerRadius = CornerRadius(u(1f)),
                    style = fine
                )
                drawCircle(tint, u(0.75f), point(8f, 12f))
                drawCircle(tint, u(0.75f), point(12f, 12f))
                drawCircle(tint, u(0.75f), point(16f, 12f))
                drawCircle(tint, u(0.75f), point(8f, 15.5f))
                drawCircle(tint, u(0.75f), point(12f, 15.5f))
                drawCircle(tint, u(0.75f), point(16f, 15.5f))
            }
            HisabiSymbol.Clock -> {
                // Sketched time & history dial with open backward circular arrow
                val arcPath = Path().apply {
                    // 280 degree arc
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(u(3.5f), u(3.5f), u(20.5f), u(20.5f)),
                        startAngleDegrees = -75f,
                        sweepAngleDegrees = 320f,
                        forceMoveTo = true
                    )
                }
                drawPath(arcPath, tint, style = pen)
                // Backward arrow head at the top-left opening
                val arrowHead = Path().apply {
                    moveTo(u(6.8f), u(2.2f))
                    lineTo(u(10.5f), u(3.8f))
                    lineTo(u(8.8f), u(7.2f))
                }
                drawPath(arrowHead, tint, style = pen)
                // Clock hands (hour & minute)
                drawLine(tint, point(12f, 12f), point(12f, 7.5f), u(1.4f), StrokeCap.Round)
                drawLine(tint, point(12f, 12f), point(15.5f, 12f), u(1.4f), StrokeCap.Round)
                drawCircle(tint, u(1.2f), point(12f, 12f))
            }
            HisabiSymbol.Gear -> {
                // Sleek authentic 6-tooth mechanical gear
                val rInner = 6.2f
                val rOuter = 8.8f
                val gearPath = Path().apply {
                    for (i in 0 until 6) {
                        val a0 = ((i * 60f - 14f) * Math.PI / 180.0).toFloat()
                        val a1 = ((i * 60f - 7f) * Math.PI / 180.0).toFloat()
                        val a2 = ((i * 60f + 7f) * Math.PI / 180.0).toFloat()
                        val a3 = ((i * 60f + 14f) * Math.PI / 180.0).toFloat()
                        val p0 = Offset(u(12f + rInner * kotlin.math.cos(a0)), u(12f + rInner * kotlin.math.sin(a0)))
                        val p1 = Offset(u(12f + rOuter * kotlin.math.cos(a1)), u(12f + rOuter * kotlin.math.sin(a1)))
                        val p2 = Offset(u(12f + rOuter * kotlin.math.cos(a2)), u(12f + rOuter * kotlin.math.sin(a2)))
                        val p3 = Offset(u(12f + rInner * kotlin.math.cos(a3)), u(12f + rInner * kotlin.math.sin(a3)))
                        if (i == 0) moveTo(p0.x, p0.y) else lineTo(p0.x, p0.y)
                        lineTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                    }
                    close()
                }
                drawPath(gearPath, tint, style = fine)
                // Center axle hole
                drawCircle(tint, u(3.2f), point(12f, 12f), style = pen)
            }
            HisabiSymbol.Lightbulb -> {
                val bulb = Path().apply {
                    moveTo(u(8.5f), u(13.5f))
                    cubicTo(u(5.5f), u(10.5f), u(5.5f), u(6f), u(12f), u(4.5f))
                    cubicTo(u(18.5f), u(6f), u(18.5f), u(10.5f), u(15.5f), u(13.5f))
                    lineTo(u(14.5f), u(16.5f))
                    lineTo(u(9.5f), u(16.5f))
                    close()
                }
                drawPath(bulb, tint, style = pen)
                drawLine(tint, point(10f, 18.5f), point(14f, 18.5f), u(1.4f), StrokeCap.Round)
                // Small filament rays
                drawLine(tint, point(3.5f, 10f), point(5f, 10f), u(1.2f), StrokeCap.Round)
                drawLine(tint, point(19f, 10f), point(20.5f, 10f), u(1.2f), StrokeCap.Round)
                drawLine(tint, point(12f, 2f), point(12f, 3.5f), u(1.2f), StrokeCap.Round)
            }
            HisabiSymbol.Pencil -> {
                val pencil = Path().apply {
                    moveTo(u(5f), u(19f))
                    lineTo(u(8f), u(19f))
                    lineTo(u(19f), u(8f))
                    lineTo(u(16f), u(5f))
                    lineTo(u(5f), u(16f))
                    close()
                }
                drawPath(pencil, tint, style = fine)
                drawLine(tint, point(14f, 7f), point(17f, 10f), u(1.1f), StrokeCap.Round)
                drawLine(tint, point(4f, 20f), point(5f, 19f), u(1.3f), StrokeCap.Round)
            }
            HisabiSymbol.Copy -> {
                val back = Path().apply {
                    moveTo(u(5f), u(15f))
                    lineTo(u(5f), u(5f))
                    lineTo(u(15f), u(5f))
                }
                drawPath(back, tint, style = pen)
                drawRoundRect(
                    tint,
                    topLeft = point(8f, 8f),
                    size = Size(u(11f), u(11f)),
                    cornerRadius = CornerRadius(u(2f)),
                    style = pen
                )
            }
            HisabiSymbol.Calendar -> {
                // Calendar body outline
                drawRoundRect(
                    tint,
                    topLeft = point(4f, 5.5f),
                    size = Size(u(16f), u(14.5f)),
                    cornerRadius = CornerRadius(u(2.5f)),
                    style = pen
                )
                // Header divider line
                drawLine(tint, point(4f, 9.5f), point(20f, 9.5f), u(1.2f))
                // Rings / binder loops
                drawLine(tint, point(8f, 3.5f), point(8f, 6.5f), u(1.5f), StrokeCap.Round)
                drawLine(tint, point(16f, 3.5f), point(16f, 6.5f), u(1.5f), StrokeCap.Round)
                // Small day dots
                drawCircle(tint, u(0.85f), point(8f, 13f))
                drawCircle(tint, u(0.85f), point(12f, 13f))
                drawCircle(tint, u(0.85f), point(16f, 13f))
                drawCircle(tint, u(0.85f), point(8f, 16.5f))
                drawCircle(tint, u(0.85f), point(12f, 16.5f))
            }
            HisabiSymbol.Pin -> {
                // Pin needle pointing down
                drawLine(tint, point(12f, 15f), point(12f, 21.5f), u(1.4f), StrokeCap.Round)
                // Bottom flange / base of pin head
                drawLine(tint, point(8f, 15f), point(16f, 15f), u(1.5f), StrokeCap.Round)
                // Pin body tapered sides
                val bodyPath = Path().apply {
                    moveTo(u(9f), u(8.5f))
                    lineTo(u(8.2f), u(15f))
                    moveTo(u(15f), u(8.5f))
                    lineTo(u(15.8f), u(15f))
                    // Top ridge
                    moveTo(u(8f), u(8.5f))
                    lineTo(u(16f), u(8.5f))
                }
                drawPath(bodyPath, tint, style = pen)
                // Top knob of pin
                drawRoundRect(
                    tint,
                    topLeft = point(9.5f, 4f),
                    size = Size(u(5f), u(4.5f)),
                    cornerRadius = CornerRadius(u(2f)),
                    style = pen
                )
            }
            HisabiSymbol.PinFilled -> {
                // Pin needle pointing down
                drawLine(tint, point(12f, 15f), point(12f, 21.5f), u(1.5f), StrokeCap.Round)
                // Bottom flange / base of pin head
                drawLine(tint, point(8f, 15f), point(16f, 15f), u(1.6f), StrokeCap.Round)
                // Pin body tapered sides (solid filled)
                val bodyPath = Path().apply {
                    moveTo(u(8.5f), u(8.5f))
                    lineTo(u(15.5f), u(8.5f))
                    lineTo(u(16f), u(15f))
                    lineTo(u(8f), u(15f))
                    close()
                }
                drawPath(bodyPath, tint)
                // Top knob of pin (solid filled)
                drawRoundRect(
                    tint,
                    topLeft = point(9.5f, 4f),
                    size = Size(u(5f), u(4.5f)),
                    cornerRadius = CornerRadius(u(2f))
                )
            }
            HisabiSymbol.Smile -> {
                // Circle face outline
                drawCircle(tint, u(9f), point(12f, 12f), style = pen)
                // Two expressive eyes
                drawCircle(tint, u(1.1f), point(8.5f, 9.5f))
                drawCircle(tint, u(1.1f), point(15.5f, 9.5f))
                // Happy hand-drawn smile arc
                val smilePath = Path().apply {
                    moveTo(u(7.5f), u(13.5f))
                    quadraticTo(u(12f), u(17.5f), u(16.5f), u(13.5f))
                }
                drawPath(smilePath, tint, style = pen)
            }
            HisabiSymbol.Folder -> {
                val folderOutline = Path().apply {
                    moveTo(u(3.5f), u(7f))
                    lineTo(u(8.5f), u(7f))
                    lineTo(u(10.5f), u(9f))
                    lineTo(u(20.5f), u(9f))
                    quadraticTo(u(21.5f), u(9f), u(21.5f), u(10f))
                    lineTo(u(21.5f), u(18.5f))
                    quadraticTo(u(21.5f), u(19.5f), u(20.5f), u(19.5f))
                    lineTo(u(3.5f), u(19.5f))
                    quadraticTo(u(2.5f), u(19.5f), u(2.5f), u(18.5f))
                    lineTo(u(2.5f), u(8f))
                    quadraticTo(u(2.5f), u(7f), u(3.5f), u(7f))
                    close()
                }
                drawPath(folderOutline, tint, style = pen)
                drawLine(tint, point(2.5f, 12.5f), point(21.5f, 12.5f), u(1.2f), StrokeCap.Round)
            }
            HisabiSymbol.Share -> {
                val isRtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
                val xSource = if (isRtl) 18f else 6f
                val xBranch = if (isRtl) 6f else 18f
                val nodeR = 2.1f
                // Three circular nodes
                drawCircle(tint, u(nodeR), point(xSource, 12f), style = pen)
                drawCircle(tint, u(nodeR), point(xBranch, 6f), style = pen)
                drawCircle(tint, u(nodeR), point(xBranch, 18f), style = pen)
                // Connecting lines between node perimeters
                val dx = if (isRtl) -1f else 1f
                drawLine(tint, point(xSource + dx * 1.9f, 11f), point(xBranch - dx * 1.9f, 7f), u(1.35f), StrokeCap.Round)
                drawLine(tint, point(xSource + dx * 1.9f, 13f), point(xBranch - dx * 1.9f, 17f), u(1.35f), StrokeCap.Round)
            }
            HisabiSymbol.Undo -> {
                val isRtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
                fun mx(x: Float) = if (isRtl) 24f - x else x

                // 1. Smooth arch tail curving from bottom-right over the top to the arrowhead
                val arch = Path().apply {
                    moveTo(u(mx(18.5f)), u(17.5f))
                    cubicTo(
                        u(mx(18.5f)), u(10.5f),
                        u(mx(14f)), u(5.5f),
                        u(mx(7f)), u(9.5f)
                    )
                }
                drawPath(arch, tint, style = pen)

                // 2. Crisp, prominent arrowhead pointing backward (left in LTR, right in RTL)
                val arrowHead = Path().apply {
                    moveTo(u(mx(10.5f)), u(5.5f))
                    lineTo(u(mx(5.5f)), u(9.5f))
                    lineTo(u(mx(10.5f)), u(13.5f))
                }
                drawPath(arrowHead, tint, style = pen)
            }
            HisabiSymbol.Table -> {
                drawRoundRect(
                    tint,
                    topLeft = point(3.5f, 4.5f),
                    size = Size(u(17f), u(15f)),
                    cornerRadius = CornerRadius(u(2f)),
                    style = pen
                )
                drawLine(tint, point(3.5f, 9.5f), point(20.5f, 9.5f), u(1.3f))
                drawLine(tint, point(3.5f, 14.5f), point(20.5f, 14.5f), u(1.1f))
                drawLine(tint, point(9.5f, 4.5f), point(9.5f, 19.5f), u(1.1f))
                drawLine(tint, point(15.5f, 4.5f), point(15.5f, 19.5f), u(1.1f))
            }
            HisabiSymbol.Lock -> {
                // Shackle (Arch)
                val shackle = Path().apply {
                    moveTo(u(7.5f), u(10.5f))
                    lineTo(u(7.5f), u(6.5f))
                    cubicTo(u(7.5f), u(3.5f), u(16.5f), u(3.5f), u(16.5f), u(6.5f))
                    lineTo(u(16.5f), u(10.5f))
                }
                drawPath(shackle, tint, style = Stroke(width = u(1.6f), cap = StrokeCap.Round))
                // Lock Body
                drawRoundRect(
                    tint,
                    topLeft = point(4.5f, 10.5f),
                    size = Size(u(15f), u(10.5f)),
                    cornerRadius = CornerRadius(u(2.5f)),
                    style = pen
                )
                // Keyhole
                drawCircle(tint, u(1.4f), point(12f, 14.5f), style = Stroke(width = u(1.2f)))
                drawLine(tint, point(12f, 15.5f), point(12f, 18f), u(1.3f), StrokeCap.Round)
            }
            HisabiSymbol.Fingerprint -> {
                // Hand-drawn fingerprint ridges (concentric sketch arches)
                val r1 = Path().apply {
                    moveTo(u(12f), u(18.5f))
                    lineTo(u(12f), u(14f))
                    cubicTo(u(12f), u(12.5f), u(13.5f), u(12.5f), u(13.5f), u(14.5f))
                    lineTo(u(13.5f), u(18f))
                }
                drawPath(r1, tint, style = pen)

                val r2 = Path().apply {
                    moveTo(u(9.5f), u(18f))
                    lineTo(u(9.5f), u(12.5f))
                    cubicTo(u(9.5f), u(8.5f), u(16f), u(8.5f), u(16f), u(13f))
                    lineTo(u(16f), u(18.5f))
                }
                drawPath(r2, tint, style = pen)

                val r3 = Path().apply {
                    moveTo(u(7f), u(16.5f))
                    lineTo(u(7f), u(11.5f))
                    cubicTo(u(7f), u(5.5f), u(18.5f), u(5.5f), u(18.5f), u(11f))
                    lineTo(u(18.5f), u(16.5f))
                }
                drawPath(r3, tint, style = pen)
            }
            HisabiSymbol.Bell -> {
                // Top loop / hanger
                val loop = Path().apply {
                    moveTo(u(10.5f), u(4.5f))
                    cubicTo(u(10.5f), u(2.2f), u(13.5f), u(2.2f), u(13.5f), u(4.5f))
                }
                drawPath(loop, tint, style = fine)

                // Bell dome and flared rim
                val bellBody = Path().apply {
                    moveTo(u(12f), u(4.5f))
                    cubicTo(u(8.2f), u(4.8f), u(6.5f), u(9.5f), u(6.5f), u(14f))
                    cubicTo(u(6.5f), u(15.5f), u(5.2f), u(16.5f), u(4.5f), u(17.2f))
                    lineTo(u(19.5f), u(17.2f))
                    cubicTo(u(18.8f), u(16.5f), u(17.5f), u(15.5f), u(17.5f), u(14f))
                    cubicTo(u(17.5f), u(9.5f), u(15.8f), u(4.8f), u(12f), u(4.5f))
                    close()
                }
                drawPath(bellBody, tint, style = pen)

                // Bottom clapper peeking out
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = point(10.2f, 16.5f),
                    size = Size(u(3.6f), u(3.2f)),
                    style = pen
                )
            }
            HisabiSymbol.VolumeHigh -> {
                // Speaker body (box + flared cone)
                val speaker = Path().apply {
                    moveTo(u(3.5f), u(9f))
                    lineTo(u(7.5f), u(9f))
                    lineTo(u(13.5f), u(5f))
                    lineTo(u(13.5f), u(19f))
                    lineTo(u(7.5f), u(15f))
                    lineTo(u(3.5f), u(15f))
                    close()
                }
                drawPath(speaker, tint, style = pen)
                // Sound wave 1
                drawArc(
                    color = tint,
                    startAngle = -40f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = point(13f, 8f),
                    size = Size(u(5f), u(8f)),
                    style = pen
                )
                // Sound wave 2
                drawArc(
                    color = tint,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = point(14.5f, 5.5f),
                    size = Size(u(7.5f), u(13f)),
                    style = fine
                )
            }
            HisabiSymbol.VolumeMute -> {
                // Speaker body
                val speaker = Path().apply {
                    moveTo(u(3.5f), u(9f))
                    lineTo(u(7.5f), u(9f))
                    lineTo(u(13.5f), u(5f))
                    lineTo(u(13.5f), u(19f))
                    lineTo(u(7.5f), u(15f))
                    lineTo(u(3.5f), u(15f))
                    close()
                }
                drawPath(speaker, tint, style = pen)
                // Mute X mark
                drawLine(tint, point(16f, 9.5f), point(21.5f, 15f), u(1.4f), StrokeCap.Round)
                drawLine(tint, point(21.5f, 9.5f), point(16f, 15f), u(1.4f), StrokeCap.Round)
            }
            HisabiSymbol.Coin -> {
                // Outer circle rim
                drawCircle(tint, u(8.5f), point(12f, 12f), style = pen)
                // Inner groove
                drawCircle(tint, u(6.2f), point(12f, 12f), style = fine)
                // Center denomination numeral 1
                drawLine(tint, point(12f, 8.5f), point(12f, 15.5f), u(1.4f), StrokeCap.Round)
                drawLine(tint, point(10.5f, 10.2f), point(12f, 8.5f), u(1.2f), StrokeCap.Round)
                drawLine(tint, point(10.2f, 15.5f), point(13.8f, 15.5f), u(1.2f), StrokeCap.Round)
            }
            HisabiSymbol.Banknote -> {
                // Outer note rectangle
                drawRoundRect(
                    color = tint,
                    topLeft = point(2.5f, 6f),
                    size = Size(u(19f), u(14.8f)),
                    cornerRadius = CornerRadius(u(2f), u(2f)),
                    style = pen
                )
                // Center watermark ellipse
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = point(9f, 8.5f),
                    size = Size(u(6f), u(9.8f)),
                    style = fine
                )
                // Side decorative hash strokes
                drawLine(tint, point(5f, 9.5f), point(5f, 17f), u(1.1f), StrokeCap.Round)
                drawLine(tint, point(19f, 9.5f), point(19f, 17f), u(1.1f), StrokeCap.Round)
            }
        }
    }
}
