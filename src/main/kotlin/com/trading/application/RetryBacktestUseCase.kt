package com.trading.application

import com.trading.domain.Backtest
import com.trading.domain.BacktestRepository
import com.trading.domain.BacktestStatus
import com.trading.domain.NotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RetryBacktestUseCase(
    private val repository: BacktestRepository,
    private val executeBacktest: ExecuteBacktestUseCase
) {
    fun execute(id: UUID): Backtest {
        val backtest = repository.findById(id)
            ?: throw NotFoundException("Backtest $id not found")

        val running = backtest.transitionTo(BacktestStatus.RUNNING)
        val saved = repository.save(running)
        executeBacktest.execute(saved.id)
        return saved
    }
}
