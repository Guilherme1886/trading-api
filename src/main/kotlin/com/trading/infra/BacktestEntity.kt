package com.trading.infra

import com.trading.domain.Backtest
import com.trading.domain.BacktestResult
import com.trading.domain.BacktestStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "backtests")
class BacktestEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    var name: String = "",
    var startDate: LocalDate = LocalDate.now(),
    var endDate: LocalDate = LocalDate.now(),
    @Enumerated(EnumType.STRING)
    var status: BacktestStatus = BacktestStatus.PENDING,
    var pnl: BigDecimal? = null,
    var createdAt: Instant = Instant.now()
) {
    fun toDomain(): Backtest = Backtest(
        id = id,
        name = name,
        startDate = startDate,
        endDate = endDate,
        status = status,
        result = pnl?.let { BacktestResult(pnl = it) },
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(backtest: Backtest): BacktestEntity = BacktestEntity(
            id = backtest.id,
            name = backtest.name,
            startDate = backtest.startDate,
            endDate = backtest.endDate,
            status = backtest.status,
            pnl = backtest.result?.pnl,
            createdAt = backtest.createdAt
        )
    }
}
