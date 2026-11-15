package pe.edu.unjbg.studylock.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pe.edu.unjbg.studylock.domain.model.Course
import pe.edu.unjbg.studylock.domain.model.History
import pe.edu.unjbg.studylock.domain.model.WeeklyProgress
import pe.edu.unjbg.studylock.domain.repo.CourseRepository
import pe.edu.unjbg.studylock.domain.repo.StatsRepository

data class ProgressUi(
    val weekly: WeeklyProgress? = null,
    val courses: List<Course> = emptyList(),
    val history: History? = null,
)

class ProgressViewModel(
    stats: StatsRepository,
    courses: CourseRepository,
) : ViewModel() {
    val ui = combine(
        stats.observeWeekly(),
        courses.observeCourses(),
        stats.observeHistory(HISTORY_DAYS),
    ) { weekly, list, history ->
        ProgressUi(weekly = weekly, courses = list, history = history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUi())

    private companion object {
        const val HISTORY_DAYS = 14
    }
}
