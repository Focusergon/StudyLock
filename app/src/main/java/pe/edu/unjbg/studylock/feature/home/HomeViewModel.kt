package pe.edu.unjbg.studylock.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pe.edu.unjbg.studylock.data.prefs.UserPreferences
import pe.edu.unjbg.studylock.domain.model.Dashboard
import pe.edu.unjbg.studylock.domain.repo.StatsRepository

data class HomeUi(
    val dashboard: Dashboard? = null,
    val goalMinutes: Int = 30,
    val emergencyEnabled: Boolean = true,
)

class HomeViewModel(
    stats: StatsRepository,
    prefs: UserPreferences,
) : ViewModel() {

    val ui = combine(stats.observeDashboard(), prefs.flow) { dashboard, p ->
        HomeUi(
            dashboard = dashboard,
            goalMinutes = p.dailyGoalMinutes,
            emergencyEnabled = p.emergencyEnabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUi())
}
