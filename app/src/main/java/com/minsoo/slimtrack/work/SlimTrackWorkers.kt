package com.minsoo.slimtrack.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.minsoo.slimtrack.R
import com.minsoo.slimtrack.SlimTrackApp
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val CHANNEL_ID_COACH = "slimtrack_coach_channel"
    const val CHANNEL_ID_REMINDER = "slimtrack_missing_meal_channel"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val coachChannel = NotificationChannel(
            CHANNEL_ID_COACH,
            "일요일 아침 주간 코치 리포트 (09:00)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "매주 일요일 오전 9시에 주간 체중/칼로리 엄격 어드바이스를 알립니다."
        }
        val reminderChannel = NotificationChannel(
            CHANNEL_ID_REMINDER,
            "저녁 9시 식사 미기록 실패 경고 (21:00)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "당일 21시까지 식사 기록이 없으면 실패 판정 경고 알림을 보냅니다."
        }
        nm.createNotificationChannel(coachChannel)
        nm.createNotificationChannel(reminderChannel)
    }

    fun sendNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(notificationId, notification)
    }
}

class HealthSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? SlimTrackApp ?: return Result.success()
        runCatching {
            app.repository.syncFromHealthConnect()
        }
        return Result.success()
    }
}

/**
 * Fires once per week on Sunday morning at 09:00 (user requirement #1).
 */
class SundayMorningAdviceWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? SlimTrackApp ?: return Result.success()
        val settings = app.repository.settingsStore.settingsFlow.first()
        if (settings.sundayMorningAlarmEnabled) {
            runCatching {
                val advice = app.repository.generateAndPersistAdvice(kind = "WEEKLY_SUNDAY")
                NotificationHelper.sendNotification(
                    context = applicationContext,
                    channelId = NotificationHelper.CHANNEL_ID_COACH,
                    notificationId = 1001,
                    title = "📋 일요일 09:00 주간 코치 판정: ${advice.headline}",
                    body = advice.bodyText
                )
            }
        }
        AlarmScheduler.scheduleNextSunday0900(applicationContext)
        return Result.success()
    }
}

/**
 * Fires every evening at 21:00 (user requirement #2).
 * If no meals are logged for today, marks the day as RED failure and alerts the user.
 */
class MissingMeal21Worker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? SlimTrackApp ?: return Result.success()
        val settings = app.repository.settingsStore.settingsFlow.first()
        if (settings.missingMeal21AlarmEnabled) {
            val todayIso = LocalDate.now().toString()
            val count = app.repository.dao.countMealsForDate(todayIso)
            if (count == 0) {
                NotificationHelper.sendNotification(
                    context = applicationContext,
                    channelId = NotificationHelper.CHANNEL_ID_REMINDER,
                    notificationId = 1002,
                    title = "🔴 [실패 판정 경고] 오늘 식사 기록이 0건입니다 (21:00)",
                    body = "식사를 기록하지 않은 날은 캘린더에 실패(빨강)로 기록됩니다. 지금 즉시 오늘 드신 식사 사진을 등록하십시오."
                )
            }
        }
        AlarmScheduler.scheduleNextDaily2100(applicationContext)
        return Result.success()
    }
}

object AlarmScheduler {
    private const val WORK_HEALTH_SYNC = "slimtrack_periodic_hc_sync"
    private const val WORK_SUNDAY_0900 = "slimtrack_sunday_0900_advice"
    private const val WORK_DAILY_2100 = "slimtrack_daily_2100_missing_meal"

    fun scheduleAll(context: Context) {
        NotificationHelper.ensureChannels(context)
        val wm = WorkManager.getInstance(context)

        val hcRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(6, TimeUnit.HOURS)
            .build()
        wm.enqueueUniquePeriodicWork(
            WORK_HEALTH_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            hcRequest
        )

        scheduleNextSunday0900(context)
        scheduleNextDaily2100(context)
    }

    fun scheduleNextSunday0900(context: Context, now: LocalDateTime = LocalDateTime.now()) {
        val delay = computeDelayUntilNextSunday0900(now)
        val req = OneTimeWorkRequestBuilder<SundayMorningAdviceWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_SUNDAY_0900,
            androidx.work.ExistingWorkPolicy.REPLACE,
            req
        )
    }

    fun scheduleNextDaily2100(context: Context, now: LocalDateTime = LocalDateTime.now()) {
        val delay = computeDelayUntilNext2100(now)
        val req = OneTimeWorkRequestBuilder<MissingMeal21Worker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_DAILY_2100,
            androidx.work.ExistingWorkPolicy.REPLACE,
            req
        )
    }

    fun computeDelayUntilNextSunday0900(now: LocalDateTime): Duration {
        var candidate = now.toLocalDate()
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .atTime(LocalTime.of(9, 0))
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusWeeks(1)
        }
        return Duration.between(now, candidate)
    }

    fun computeDelayUntilNext2100(now: LocalDateTime): Duration {
        var candidate = now.toLocalDate().atTime(LocalTime.of(21, 0))
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return Duration.between(now, candidate)
    }
}
