package pe.edu.unjbg.studylock.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.edu.unjbg.studylock.domain.model.DayActivity

@Composable
fun SlScreen(
    modifier: Modifier = Modifier,
    background: Color = SlColors.Background,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .systemBarsPadding(),
        content = content,
    )
}

@Composable
fun SlTopBar(
    onBack: (() -> Unit)? = null,
    title: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SlColors.ChipBlue)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = SlColors.Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title.orEmpty(),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = SlColors.Ink,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun SlCard(
    modifier: Modifier = Modifier,
    color: Color = SlColors.Surface,
    radius: Dp = 18.dp,
    border: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val stroke = if (border) BorderStroke(1.dp, SlColors.Border) else null
    if (onClick != null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = color,
            border = stroke,
            onClick = onClick,
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = color,
            border = stroke,
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun SlPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
    containerColor: Color = SlColors.Primary,
    contentColor: Color = SlColors.Surface,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = SlColors.Border,
            disabledContentColor = SlColors.Muted,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(text = text, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SlTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = SlColors.Muted,
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(placeholder, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = SlColors.Muted)
            },
            singleLine = true,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlColors.Surface,
                unfocusedContainerColor = SlColors.Surface,
                focusedBorderColor = SlColors.Primary,
                unfocusedBorderColor = SlColors.Border,
            ),
        )
    }
}

@Composable
fun SlTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = SlColors.Primary) {
    TextButton(onClick = onClick, modifier = modifier, colors = ButtonDefaults.textButtonColors(contentColor = color)) {
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SlSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        color = SlColors.Muted,
    )
}

@Composable
fun SlPill(text: String, textColor: Color, background: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = textColor)
    }
}

@Composable
fun SlIconBadge(icon: ImageVector, tint: Color, background: Color, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.48f))
    }
}

@Composable
fun SlProgressBar(progress: Float, color: Color = SlColors.Teal, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(SlColors.Border),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
fun SlSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SlColors.Surface,
            unfocusedContainerColor = SlColors.Surface,
            focusedBorderColor = SlColors.Primary,
            unfocusedBorderColor = SlColors.Border,
        ),
    )
}

@Composable
fun SlCheckMark(checked: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (checked) SlColors.Teal else SlColors.ChipBlue)
            .border(1.dp, if (checked) SlColors.Teal else SlColors.Border, RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = SlColors.Surface, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SlStatColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = SlColors.Ink,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = SlColors.Muted,
        )
    }
}

enum class OptionState { NORMAL, SELECTED, CORRECT, WRONG }

@Composable
fun SlAnswerOption(
    letter: String,
    text: String,
    state: OptionState,
    onClick: () -> Unit,
) {
    val background = when (state) {
        OptionState.NORMAL -> SlColors.Surface
        OptionState.SELECTED -> SlColors.ChipBlue
        OptionState.CORRECT -> SlColors.Mint
        OptionState.WRONG -> SlColors.DangerSoft
    }
    val borderColor = when (state) {
        OptionState.NORMAL -> SlColors.Border
        OptionState.SELECTED -> SlColors.Primary
        OptionState.CORRECT -> SlColors.Teal
        OptionState.WRONG -> SlColors.Danger
    }
    val badgeBg = when (state) {
        OptionState.NORMAL -> SlColors.ChipBlue
        OptionState.SELECTED -> SlColors.Primary
        OptionState.CORRECT -> SlColors.Teal
        OptionState.WRONG -> SlColors.Danger
        else -> SlColors.ChipBlue
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(badgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = SlColors.Surface,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = SlColors.Ink,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun SlBarChart(week: List<DayActivity>, modifier: Modifier = Modifier, highlightIndex: Int = week.lastIndex) {
    val max = (week.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            if (week.isEmpty()) return@Canvas
            val gap = size.width * 0.03f
            val barWidth = (size.width - gap * (week.size - 1)) / week.size
            week.forEachIndexed { index, day ->
                val ratio = day.minutes.toFloat() / max
                val barHeight = (size.height * ratio).coerceAtLeast(6f)
                val x = index * (barWidth + gap)
                val color = if (index == highlightIndex) SlColors.Primary else SlColors.ChipBlue
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            week.forEach { day ->
                Text(
                    text = day.dayLabel,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = SlColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun SlEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = SlColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = SlColors.Muted,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}
