package com.example.utils

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * AutoResizingText
 * ────────────────
 * Custom Compose component that dynamically shrinks font size down to 8.sp to ensure
 * text fits completely on a single line without wrapping, overlapping, or multi-lining.
 */
@Composable
fun AutoResizingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current
) {
    var resizedFontSize by remember(text, fontSize, style) {
        val initialSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize
        mutableStateOf(if (initialSize != TextUnit.Unspecified) initialSize else 16.sp)
    }
    var shouldDraw by remember(text) { mutableStateOf(false) }

    val mergedStyle = style.copy(
        color = if (color != Color.Unspecified) color else style.color,
        fontSize = resizedFontSize,
        fontWeight = fontWeight ?: style.fontWeight,
        textAlign = textAlign ?: style.textAlign,
        lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else style.lineHeight,
        fontFamily = fontFamily ?: style.fontFamily,
        fontStyle = fontStyle ?: style.fontStyle,
        letterSpacing = if (letterSpacing != TextUnit.Unspecified) letterSpacing else style.letterSpacing,
        textDecoration = textDecoration ?: style.textDecoration
    )

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (shouldDraw) {
                drawContent()
            }
        },
        style = mergedStyle,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth) {
                if (resizedFontSize.isSp && resizedFontSize.value > 8f) {
                    resizedFontSize = (resizedFontSize.value * 0.9f).sp
                } else {
                    shouldDraw = true
                }
            } else {
                shouldDraw = true
            }
        }
    )
}
