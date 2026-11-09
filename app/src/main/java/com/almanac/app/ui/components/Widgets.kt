package com.almanac.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.almanac.app.ui.theme.LocalPalette
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.PI

val p @Composable get() = LocalPalette.current

/** Small-caps section heading followed by a hairline rule. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(p.line))
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    color: Color = p.card,
    padding: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(26.dp))
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

/** Outlined capsule chip. */
@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = p.ink,
    leading: (@Composable () -> Unit)? = null,
) {
    val bg by animateColorAsState(if (selected) accent else Color.Transparent, label = "chip")
    val fg by animateColorAsState(if (selected) p.paper else p.inkSoft, label = "chipText")
    Row(
        modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) accent else p.line, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.invoke()
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
fun RoundIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = p.ink,
    tint: Color = p.paper,
    size: Dp = 48.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = tint),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}

/** Large ink button used for primary actions in sheets. */
@Composable
fun InkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = p.ink) {
    Box(
        modifier
            .clip(CircleShape)
            .background(if (enabled) color else p.line)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) p.paper else p.muted)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = p.inkSoft) {
    Box(
        modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** A quiet text field: no box, just text over a hairline. */
@Composable
fun LineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    singleLine: Boolean = true,
    onDone: (() -> Unit)? = null,
) {
    val style = (if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge)
        .copy(color = p.ink)
    Column(modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = style,
            singleLine = singleLine,
            cursorBrush = SolidColor(p.terracotta),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = style.copy(color = p.muted))
                    inner()
                }
            },
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
    }
}

/** Row of swatches for picking an accent tint. */
@Composable
fun TintPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        p.tints.forEachIndexed { i, c ->
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (i == selected) p.ink else Color.Transparent, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(c)
                    .clickable { onSelect(i) },
            )
        }
    }
}

/** A drawn moon showing the real phase for [date]. */
@Composable
fun MoonGlyph(date: LocalDate, size: Dp, modifier: Modifier = Modifier) {
    val lit = p.brass
    val shadow = if (p.isDark) Color(0xFF2B3036) else Color(0xFFD9CEBB)
    val f = Moon.fraction(date)
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        val c = Offset(this.size.width / 2, this.size.height / 2)
        drawCircle(shadow, r, c)
        val k = cos(2 * PI * f).toFloat() // 1 new, -1 full
        val waxing = f < 0.5
        // lit half-disc on the side the sun is on
        val half = Path().apply {
            addArc(Rect(c, r), if (waxing) -90f else 90f, 180f)
            close()
        }
        drawPath(half, lit)
        // terminator ellipse: shadow for crescents, light for gibbous
        val w = abs(k) * r
        drawOval(
            color = if (k > 0) shadow else lit,
            topLeft = Offset(c.x - w, c.y - r),
            size = Size(w * 2, r * 2),
        )
        drawCircle(lit.copy(alpha = 0.35f), r, c, style = Stroke(1.dp.toPx()))
    }
}

/** Thin circular progress ring with a rounded cap. */
@Composable
fun Ring(progress: Float, color: Color, modifier: Modifier = Modifier, track: Color = p.line, stroke: Dp = 6.dp) {
    Canvas(modifier) {
        val s = stroke.toPx()
        val inset = s / 2
        val arcSize = Size(size.width - s, size.height - s)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(s))
        drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(s, cap = StrokeCap.Round))
    }
}

/** Thin horizontal progress bar. */
@Composable
fun Bar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(p.line)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).clip(CircleShape).background(color))
    }
}

@Composable
fun ScreenTitle(kicker: String, title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(kicker.uppercase(), style = MaterialTheme.typography.labelMedium, color = p.terracotta)
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.displaySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
        color = p.muted,
        modifier = modifier.padding(vertical = 14.dp),
    )
}
