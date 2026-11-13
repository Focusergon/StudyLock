package pe.edu.unjbg.studylock.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pe.edu.unjbg.studylock.domain.model.Course
import pe.edu.unjbg.studylock.domain.model.WeeklyProgress
import pe.edu.unjbg.studylock.domain.repo.CourseRepository
import pe.edu.unjbg.studylock.domain.repo.StatsRepository

data class ProgressUi(
    val weekly: WeeklyProgress? = null,
    val courses: List<Course> = emptyList(),
)

class ProgressViewModel(
    stats: StatsRepository,
    courses: CourseRepository,
) : ViewModel() {
    val ui = combine(stats.observeWeekly(), courses.observeCourses()) { weekly, list ->
        ProgressUi(weekly = weekly, courses = list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUi())
}
