package software.kanunnikoff.izhitsa.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.text.font.FontWeight
import software.kanunnikoff.izhitsa.R

data class KeyInfo(
    val code: Int,
    val label: String? = null,
    val iconResId: Int? = null,
    val weight: Float = 1f,
    val isModifier: Boolean = false
)

@Composable
fun KeyboardScreen(
    rows: List<List<KeyInfo>>,
    onKeyClick: (Int) -> Unit
) {
    val navBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    val isDark = isSystemInDarkTheme()

    val palette = if (isDark) {
        KeyboardPalette(
            background = Color(0xFF1B1C1F),
            key = Color(0xFF2A2C31),
            keySecondary = Color(0xFF3A3D45),
            keyAccent = Color(0xFF3D4A67),
            text = Color(0xFFE6E6E6),
            textSecondary = Color(0xFFB7BBC5)
        )
    } else {
        KeyboardPalette(
            background = Color(0xFFF2F3F8),
            key = Color(0xFFFFFFFF),
            keySecondary = Color(0xFFE1E6F4),
            keyAccent = Color(0xFFD5E0FF),
            text = Color(0xFF111318),
            textSecondary = Color(0xFF4A4E57)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = palette.background)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .padding(bottom = navBarsPadding.calculateBottomPadding() * 2)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { key ->
                    KeyButton(
                        key = key,
                        modifier = Modifier.weight(key.weight),
                        palette = palette,
                        onClick = { onKeyClick(key.code) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

private data class KeyboardPalette(
    val background: Color,
    val key: Color,
    val keySecondary: Color,
    val keyAccent: Color,
    val text: Color,
    val textSecondary: Color
)

@Composable
private fun KeyButton(
    key: KeyInfo,
    modifier: Modifier = Modifier,
    palette: KeyboardPalette,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.10f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "key-press-scale"
    )

    val isModifier = key.isModifier || key.code < 0

    val keyColor = when {
        key.code == 10 -> palette.keyAccent
        isModifier -> palette.keySecondary
        else -> palette.key
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .clip(shape = RoundedCornerShape(size = 10.dp))
            .background(keyColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Можно добавить ripple
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (key.iconResId != null) {
            Icon(
                imageVector = ImageVector.vectorResource(id = key.iconResId),
                contentDescription = null,
                tint = palette.text,
                modifier = Modifier.size(24.dp)
            )
        } else if (key.label != null) {
            Text(
                text = key.label,
                color = palette.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
