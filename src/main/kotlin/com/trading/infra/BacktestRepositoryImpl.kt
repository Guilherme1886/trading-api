package com.trading.infra

import com.trading.domain.Backtest
import com.trading.domain.BacktestRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class BacktestRepositoryImpl(
    private val jpa: JpaBacktestRepository
) : BacktestRepository {

    override fun save(backtest: Backtest): Backtest {
        val entity = BacktestEntity.fromDomain(backtest)
        return jpa.save(entity).toDomain()
    }

    override fun findById(id: UUID): Backtest? {
        return jpa.findById(id).orElse(null)?.toDomain()
    }

    override fun findAll(): List<Backtest> {
        return jpa.findAll().map { it.toDomain() }
    }
}
