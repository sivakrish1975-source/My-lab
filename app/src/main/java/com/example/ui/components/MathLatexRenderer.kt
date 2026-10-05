package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast

sealed class MathContentBlock {
    data class TextBlock(val content: String) : MathContentBlock()
    data class MathBlock(val rawLatex: String, val formatted: String) : MathContentBlock()
    data class HeadingBlock(val level: Int, val text: String) : MathContentBlock()
    data class BulletBlock(val text: String) : MathContentBlock()
}

object LatexConverter {

    private val GREEK_MAP = mapOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\epsilon" to "ε", "\\varepsilon" to "ε", "\\zeta" to "ζ", "\\eta" to "η",
        "\\theta" to "θ", "\\vartheta" to "ϑ", "\\iota" to "ι", "\\kappa" to "κ",
        "\\lambda" to "λ", "\\mu" to "μ", "\\nu" to "ν", "\\xi" to "ξ",
        "\\pi" to "π", "\\varpi" to "ϖ", "\\rho" to "ρ", "\\varrho" to "ϱ",
        "\\sigma" to "σ", "\\varsigma" to "ς", "\\tau" to "τ", "\\upsilon" to "υ",
        "\\phi" to "φ", "\\varphi" to "ϕ", "\\chi" to "χ", "\\psi" to "ψ",
        "\\omega" to "ω",
        // Capital Greek
        "\\Gamma" to "Γ", "\\Delta" to "Δ", "\\Theta" to "Θ", "\\Lambda" to "Λ",
        "\\Xi" to "Ξ", "\\Pi" to "Π", "\\Sigma" to "Σ", "\\Upsilon" to "Υ",
        "\\Phi" to "Φ", "\\Psi" to "Ψ", "\\Omega" to "Ω"
    )

    private val SYMBOLS_MAP = mapOf(
        "\\pm" to "±", "\\mp" to "∓", "\\times" to "×", "\\div" to "÷",
        "\\cdot" to "·", "\\ast" to "*", "\\star" to "★", "\\circ" to "°",
        "\\bullet" to "•", "\\approx" to "≈", "\\sim" to "∼", "\\simeq" to "≃",
        "\\cong" to "≅", "\\neq" to "≠", "\\ne" to "≠", "\\le" to "≤",
        "\\leq" to "≤", "\\ge" to "≥", "\\geq" to "≥", "\\ll" to "≪",
        "\\gg" to "≫", "\\infty" to "∞", "\\propto" to "∝", "\\equiv" to "≡",
        "\\in" to "∈", "\\notin" to "∉", "\\ni" to "∋", "\\subset" to "⊂",
        "\\supset" to "⊃", "\\subseteq" to "⊆", "\\supseteq" to "⊇",
        "\\cup" to "∪", "\\cap" to "∩", "\\setminus" to "∖",
        "\\forall" to "∀", "\\exists" to "∃", "\\nexists" to "∄", "\\empty" to "∅",
        "\\emptyset" to "∅", "\\nabla" to "∇", "\\partial" to "∂",
        "\\int" to "∫", "\\iint" to "∬", "\\iiint" to "∭", "\\oint" to "∮",
        "\\sum" to "∑", "\\prod" to "∏",
        "\\leftarrow" to "←", "\\gets" to "←", "\\rightarrow" to "→", "\\to" to "→",
        "\\leftrightarrow" to "↔", "\\Leftarrow" to "⇐", "\\Rightarrow" to "⇒",
        "\\Leftrightarrow" to "⇔", "\\uparrow" to "↑", "\\downarrow" to "↓",
        "\\degree" to "°", "\\angle" to "∠", "\\perp" to "⊥", "\\parallel" to "∥"
    )

    private val SUPERSCRIPT_MAP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ', 'x' to 'ˣ', 'y' to 'ʸ', 't' to 'ᵗ'
    )

    private val SUBSCRIPT_MAP = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'o' to 'ₒ', 'x' to 'ₓ', 'h' to 'ₕ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'p' to 'ₚ',
        's' to 'ₛ', 't' to 'ₜ'
    )

    /**
     * Converts a raw LaTeX formula string into a readable formatted Unicode string.
     */
    fun toReadableMath(latex: String): String {
        var res = latex.trim()

        // Remove math environment delimiters
        if (res.startsWith("$$") && res.endsWith("$$")) {
            res = res.removeSurrounding("$$")
        } else if (res.startsWith("\\[") && res.endsWith("\\]")) {
            res = res.removePrefix("\\[").removeSuffix("\\]")
        } else if (res.startsWith("$") && res.endsWith("$")) {
            res = res.removeSurrounding("$")
        } else if (res.startsWith("\\(") && res.endsWith("\\)")) {
            res = res.removePrefix("\\(").removeSuffix("\\)")
        }

        // Replace Greek symbols
        for ((k, v) in GREEK_MAP) {
            res = res.replace(Regex(Regex.escape(k) + "(?![a-zA-Z])"), v)
        }

        // Replace Mathematical symbols
        for ((k, v) in SYMBOLS_MAP) {
            res = res.replace(Regex(Regex.escape(k) + "(?![a-zA-Z])"), v)
        }

        // Fractions: \frac{num}{den} -> (num) / (den)
        val fracRegex = Regex("\\\\frac\\{([^{}]+)\\}\\{([^{}]+)\\}")
        while (fracRegex.containsMatchIn(res)) {
            res = res.replace(fracRegex) { match ->
                val num = match.groupValues[1].trim()
                val den = match.groupValues[2].trim()
                val cleanNum = if (num.contains(" ") || num.contains("+") || num.contains("-")) "($num)" else num
                val cleanDen = if (den.contains(" ") || den.contains("+") || den.contains("-")) "($den)" else den
                "$cleanNum / $cleanDen"
            }
        }

        // Square roots: \sqrt{arg} -> √(arg)
        val sqrtRegex = Regex("\\\\sqrt\\{([^{}]+)\\}")
        res = res.replace(sqrtRegex) { match ->
            "√(${match.groupValues[1].trim()})"
        }
        val sqrtNRegex = Regex("\\\\sqrt\\[([^{}\\[\\]]+)\\]\\{([^{}]+)\\}")
        res = res.replace(sqrtNRegex) { match ->
            "${match.groupValues[1].trim()}√(${match.groupValues[2].trim()})"
        }

        // Superscript conversion: ^{...} or ^x
        val supBraceRegex = Regex("\\^\\{([a-zA-Z0-9+-=]+)\\}")
        res = res.replace(supBraceRegex) { match ->
            match.groupValues[1].map { SUPERSCRIPT_MAP[it] ?: it }.joinToString("")
        }
        val supSingleRegex = Regex("\\^([0-9nixy23])")
        res = res.replace(supSingleRegex) { match ->
            val char = match.groupValues[1][0]
            SUPERSCRIPT_MAP[char]?.toString() ?: "^$char"
        }

        // Subscript conversion: _{...} or _x
        val subBraceRegex = Regex("_\\{([a-zA-Z0-9+-=]+)\\}")
        res = res.replace(subBraceRegex) { match ->
            match.groupValues[1].map { SUBSCRIPT_MAP[it] ?: it }.joinToString("")
        }
        val subSingleRegex = Regex("_([0-9aeoxhklt])")
        res = res.replace(subSingleRegex) { match ->
            val char = match.groupValues[1][0]
            SUBSCRIPT_MAP[char]?.toString() ?: "_$char"
        }

        // Clean up common LaTeX formatting wrappers
        res = res.replace(Regex("\\\\text\\{([^{}]+)\\}"), "$1")
        res = res.replace(Regex("\\\\mathrm\\{([^{}]+)\\}"), "$1")
        res = res.replace(Regex("\\\\mathbf\\{([^{}]+)\\}"), "$1")
        res = res.replace(Regex("\\\\mathit\\{([^{}]+)\\}"), "$1")
        res = res.replace(Regex("\\\\left|\\\\right"), "")
        res = res.replace("\\,", " ")
        res = res.replace("\\;", " ")
        res = res.replace("\\quad", "   ")
        res = res.replace("\\qquad", "      ")
        res = res.replace("\\%", "%")

        return res.trim()
    }

    /**
     * Parses a complete markdown string with LaTeX equations into structured blocks.
     */
    fun parseBlocks(text: String): List<MathContentBlock> {
        val blocks = mutableListOf<MathContentBlock>()
        val lines = text.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            // Check for block math ($$...$$ or \[...\])
            if (line.trim().startsWith("$$") || line.trim().startsWith("\\[")) {
                val mathLines = mutableListOf<String>()
                val startTrim = line.trim()

                if (startTrim.startsWith("$$") && startTrim.endsWith("$$") && startTrim.length > 2) {
                    val content = startTrim.removeSurrounding("$$")
                    blocks.add(MathContentBlock.MathBlock(content, toReadableMath(content)))
                    i++
                    continue
                } else if (startTrim.startsWith("\\[") && startTrim.endsWith("\\]") && startTrim.length > 2) {
                    val content = startTrim.removePrefix("\\[").removeSuffix("\\]")
                    blocks.add(MathContentBlock.MathBlock(content, toReadableMath(content)))
                    i++
                    continue
                }

                // Multi-line block math
                mathLines.add(line.replace("$$", "").replace("\\[", ""))
                i++
                while (i < lines.size && !lines[i].contains("$$") && !lines[i].contains("\\]")) {
                    mathLines.add(lines[i])
                    i++
                }
                if (i < lines.size) {
                    mathLines.add(lines[i].replace("$$", "").replace("\\]", ""))
                    i++
                }
                val rawMath = mathLines.joinToString("\n")
                blocks.add(MathContentBlock.MathBlock(rawMath, toReadableMath(rawMath)))
                continue
            }

            // Headings
            if (line.startsWith("### ")) {
                blocks.add(MathContentBlock.HeadingBlock(3, line.removePrefix("### ")))
                i++
                continue
            } else if (line.startsWith("## ")) {
                blocks.add(MathContentBlock.HeadingBlock(2, line.removePrefix("## ")))
                i++
                continue
            } else if (line.startsWith("# ")) {
                blocks.add(MathContentBlock.HeadingBlock(1, line.removePrefix("# ")))
                i++
                continue
            }

            // Bullet points
            if (line.trim().startsWith("- ") || line.trim().startsWith("* ") || line.trim().startsWith("• ")) {
                val bulletText = line.trim().removePrefix("- ").removePrefix("* ").removePrefix("• ")
                blocks.add(MathContentBlock.BulletBlock(bulletText))
                i++
                continue
            }

            // Regular paragraph or line with potential inline math
            if (line.isNotBlank()) {
                blocks.add(MathContentBlock.TextBlock(line))
            }
            i++
        }

        return blocks
    }
}

