package pe.edu.unjbg.studylock.data.repos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.edu.unjbg.studylock.core.util.TimeUtils
import pe.edu.unjbg.studylock.data.local.StudyLockDatabase
import pe.edu.unjbg.studylock.data.local.count
import pe.edu.unjbg.studylock.data.local.i
import pe.edu.unjbg.studylock.data.local.list
import pe.edu.unjbg.studylock.data.local.str
import pe.edu.unjbg.studylock.data.prefs.Prefs
import pe.edu.unjbg.studylock.data.prefs.UserPreferences
import pe.edu.unjbg.studylock.domain.HistoryStats
import pe.edu.unjbg.studylock.domain.model.BlockedApp
import pe.edu.unjbg.studylock.domain.model.Course
import pe.edu.unjbg.studylock.domain.model.DailyStatsRow
import pe.edu.unjbg.studylock.domain.model.Dashboard
import pe.edu.unjbg.studylock.domain.model.DayActivity
import pe.edu.unjbg.studylock.domain.model.DayHistory
import pe.edu.unjbg.studylock.domain.model.History
import pe.edu.unjbg.studylock.domain.model.WeeklyProgress
import pe.edu.unjbg.studylock.domain.repo.CourseRepository
import pe.edu.unjbg.studylock.domain.repo.StatsRepository
import java.time.LocalDate

class StatsRepositoryImpl(
    private val db: StudyLockDatabase,
    private val prefs: UserPreferences,
    private val courses: CourseRepository,
    private val io: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) : StatsRepository {

    override fun observeDashboard(): Flow<Dashboard> =
        combine(db.changes, prefs.flow) { _, p -> p }
            .map { buildDashboard(it) }
            .flowOn(io)

    override fun observeWeekly(): Flow<WeeklyProgress> =
        db.changes.map { buildWeekly() }.flowOn(io)

    override fun observeHistory(days: Int): Flow<History> =
        db.changes.map { buildHistory(days) }.flowOn(io)

    override suspend fun todayEarned(): Int = withContext(io) { today().earned }

    override suspend fun todayQuestions(): Int = withContext(io) { today().questions }

    private suspend fun buildDashboard(p: Prefs): Dashboard {
        val today = today()
        val week = lastDays(7)
        val next = courses.weakestCourseAndTopic()
        return Dashboard(
            availableMinutes = balance(),
            minutesEarnedToday = today.earned,
            minutesSpentToday = today.spent,
            questionsToday = today.questions,
            correctToday = today.correct,
            streakDays = streak(),
            week = week,
            blockedApps = blockedApps(),
            nextCourse = next?.first,
            nextTopic = next?.second,
            userName = p.displayName,
        )
    }

    private fun buildWeekly(): WeeklyProgress {
        val week = lastDays(7)
        val from = TimeUtils.lastNDays(7).first().toString()
        val stats = statsSince(from)
        val totalQuestions = stats.sumOf { it.questions }
        val totalCorrect = stats.sumOf { it.correct }
        val startMs = TimeUtils.startOfDay(System.currentTimeMillis() - 6L * 24 * 60 * 60 * 1000)
        val sessions = db.readableDatabase.count(
            "SELECT COUNT(*) FROM sessions WHERE finished_at IS NOT NULL AND finished_at >= $startMs",
        )
        return WeeklyProgress(
            minutes = week.sumOf { it.minutes },
            sessions = sessions,
            questions = totalQuestions,
            correct = totalCorrect,
            consecutiveDays = streak(),
            week = week,
        )
    }

    private fun balance(): Int = db.readableDatabase.count("SELECT COALESCE(SUM(minutes), 0) FROM minute_tx")

    private fun buildHistory(days: Int): History {
        val dates = TimeUtils.lastNDays(days)
        val from = dates.first().toString()
        val rows = db.readableDatabase.list(
            "SELECT * FROM daily_stats WHERE day >= ?",
            arrayOf(from),
        ) { c ->
            c.str("day") to DailyStatsRow(
                questions = c.i("questions"),
                correct = c.i("correct"),
                earned = c.i("earned"),
                spent = c.i("spent"),
                minutes = c.i("minutes"),
            )
        }.toMap()

        val list = dates.map { date ->
            val row = rows[date.toString()] ?: DailyStatsRow()
            DayHistory(
                dayKey = date.toString(),
                dayLabel = TimeUtils.dayShort(date),
                minutes = row.minutes,
                earned = row.earned,
                spent = row.spent,
                questions = row.questions,
                correct = row.correct,
            )
        }
        return History(
            days = list,
            totalMinutes = list.sumOf { it.minutes },
            totalQuestions = list.sumOf { it.questions },
            activeDays = HistoryStats.activeDays(list),
            bestStreak = HistoryStats.bestStreak(list),
            currentStreak = streak(),
        )
    }

    private fun today(): DailyStatsRow = db.readableDatabase.list(
        "SELECT * FROM daily_stats WHERE day = ?",
        arrayOf(TimeUtils.todayKey()),
    ) { c ->
        DailyStatsRow(
            questions = c.i("questions"),
            correct = c.i("correct"),
            earned = c.i("earned"),
            spent = c.i("spent"),
            minutes = c.i("minutes"),
        )
    }.firstOrNull() ?: DailyStatsRow()

    private fun statsSince(day: String): List<DailyStatsRow> = db.readableDatabase.list(
        "SELECT * FROM daily_stats WHERE day >= ? ORDER BY day",
        arrayOf(day),
    ) { c ->
        DailyStatsRow(
            questions = c.i("questions"),
            correct = c.i("correct"),
            earned = c.i("earned"),
            spent = c.i("spent"),
            minutes = c.i("minutes"),
        )
    }

    private fun lastDays(n: Int): List<DayActivity> {
        val days = TimeUtils.lastNDays(n)
        val from = days.first().toString()
        val byDay = db.readableDatabase.list(
            "SELECT day, minutes FROM daily_stats WHERE day >= ?",
            arrayOf(from),
        ) { c -> c.str("day") to c.i("minutes") }.toMap()
        return days.map { date -> DayActivity(TimeUtils.dayLabel(date), byDay[date.toString()] ?: 0) }
    }

    private fun streak(): Int {
        val days = db.readableDatabase.list(
            "SELECT day FROM daily_stats WHERE questions > 0 ORDER BY day DESC LIMIT 120",
        ) { c -> c.str("day") }.toSet()
        if (days.isEmpty()) return 0
        var cursor = LocalDate.now()
        if (cursor.toString() !in days) cursor = cursor.minusDays(1)
        var streak = 0
        while (cursor.toString() in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun blockedApps(): List<BlockedApp> = db.readableDatabase.list(
        "SELECT * FROM blocked_apps WHERE enabled = 1 ORDER BY label",
    ) { c ->
        BlockedApp(
            packageName = c.str("package"),
            label = c.str("label"),
            category = c.str("category"),
            enabled = true,
        )
    }

    @Suppress("unused")
    private fun Course.topicCount(): Int = topics.size
}
