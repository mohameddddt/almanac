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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.almanac.app.ui.theme.LocalPalette
import com.almanac.app.ui.theme.Type
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

val p @Composable get() = LocalPalette.current

/** Plain ripple-free click: print doesn't ripple. */
@Composable
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clickable(remember { MutableInteractionSource() }, indication = null, enabled = enabled, onClick = onClick)

@Composable
fun Rule(modifier: Modifier = Modifier, color: Color = p.rule, thickness: Dp = 1.dp) {
    Box(modifier.fillMaxWidth().height(thickness).background(color))
}

/** Heavy-over-light double rule, as under a newspaper masthead. */
@Composable
fun DoubleRule(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Rule(color = p.ink, thickness = 2.dp)
        Spacer(Modifier.height(3.dp))
        Rule(color = p.ink, thickness = 1.dp)
    }
}

/** Numbered section head: `02  AGENDA ............ 3` over an ink rule. */
@Composable
fun SectionHead(index: String, title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 6.dp)) {
            Text(index, style = Type.tag, color = p.signal)
            Spacer(Modifier.width(10.dp))
            Text(title.uppercase(), style = Type.tag, color = p.ink, modifier = Modifier.weight(1f))
            if (trailing != null) Text(trailing, style = Type.tag, color = p.muted)
        }
        Rule(color = p.ink)
    }
}

/** Screen title block: big condensed word, a line of mono metadata, a double rule. */
@Composable
fun Masthead(title: String, meta: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Column(modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(title.uppercase(), style = MaterialTheme.typography.displayLarge, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), content = actions)
        }
        Spacer(Modifier.height(6.dp))
        Text(meta.uppercase(), style = Type.tag, color = p.muted)
        Spacer(Modifier.height(10.dp))
        DoubleRule()
    }
}

/** A row in a ledger: fixed-width mono lead column, content, optional trailing; hairline below. */
@Composable
fun LedgerRow(
    lead: String,
    modifier: Modifier = Modifier,
    leadColor: Color = p.muted,
    leadWidth: Dp = 64.dp,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().then(if (onClick != null) Modifier.tap(onClick = onClick) else Modifier)) {
        Row(Modifier.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(lead.uppercase(), style = Type.data, color = leadColor, modifier = Modifier.width(leadWidth))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, content = content)
            trailing?.invoke()
        }
        Rule()
    }
}

/** Square ballot box. Checked boxes fill with [color] and carry a drawn tick. */
@Composable
fun BallotBox(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, color: Color = p.ink, size: Dp = 22.dp, label: String? = null) {
    val fill by animateColorAsState(if (checked) color else Color.Transparent, label = "ballot")
    val tick = p.paper
    Box(
        modifier
            .size(size)
            .background(fill)
            .border(1.5.dp, color)
            .tap(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Canvas(Modifier.size(size * 0.55f)) {
                val path = Path().apply {
                    moveTo(0f, this@Canvas.size.height * 0.55f)
                    lineTo(this@Canvas.size.width * 0.38f, this@Canvas.size.height * 0.9f)
                    lineTo(this@Canvas.size.width, this@Canvas.size.height * 0.1f)
                }
                drawPath(path, tick, style = Stroke(2.dp.toPx(), cap = StrokeCap.Square, join = StrokeJoin.Miter))
            }
        } else if (label != null) {
            Text(label, style = Type.tag, color = p.muted)
        }
    }
}

/** Equal-width options in a ruled box; the chosen one is set solid. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(1.dp, p.ink),
    ) {
        options.forEachIndexed { i, label ->
            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(p.ink))
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .background(if (on) p.ink else Color.Transparent)
                    .tap { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label.uppercase(), style = Type.tag, color = if (on) p.paper else p.ink, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

/** Solid rectangular button with a mono label. */
@Composable
fun InkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = p.ink) {
    Box(
        modifier
            .background(if (enabled) color else p.shade)
            .tap(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = if (enabled) p.paper else p.muted)
    }
}

/** Outlined rectangular button. */
@Composable
fun FrameButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = p.ink) {
    Box(
        modifier
            .border(1.dp, color)
            .tap(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** Bare mono text action. */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = p.inkSoft) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = modifier.tap(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp),
    )
}

/** Square on/off switch. */
@Composable
fun SquareToggle(on: Boolean, onChange: (Boolean) -> Unit) {
    val track by animateColorAsState(if (on) p.signal else Color.Transparent, label = "track")
    Row(
        Modifier
            .width(46.dp)
            .height(24.dp)
            .background(track)
            .border(1.5.dp, if (on) p.signal else p.ink)
            .tap { onChange(!on) }
            .padding(3.dp),
        horizontalArrangement = if (on) Arrangement.End else Arrangement.Start,
    ) {
        Box(Modifier.size(18.dp).background(if (on) p.paper else p.ink))
    }
}

/** A quiet text field: text over a hairline, no box. */
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
    val style = (if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge).copy(color = p.ink)
    Column(modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = style,
            singleLine = singleLine,
            cursorBrush = SolidColor(p.signal),
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
        Rule(color = p.ink)
    }
}

/** Ink swatches for marking events and rituals. */
@Composable
fun InkPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        p.inks.forEachIndexed { i, c ->
            Box(
                Modifier
                    .size(28.dp)
                    .border(1.5.dp, if (i == selected) p.ink else Color.Transparent)
                    .padding(4.dp)
                    .background(c)
                    .tap { onSelect(i) },
            )
        }
    }
}

/** The moon for [date], drawn in ink on paper. */
@Composable
fun MoonGlyph(date: LocalDate, size: Dp, modifier: Modifier = Modifier) {
    // printed convention: the dark part is inked; in dark mode the light part is
    val lit = if (p.isDark) p.ink else p.paper
    val shadow = if (p.isDark) p.shade else p.ink
    val outline = p.ink
    val f = Moon.fraction(date)
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        val c = Offset(this.size.width / 2, this.size.height / 2)
        drawCircle(shadow, r, c)
        val k = cos(2 * PI * f).toFloat() // 1 new, -1 full
        val waxing = f < 0.5
        val half = Path().apply {
            addArc(Rect(c, r), if (waxing) -90f else 90f, 180f)
            close()
        }
        drawPath(half, lit)
        val w = abs(k) * r
        drawOval(if (k > 0) shadow else lit, Offset(c.x - w, c.y - r), Size(w * 2, r * 2))
        drawCircle(outline, r, c, style = Stroke(1.dp.toPx()))
    }
}

/** Thin progress line. */
@Composable
fun Meter(progress: Float, color: Color, modifier: Modifier = Modifier, thickness: Dp = 3.dp) {
    Box(modifier.fillMaxWidth().height(thickness).background(p.rule)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(thickness).background(color))
    }
}

@Composable
fun Blank(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = modifier.padding(vertical = 14.dp))
}
