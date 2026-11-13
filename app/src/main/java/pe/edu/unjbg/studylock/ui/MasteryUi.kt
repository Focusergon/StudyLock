package pe.edu.unjbg.studylock.ui

import androidx.compose.ui.graphics.Color
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.domain.model.Mastery

fun masteryLabel(mastery: Mastery?): String = when (mastery) {
    Mastery.LEARNED -> "Aprendido"
    Mastery.NEEDS_REVIEW -> "Necesita repaso"
    Mastery.PENDING -> "Pendiente"
    null -> "Pendiente"
}

fun masteryColor(mastery: Mastery?): Color = when (mastery) {
    Mastery.LEARNED -> SlColors.Teal
    Mastery.NEEDS_REVIEW -> SlColors.Warning
    else -> SlColors.Muted
}

fun masteryBackground(mastery: Mastery?): Color = when (mastery) {
    Mastery.LEARNED -> SlColors.Mint
    Mastery.NEEDS_REVIEW -> Color(0xFFFDF3E4)
    else -> SlColors.ChipBlue
}
