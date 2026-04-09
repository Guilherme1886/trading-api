package com.trading.application

import com.trading.domain.Backtest
import com.trading.domain.BacktestRepository
import com.trading.domain.BacktestStatus
import com.trading.domain.NotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CancelBacktestUseCase(
    private val repository: BacktestRepository
) {
    fun execute(id: UUID): Backtest {
        val backtest = repository.findById(id)
            ?: throw NotFoundException("Backtest $id not found")

        val canceled = backtest.transitionTo(BacktestStatus.CANCELED)
        return repository.save(canceled)
    }
}
