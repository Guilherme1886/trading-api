package com.trading.application

import com.trading.domain.Backtest
import com.trading.domain.BacktestRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CreateBacktestUseCase(
    private val repository: BacktestRepository,
    private val executeBacktest: ExecuteBacktestUseCase
) {
    data class Input(
        val name: String,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    fun execute(input: Input): Backtest {
        require(input.endDate.isAfter(input.startDate)) {
            "endDate must be after startDate"
        }

        val backtest = Backtest(
            name = input.name,
            startDate = input.startDate,
            endDate = input.endDate
        )

        val saved = repository.save(backtest)
        executeBacktest.execute(saved.id)
        return saved
    }
}
