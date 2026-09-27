package app.menosan.android.data.repo

import app.menosan.android.core.time.WeekCalc
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

fun currentWeekStartFlow(clock: Clock, maxTick: Duration = Duration.ofMinutes(1)): Flow<LocalDate> = flow {
    while (true) {
        val week = WeekCalc.currentWeekStart(clock)
        emit(week)
        val untilRollover = Duration.between(clock.instant(), WeekCalc.endExclusive(week)).toMillis()
        delay(untilRollover.coerceIn(1L, maxTick.toMillis()))
    }
}.distinctUntilChanged()
