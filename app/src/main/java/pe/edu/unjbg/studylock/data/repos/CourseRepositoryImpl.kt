package pe.edu.unjbg.studylock.data.repos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pe.edu.unjbg.studylock.data.local.StudyLockDatabase
import pe.edu.unjbg.studylock.data.local.d
import pe.edu.unjbg.studylock.data.local.i
import pe.edu.unjbg.studylock.data.local.l
import pe.edu.unjbg.studylock.data.local.list
import pe.edu.unjbg.studylock.data.local.str
import pe.edu.unjbg.studylock.domain.model.Course
import pe.edu.unjbg.studylock.domain.model.Mastery
import pe.edu.unjbg.studylock.domain.model.Topic
import pe.edu.unjbg.studylock.domain.repo.CourseRepository

class CourseRepositoryImpl(
    private val db: StudyLockDatabase,
    private val io: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) : CourseRepository {

    override fun observeCourses(): Flow<List<Course>> =
        db.changes.map { loadCourses() }.flowOn(io)

    override suspend fun getCourse(id: String): Course? = withContext(io) {
        loadCourses().firstOrNull { it.id == id }
    }

    override suspend fun getTopics(courseId: String): List<Topic> = withContext(io) {
        loadTopics(courseId)
    }

    override suspend fun weakestCourseAndTopic(): Pair<Course, Topic>? = withContext(io) {
        loadCourses().asSequence()
            .flatMap { course -> course.topics.asSequence().map { course to it } }
            .filter { (_, topic) -> topic.mastery != Mastery.LEARNED }
            .minByOrNull { (_, topic) -> topic.priority }
    }

    private fun loadCourses(): List<Course> {
        val courses = db.readableDatabase.list("SELECT * FROM courses ORDER BY sort") { c ->
            Course(
                id = c.str("id"),
                name = c.str("name"),
                professor = c.str("professor"),
                semester = c.str("semester"),
                accent = c.l("accent"),
            )
        }
        if (courses.isEmpty()) return emptyList()
        val topics = loadAllTopics().groupBy { it.courseId }
        return courses.map { it.copy(topics = topics[it.id].orEmpty()) }
    }

    private fun loadAllTopics(): List<Topic> =
        db.readableDatabase.list("SELECT * FROM topics ORDER BY sort") { c -> c.toTopic() }

    private fun loadTopics(courseId: String): List<Topic> =
        db.readableDatabase.list("SELECT * FROM topics WHERE course_id = ? ORDER BY sort", arrayOf(courseId)) { c ->
            c.toTopic()
        }

    private fun android.database.Cursor.toTopic(): Topic = Topic(
        id = str("id"),
        courseId = str("course_id"),
        name = str("name"),
        reviewCount = i("review_count"),
        correctCount = i("correct_count"),
        ease = d("ease"),
        intervalDays = i("interval_days"),
        repStreak = i("rep_streak"),
        lastReviewAt = l("last_review_at"),
    )
}
