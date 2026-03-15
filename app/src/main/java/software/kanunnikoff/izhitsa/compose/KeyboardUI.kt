package software.kanunnikoff.izhitsa.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1C)) // Темный фон клавиатуры
            .padding(all = 4.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { key ->
                    KeyButton(
                        key = key,
                        modifier = Modifier.weight(key.weight),
                        onClick = { onKeyClick(key.code) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun KeyButton(
    key: KeyInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(if (key.isModifier) Color(0xFF333333) else Color(0xFF4A4A4A))
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
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else if (key.label != null) {
            Text(
                text = key.label,
                color = Color.White,
                fontSize = 18.sp
            )
        }
    }
}
