package pe.edu.unjbg.studylock.feature.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pe.edu.unjbg.studylock.domain.model.Course
import pe.edu.unjbg.studylock.domain.repo.CourseRepository

data class CoursesUi(
    val courses: List<Course> = emptyList(),
    val loading: Boolean = true,
) {
    val totalTopics: Int get() = courses.sumOf { it.topics.size }
    val learnedTopics: Int get() = courses.sumOf { it.learnedTopics }
}

class CoursesViewModel(
    courses: CourseRepository,
) : ViewModel() {
    val ui = courses.observeCourses()
        .map { CoursesUi(courses = it, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoursesUi())
}

class CourseDetailViewModel(
    courses: CourseRepository,
    courseId: String,
) : ViewModel() {
    val ui = courses.observeCourses()
        .map { list -> list.firstOrNull { it.id == courseId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
