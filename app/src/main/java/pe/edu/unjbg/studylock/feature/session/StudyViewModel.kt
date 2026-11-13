package pe.edu.unjbg.studylock.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.unjbg.studylock.di.AppGraph
import pe.edu.unjbg.studylock.domain.RewardPolicy
import pe.edu.unjbg.studylock.domain.model.Question
import pe.edu.unjbg.studylock.domain.model.SessionKind
import pe.edu.unjbg.studylock.navigation.Routes

data class StudyUi(
    val loading: Boolean = true,
    val courseName: String = "",
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val selectedOptionId: String? = null,
    val answered: Boolean = false,
    val wasCorrect: Boolean = false,
    val correctCount: Int = 0,
    val finishing: Boolean = false,
    val finished: Boolean = false,
    val minutesAwarded: Int = 0,
    val empty: Boolean = false,
) {
    val current: Question? get() = questions.getOrNull(index)
    val total: Int get() = questions.size
    val isLast: Boolean get() = index >= questions.lastIndex
    val accuracy: Int get() = if (questions.isEmpty()) 0 else correctCount * 100 / questions.size
}

class StudyViewModel(
    private val graph: AppGraph,
    private val courseId: String,
) : ViewModel() {

    private val _ui = MutableStateFlow(StudyUi())
    val ui: StateFlow<StudyUi> = _ui.asStateFlow()

    private var sessionId: String = ""
    private var finishedOnce = false

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val weak = courseId == Routes.WEAK
            val course = if (weak) null else graph.courses.getCourse(courseId)
            val draft = if (weak) {
                graph.study.startWeakSession(SessionKind.REVIEW, RewardPolicy.REVIEW_QUESTIONS)
            } else {
                graph.study.startSession(courseId, SessionKind.REVIEW, RewardPolicy.REVIEW_QUESTIONS)
            }
            sessionId = draft.sessionId
            _ui.value = StudyUi(
                loading = false,
                courseName = course?.name ?: "Repaso adaptativo",
                questions = draft.questions,
                empty = draft.questions.isEmpty(),
            )
        }
    }

    fun choose(optionId: String) {
        val state = _ui.value
        val question = state.current ?: return
        if (state.answered || state.finished || state.loading) return
        _ui.update { it.copy(selectedOptionId = optionId) }
        viewModelScope.launch {
            val correct = graph.study.answer(sessionId, question, optionId)
            _ui.update {
                it.copy(
                    answered = true,
                    wasCorrect = correct,
                    correctCount = it.correctCount + if (correct) 1 else 0,
                )
            }
        }
    }

    fun next() {
        val state = _ui.value
        if (!state.answered) return
        if (state.isLast) {
            finish(state)
        } else {
            _ui.update {
                it.copy(
                    index = it.index + 1,
                    selectedOptionId = null,
                    answered = false,
                    wasCorrect = false,
                )
            }
        }
    }

    fun finishNow() {
        finish(_ui.value)
    }

    private fun finish(state: StudyUi) {
        if (finishedOnce || state.questions.isEmpty()) return
        finishedOnce = true
        _ui.update { it.copy(finishing = true) }
        viewModelScope.launch {
            val minutes = RewardPolicy.minutesFor(SessionKind.REVIEW, state.total, state.correctCount)
            graph.study.finishSession(sessionId, state.total, state.correctCount, minutes)
            _ui.update { it.copy(finishing = false, finished = true, minutesAwarded = minutes) }
        }
    }
}
