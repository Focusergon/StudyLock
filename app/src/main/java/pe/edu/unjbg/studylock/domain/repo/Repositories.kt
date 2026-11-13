package pe.edu.unjbg.studylock.domain.repo

import kotlinx.coroutines.flow.Flow
import pe.edu.unjbg.studylock.domain.model.BlockedApp
import pe.edu.unjbg.studylock.domain.model.Course
import pe.edu.unjbg.studylock.domain.model.Dashboard
import pe.edu.unjbg.studylock.domain.model.MinuteTx
import pe.edu.unjbg.studylock.domain.model.Question
import pe.edu.unjbg.studylock.domain.model.SessionKind
import pe.edu.unjbg.studylock.domain.model.Topic
import pe.edu.unjbg.studylock.domain.model.WeeklyProgress

data class SessionDraft(
    val sessionId: String,
    val courseId: String,
    val questions: List<Question>,
)

interface CourseRepository {
    fun observeCourses(): Flow<List<Course>>
    suspend fun getCourse(id: String): Course?
    suspend fun getTopics(courseId: String): List<Topic>
    suspend fun weakestCourseAndTopic(): Pair<Course, Topic>?
}

interface StudyRepository {
    suspend fun startSession(courseId: String, kind: SessionKind, count: Int): SessionDraft
    suspend fun startWeakSession(kind: SessionKind, count: Int): SessionDraft
    suspend fun answer(sessionId: String, question: Question, chosenOptionId: String): Boolean
    suspend fun finishSession(sessionId: String, questionCount: Int, correctCount: Int, minutesAwarded: Int)
}

interface MinuteRepository {
    fun observeBalance(): Flow<Int>
    fun observeLedger(limit: Int = 40): Flow<List<MinuteTx>>
    suspend fun balance(): Int
    suspend fun earn(minutes: Int, note: String)
    suspend fun spend(minutes: Int, note: String)
    suspend fun grantEmergency(minutes: Int, note: String)
}

interface BlockedAppsRepository {
    fun observe(): Flow<List<BlockedApp>>
    suspend fun saveAll(apps: List<BlockedApp>)
    suspend fun setEnabled(packageName: String, enabled: Boolean)
    suspend fun remove(packageName: String)
    suspend fun enabledPackages(): Set<String>
}

interface StatsRepository {
    fun observeDashboard(): Flow<Dashboard>
    fun observeWeekly(): Flow<WeeklyProgress>
    suspend fun todayEarned(): Int
    suspend fun todayQuestions(): Int
}
