package pe.edu.unjbg.studylock.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.unjbg.studylock.domain.model.ClassBlock
import pe.edu.unjbg.studylock.domain.model.Course
import pe.edu.unjbg.studylock.domain.repo.CourseRepository
import pe.edu.unjbg.studylock.domain.repo.ScheduleRepository
import java.time.LocalTime
import java.util.UUID

/** Estado del editor de horario de clases. */
data class ScheduleUi(
    val blocks: List<ClassBlock> = emptyList(),
    val courses: List<Course> = emptyList(),
    val dayOfWeek: Int = 1,
    val courseName: String = "",
    val startText: String = "08:00",
    val endText: String = "10:00",
    val room: String = "",
    val error: String? = null,
) {
    val todayBlocks: List<ClassBlock> get() = blocks.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startMinute }

    /** Franjas libres del día seleccionado entre las 07:00 y las 22:00. */
    val freeWindows: List<Pair<Int, Int>> get() = ScheduleUi.freeWindows(dayOfWeek, blocks)

    companion object {
        const val DAY_START = 7 * 60
        const val DAY_END = 22 * 60

        fun freeWindows(dayOfWeek: Int, blocks: List<ClassBlock>): List<Pair<Int, Int>> {
            val busy = blocks.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startMinute }
            val out = ArrayList<Pair<Int, Int>>()
            var cursor = DAY_START
            for (block in busy) {
                if (block.startMinute > cursor) out += cursor to block.startMinute
                cursor = maxOf(cursor, block.endMinute)
            }
            if (cursor < DAY_END) out += cursor to DAY_END
            return out.filter { it.second - it.first >= 20 }
        }
    }
}

/** Gestiona el horario de clases que alimenta las notificaciones inteligentes. */
class ScheduleViewModel(
    private val schedule: ScheduleRepository,
    courses: CourseRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(ScheduleUi())
    val ui: StateFlow<ScheduleUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            combine(schedule.observe(), courses.observeCourses()) { blocks, courseList ->
                blocks to courseList
            }.collect { (blocks, courseList) ->
                _ui.update { it.copy(blocks = blocks, courses = courseList) }
            }
        }
    }

    fun selectDay(day: Int) = _ui.update { it.copy(dayOfWeek = day.coerceIn(1, 7), error = null) }
    fun setCourseName(value: String) = _ui.update { it.copy(courseName = value, error = null) }
    fun setStartText(value: String) = _ui.update { it.copy(startText = value, error = null) }
    fun setEndText(value: String) = _ui.update { it.copy(endText = value, error = null) }
    fun setRoom(value: String) = _ui.update { it.copy(room = value) }
    fun dismissError() = _ui.update { it.copy(error = null) }

    fun addBlock() {
        val state = _ui.value
        val name = state.courseName.trim()
        if (name.isBlank()) {
            _ui.update { it.copy(error = "Escribe el nombre del curso o la actividad.") }
            return
        }
        val start = parseMinute(state.startText)
        val end = parseMinute(state.endText)
        if (start == null || end == null) {
            _ui.update { it.copy(error = "Usa el formato HH:MM, por ejemplo 08:30.") }
            return
        }
        if (end <= start) {
            _ui.update { it.copy(error = "La hora de fin debe ser posterior a la de inicio.") }
            return
        }
        val course = state.courses.firstOrNull { it.name.equals(name, ignoreCase = true) }
        val block = ClassBlock(
            id = UUID.randomUUID().toString(),
            courseId = course?.id.orEmpty(),
            courseName = name,
            dayOfWeek = state.dayOfWeek,
            startMinute = start,
            endMinute = end,
            room = state.room.trim(),
        )
        viewModelScope.launch {
            schedule.add(block)
            _ui.update { it.copy(courseName = "", room = "", error = null) }
        }
    }

    fun removeBlock(id: String) {
        viewModelScope.launch { schedule.remove(id) }
    }

    /** Acepta «8:5», «08:05», «0805» y devuelve minutos desde las 00:00. */
    private fun parseMinute(raw: String): Int? {
        val text = raw.trim()
        val normalized = when {
            text.contains(':') -> text
            text.length in 3..4 && text.all(Char::isDigit) -> text.dropLast(2) + ":" + text.takeLast(2)
            else -> return null
        }
        val (h, m) = normalized.split(':').takeIf { it.size == 2 } ?: return null
        val hour = h.toIntOrNull() ?: return null
        val minute = m.toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    @Suppress("unused")
    private fun nowLabel(): String = LocalTime.now().let { "%02d:%02d".format(it.hour, it.minute) }
}
