package com.trading.application

import com.trading.domain.Backtest
import com.trading.domain.BacktestRepository
import com.trading.domain.NotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GetBacktestUseCase(
    private val repository: BacktestRepository
) {
    fun execute(id: UUID): Backtest {
        return repository.findById(id)
            ?: throw NotFoundException("Backtest $id not found")
    }
}
