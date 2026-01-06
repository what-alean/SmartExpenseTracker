package com.example.smart_expense_tracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min

val vibrantColors = listOf(
    Color(0xFFF44336), // Red
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF673AB7), // Deep Purple
    Color(0xFF3F51B5), // Indigo
    Color(0xFF2196F3), // Blue
    Color(0xFF03A9F4), // Light Blue
    Color(0xFF00BCD4), // Cyan
    Color(0xFF009688), // Teal
    Color(0xFF4CAF50), // Green
    Color(0xFF8BC34A), // Light Green
    Color(0xFFCDDC39), // Lime
    Color(0xFFFFEB3B), // Yellow
    Color(0xFFFFC107), // Amber
    Color(0xFFFF9800), // Orange
    Color(0xFFFF5722), // Deep Orange
)


data class PieChartData(
    val label: String,
    val value: Double,
    val color: Color
)

data class PieChartConfig(
    val strokeWidth: Float,
    val animationDuration: Int
)

@Composable
fun PieChartCompose(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 30f,
    animationDuration: Int = 1000
) {
    val totalValue = data.sumOf { it.value }
    val animationProgressAnim = remember(data) { Animatable(0f) }

    LaunchedEffect(data, totalValue) {
        animationProgressAnim.snapTo(0f)
        animationProgressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(animationDuration)
        )
    }
    
    val pieConfig = remember(strokeWidth, animationDuration) {
        PieChartConfig(strokeWidth, animationDuration)
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        PieChartCanvas(
            data = data,
            totalValue = totalValue,
            animationProgress = animationProgressAnim.value,
            config = pieConfig
        )
    }
}

@Composable
private fun PieChartCanvas(
    data: List<PieChartData>,
    totalValue: Double,
    animationProgress: Float,
    config: PieChartConfig
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(180.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = min(size.width, size.height) / 2 - config.strokeWidth

            var currentAngle = -PI / 2 // 从12点钟方向开始

            data.forEach { pieData ->
                val sweepAngleRad = 2 * PI * (pieData.value / totalValue) * animationProgress
                // convert to degrees for drawArc
                val startAngleDeg = (currentAngle * 180.0 / PI).toFloat()
                val sweepAngleDeg = (sweepAngleRad * 180.0 / PI).toFloat()

                // 绘制饼图扇形
                drawArc(
                    color = pieData.color,
                    startAngle = startAngleDeg,
                    sweepAngle = sweepAngleDeg,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = config.strokeWidth)
                )

                currentAngle += sweepAngleRad
            }
        }

        // 中心圆
        Canvas(
            modifier = Modifier.size(120.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = min(size.width, size.height) / 2

            drawCircle(
                color = colorScheme.surface,
                radius = radius,
                center = center
            )
        }
    }
}

@Composable
fun PieChartWithLegend(
    data: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 饼图
        PieChartCompose(
            data = data,
            modifier = Modifier.size(200.dp),
            strokeWidth = 40f
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 图例
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(data) { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 颜色指示器
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(item.color)
                    )
                    
                    // 标签和百分比
                    val percentage = if (data.sumOf { it.value } > 0) {
                        ((item.value / data.sumOf { it.value }) * 100).toInt()
                    } else {
                        0
                    }
                    
                    Text(
                        text = "${item.label} ${percentage}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}