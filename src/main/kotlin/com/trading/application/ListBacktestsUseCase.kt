package com.trading.application

import com.trading.domain.Backtest
import com.trading.domain.BacktestRepository
import org.springframework.stereotype.Service

@Service
class ListBacktestsUseCase(
    private val repository: BacktestRepository
) {
    fun execute(): List<Backtest> {
        return repository.findAll()
    }
}