/**
 * High-performance, crash-free LaTeX Math & Markdown Renderer for Compose.
 * Handles both block equations (with copy shortcut and serif typography)
 * and inline math seamlessly without relying on volatile WebView processes.
 */
@Composable
fun MathLatexRenderer(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val blocks = LatexConverter.parseBlocks(text)
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MathContentBlock.HeadingBlock -> {
                    Text(
                        text = LatexConverter.toReadableMath(block.text),
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        },
                        color = accentColor,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                is MathContentBlock.BulletBlock -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        RenderInlineMathText(
                            rawText = block.text,
                            textColor = textColor,
                            accentColor = accentColor
                        )
                    }
                }

                is MathContentBlock.MathBlock -> {
                    // Elevated, beautifully formatted equation card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            accentColor.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = accentColor.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Functions,
                                        contentDescription = "Formula",
                                        tint = accentColor,
                                        modifier = Modifier.padding(4.dp).size(14.dp)
                                    )
                                }
                                Text(
                                    text = block.formatted,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Normal,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 17.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = accentColor
                                )
                            }

                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(block.formatted))
                                    Toast.makeText(context, "Equation copied: ${block.formatted}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy formula",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                is MathContentBlock.TextBlock -> {
                    RenderInlineMathText(
                        rawText = block.content,
                        textColor = textColor,
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderInlineMathText(
    rawText: String,
    textColor: Color,
    accentColor: Color
) {
    val annotatedString = buildAnnotatedString {
        // Regex matches $...$ or \(...\) for inline formulas
        val inlineRegex = Regex("\\$([^$]+)\\$|\\\\\\(([^)]+)\\\\\\)")
        var lastIndex = 0

        inlineRegex.findAll(rawText).forEach { match ->
            // Text before the inline formula
            val before = rawText.substring(lastIndex, match.range.first)
            appendMarkdownFormatted(before, textColor)

            // Convert and style the inline formula
            val formula = match.groupValues[1].ifEmpty { match.groupValues[2] }
            val cleanMath = LatexConverter.toReadableMath(formula)

            withStyle(
                SpanStyle(
                    color = accentColor,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            ) {
                append(" $cleanMath ")
            }

            lastIndex = match.range.last + 1
        }

        if (lastIndex < rawText.length) {
            val remaining = rawText.substring(lastIndex)
            appendMarkdownFormatted(remaining, textColor)
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = 22.sp
    )
}

private fun AnnotatedString.Builder.appendMarkdownFormatted(text: String, defaultColor: Color) {
    // Basic bold **text** parsing
    val boldRegex = Regex("\\*\\*([^*]+)\\*\\*")
    var lastIdx = 0

    boldRegex.findAll(text).forEach { match ->
        val before = text.substring(lastIdx, match.range.first)
        withStyle(SpanStyle(color = defaultColor)) {
            append(LatexConverter.toReadableMath(before))
        }

        val boldText = match.groupValues[1]
        withStyle(SpanStyle(color = defaultColor, fontWeight = FontWeight.Bold)) {
            append(LatexConverter.toReadableMath(boldText))
        }

        lastIdx = match.range.last + 1
    }

    if (lastIdx < text.length) {
        withStyle(SpanStyle(color = defaultColor)) {
            append(LatexConverter.toReadableMath(text.substring(lastIdx)))
        }
    }
}
