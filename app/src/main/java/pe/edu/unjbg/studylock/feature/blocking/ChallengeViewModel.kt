package pe.edu.unjbg.studylock.feature.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.unjbg.studylock.di.AppGraph
import pe.edu.unjbg.studylock.domain.RewardPolicy
import pe.edu.unjbg.studylock.domain.model.Question
import pe.edu.unjbg.studylock.domain.model.SessionKind

enum class ChallengePhase { LOCKED, LOADING, QUESTION, RESULT }

data class ChallengeUi(
    val phase: ChallengePhase = ChallengePhase.LOCKED,
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val selectedOptionId: String? = null,
    val answered: Boolean = false,
    val wasCorrect: Boolean = false,
    val correctCount: Int = 0,
    val minutesAwarded: Int = 0,
    val emergency: Boolean = false,
    val emergencyEnabled: Boolean = true,
    val error: String? = null,
) {
    val current: Question? get() = questions.getOrNull(index)
    val total: Int get() = questions.size
    val isLast: Boolean get() = index >= questions.lastIndex
}

class ChallengeViewModel(
    private val graph: AppGraph,
) : ViewModel() {

    private val _ui = MutableStateFlow(ChallengeUi())
    val ui: StateFlow<ChallengeUi> = _ui.asStateFlow()

    private var sessionId: String = ""
    private var finishing = false

    init {
        viewModelScope.launch {
            graph.prefs.flow.collect { p ->
                _ui.update { it.copy(emergencyEnabled = p.emergencyEnabled) }
            }
        }
    }

    /** Reinicia el reto cuando la actividad se reutiliza para otra app interceptada. */
    fun reset() {
        finishing = false
        sessionId = ""
        val emergencyEnabled = _ui.value.emergencyEnabled
        _ui.value = ChallengeUi(emergencyEnabled = emergencyEnabled)
    }

    fun startChallenge() {
        if (_ui.value.phase != ChallengePhase.LOCKED) return
        _ui.update { it.copy(phase = ChallengePhase.LOADING, error = null) }
        viewModelScope.launch {
            val draft = graph.study.startWeakSession(SessionKind.CHALLENGE, RewardPolicy.CHALLENGE_QUESTIONS)
            sessionId = draft.sessionId
            if (draft.questions.isEmpty()) {
                _ui.update {
                    it.copy(
                        phase = ChallengePhase.RESULT,
                        minutesAwarded = 0,
                        error = "No hay preguntas disponibles. Usa el desbloqueo de emergencia.",
                    )
                }
            } else {
                _ui.update {
                    it.copy(phase = ChallengePhase.QUESTION, questions = draft.questions, index = 0)
                }
            }
        }
    }

    fun choose(optionId: String) {
        val state = _ui.value
        val question = state.current ?: return
        if (state.phase != ChallengePhase.QUESTION || state.answered) return
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
        if (!state.isLast) {
            _ui.update {
                it.copy(index = it.index + 1, selectedOptionId = null, answered = false, wasCorrect = false)
            }
            return
        }
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            val minutes = RewardPolicy.minutesFor(SessionKind.CHALLENGE, state.total, state.correctCount)
            graph.study.finishSession(sessionId, state.total, state.correctCount, minutes)
            _ui.update {
                it.copy(phase = ChallengePhase.RESULT, minutesAwarded = minutes, emergency = false)
            }
        }
    }

    fun emergencyUnlock() {
        viewModelScope.launch {
            val prefs = graph.prefs.flow.first()
            val elapsed = System.currentTimeMillis() - prefs.lastEmergencyAt
            val cooldownMs = RewardPolicy.EMERGENCY_COOLDOWN_MINUTES * 60_000L
            if (prefs.lastEmergencyAt != 0L && elapsed < cooldownMs) {
                val left = ((cooldownMs - elapsed) / 60_000L) + 1
                _ui.update {
                    it.copy(error = "Ya usaste el desbloqueo de emergencia. Inténtalo en $left min o resuelve el reto.")
                }
                return@launch
            }
            graph.minutes.grantEmergency(
                RewardPolicy.EMERGENCY_MINUTES,
                "Desbloqueo de emergencia",
            )
            graph.prefs.markEmergencyUsed(System.currentTimeMillis())
            _ui.update {
                it.copy(
                    phase = ChallengePhase.RESULT,
                    minutesAwarded = RewardPolicy.EMERGENCY_MINUTES,
                    emergency = true,
                    error = null,
                )
            }
        }
    }
}
