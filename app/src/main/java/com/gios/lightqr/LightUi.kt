package com.gios.lightqr

import android.graphics.fonts.SystemFonts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// ── LightOS palette (matches LightThemeColors.Dark in the SDK) ──────────────
val LightBg = Color.Black
val LightInk = Color.White
val LightMuted = Color(0xFFBBBBBB)   // contentSecondary
val LightHair = Color(0xFF2A2A2A)    // hairline dividers

/** LightOS lays everything out on a grid; 1 unit ≈ 16dp here. */
fun gu(units: Float) = (units * 16f).dp

/**
 * LightOS ships the Akkurat typeface as a system font. On a real Light Phone III
 * we pick it up directly; on an emulator/other device we fall back to a light
 * sans-serif, which keeps the same restrained feel.
 */
val LightFont: FontFamily by lazy {
    runCatching {
        val fonts = SystemFonts.getAvailableFonts()
            .filter { it.file?.name?.startsWith("Akkurat", ignoreCase = true) == true }
            .mapNotNull { f ->
                val file = f.file ?: return@mapNotNull null
                val style = if (f.style.slant != 0) FontStyle.Italic else FontStyle.Normal
                Font(file, FontWeight(f.style.weight), style)
            }
        if (fonts.isNotEmpty()) FontFamily(fonts) else FontFamily.SansSerif
    }.getOrDefault(FontFamily.SansSerif)
}

// Type ramp — light weights, generous tracking on labels (Akkurat-like).
object LightType {
    val barLabel = TextStyle(fontFamily = LightFont, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, letterSpacing = 0.12.em)
    val title = TextStyle(fontFamily = LightFont, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, letterSpacing = 0.03.em)
    val copy = TextStyle(fontFamily = LightFont, fontWeight = FontWeight.Light,
        fontSize = 21.sp, lineHeight = 30.sp)
    val row = TextStyle(fontFamily = LightFont, fontWeight = FontWeight.Normal, fontSize = 17.sp)
    val detail = TextStyle(fontFamily = LightFont, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, letterSpacing = 0.02.em)
}

@Composable
fun LText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LightInk,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = color,
        style = if (align != null) style.copy(textAlign = align) else style,
        maxLines = maxLines,
        overflow = overflow,
    )
}

/** Simple tap wrapper without the Material ripple (LightOS has no ripples). */
@Composable
fun tap(onClick: () -> Unit): Modifier =
    Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )

/** Centered title bar with optional left/right text buttons — mirrors LightTopBar. */
@Composable
fun LightTopBar(
    title: String,
    left: Pair<String, () -> Unit>? = null,
    right: Pair<String, () -> Unit>? = null,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(gu(3.25f))
            .padding(horizontal = gu(1f)),
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(gu(5f)), contentAlignment = Alignment.CenterStart) {
                left?.let { (t, cb) ->
                    LText(t.uppercase(), LightType.barLabel, tap(cb), color = LightMuted)
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.width(gu(5f)), contentAlignment = Alignment.CenterEnd) {
                right?.let { (t, cb) ->
                    LText(t.uppercase(), LightType.barLabel, tap(cb), color = LightMuted)
                }
            }
        }
        LText(title.uppercase(), LightType.title, align = TextAlign.Center, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

/** Bottom action bar — up to three evenly spaced uppercase text actions. */
@Composable
fun LightBottomBar(actions: List<Pair<String, () -> Unit>>) {
    if (actions.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .height(gu(4f))
            .padding(horizontal = gu(2f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = when (actions.size) {
            1 -> Arrangement.Center
            else -> Arrangement.SpaceBetween
        },
    ) {
        actions.forEach { (t, cb) ->
            LText(t.uppercase(), LightType.barLabel, tap(cb))
        }
    }
}

@Composable
fun Hairline() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(LightHair))
}
