package com.trading.domain

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Backtest(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: BacktestStatus = BacktestStatus.PENDING,
    val result: BacktestResult? = null,
    val createdAt: Instant = Instant.now()
) {
    fun transitionTo(newStatus: BacktestStatus): Backtest {
        check(status.canTransitionTo(newStatus)) {
            "Cannot transition from $status to $newStatus"
        }
        return copy(status = newStatus)
    }
}
