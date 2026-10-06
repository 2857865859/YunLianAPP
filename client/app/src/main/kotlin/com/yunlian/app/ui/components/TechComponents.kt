package com.yunlian.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunlian.app.R
import com.yunlian.app.ui.theme.TechBackground
import com.yunlian.app.ui.theme.TechBackgroundSecondary
import com.yunlian.app.ui.theme.TechBlue
import com.yunlian.app.ui.theme.TechCyan
import com.yunlian.app.ui.theme.TechOutline
import com.yunlian.app.ui.theme.TechPurple
import com.yunlian.app.ui.theme.TechSurface
import com.yunlian.app.ui.theme.TechTextPrimary
import com.yunlian.app.ui.theme.TechTextSecondary

val TechGradient = Brush.horizontalGradient(listOf(TechPurple, TechBlue, TechCyan))

object TechDimens {
    val PageHorizontal = 20.dp
    val CardRadius = 18.dp
    val CardPadding = 18.dp
    val SectionGap = 16.dp
}

@Composable
fun TechPageHero(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2.405f)
    ) {
        Image(
            painter = painterResource(R.drawable.resolve_hero_background_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, TechBackground)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = TechTextPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "✦",
                    color = TechCyan,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            subtitle?.let {
                Text(
                    text = it,
                    color = TechTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun TechBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(TechBackground, TechBackgroundSecondary, TechBackground))
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val grid = 48.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(Color(0x143278FF), Offset(x, 0f), Offset(x, size.height), 0.5.dp.toPx())
                x += grid
            }
            var y = 0f
            while (y < size.height) {
                drawLine(Color(0x1035D8FF), Offset(0f, y), Offset(size.width, y), 0.5.dp.toPx())
                y += grid
            }
            drawCircle(Color(0x147C4DFF), radius = size.minDimension * .42f, center = Offset(size.width, 0f))
            drawCircle(Color(0x0D35D8FF), radius = size.minDimension * .32f, center = Offset(0f, size.height * .72f))
        }
        content()
    }
}

@Composable
fun TechCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Color = TechBlue,
    highlighted: Boolean = false,
    padding: Dp = TechDimens.CardPadding,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(TechDimens.CardRadius)
    val clickable = if (onClick == null) Modifier else Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
    Box(
        modifier = modifier
            .graphicsLayer { alpha = 0.99f }
            .background(TechSurface, shape)
            .border(
                BorderStroke(
                    if (highlighted) 1.5.dp else 1.dp,
                    if (highlighted) {
                        Brush.horizontalGradient(listOf(TechPurple, TechBlue, TechCyan))
                    } else {
                        Brush.horizontalGradient(listOf(accent.copy(.55f), TechOutline, TechCyan.copy(.22f)))
                    }
                ),
                shape
            )
            .then(clickable)
            .padding(padding)
    ) { content() }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { alpha = if (enabled) 1f else .45f }
            .background(TechGradient, shape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(9.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TechPageIntro(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TechTextPrimary, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = TechTextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Surface(
            modifier = Modifier.size(62.dp),
            shape = RoundedCornerShape(20.dp),
            color = TechPurple.copy(.16f),
            border = BorderStroke(1.dp, TechPurple.copy(.45f))
        ) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = TechCyan, modifier = Modifier.size(30.dp)) } }
    }
}

@Composable
fun TechCubeMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val top = Offset(w * .52f, h * .16f)
        val left = Offset(w * .22f, h * .34f)
        val right = Offset(w * .82f, h * .34f)
        val center = Offset(w * .52f, h * .52f)
        val bottom = Offset(w * .52f, h * .82f)

        drawOval(
            color = TechPurple.copy(alpha = .22f),
            topLeft = Offset(w * .06f, h * .34f),
            size = Size(w * .88f, h * .54f),
            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        )
        drawCircle(TechCyan.copy(alpha = .9f), 2.5.dp.toPx(), Offset(w * .10f, h * .66f))
        drawCircle(TechPurple.copy(alpha = .9f), 2.dp.toPx(), Offset(w * .88f, h * .40f))

        val topFace = Path().apply {
            moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(center.x, center.y); lineTo(left.x, left.y); close()
        }
        drawPath(topFace, Brush.linearGradient(listOf(TechCyan.copy(.68f), TechPurple.copy(.42f))))
        val leftFace = Path().apply {
            moveTo(left.x, left.y); lineTo(center.x, center.y); lineTo(bottom.x, bottom.y); lineTo(w * .22f, h * .63f); close()
        }
        drawPath(leftFace, Brush.linearGradient(listOf(TechBlue.copy(.48f), TechPurple.copy(.72f))))
        val rightFace = Path().apply {
            moveTo(center.x, center.y); lineTo(right.x, right.y); lineTo(w * .82f, h * .63f); lineTo(bottom.x, bottom.y); close()
        }
        drawPath(rightFace, Brush.linearGradient(listOf(TechPurple.copy(.72f), TechBlue.copy(.38f))))

        listOf(
            top to left, top to right, left to center, right to center,
            left to Offset(w * .22f, h * .63f), right to Offset(w * .82f, h * .63f),
            center to bottom, Offset(w * .22f, h * .63f) to bottom,
            Offset(w * .82f, h * .63f) to bottom
        ).forEach { (a, b) -> drawLine(TechCyan.copy(.72f), a, b, 1.2.dp.toPx()) }

        drawRect(
            brush = Brush.linearGradient(listOf(TechCyan.copy(.75f), TechPurple.copy(.62f))),
            topLeft = Offset(w * .42f, h * .29f),
            size = Size(w * .20f, h * .18f)
        )
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(16.dp).background(TechGradient, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(9.dp))
        Text(text, color = TechTextSecondary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(Modifier.size(64.dp), CircleShape, color = TechPurple.copy(.16f), border = BorderStroke(1.dp, TechBlue.copy(.5f))) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = TechCyan, modifier = Modifier.size(28.dp)) }
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = TechTextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (description != null) {
            Spacer(Modifier.height(6.dp))
            Text(description, color = TechTextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            GradientButton(actionLabel, onAction, true, Modifier.fillMaxWidth())
        }
    }
}
