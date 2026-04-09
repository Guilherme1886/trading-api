package com.trading.application

import com.trading.domain.BacktestRepository
import com.trading.domain.BacktestResult
import com.trading.domain.BacktestStatus
import com.trading.domain.NotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class ExecuteBacktestUseCase(
    private val repository: BacktestRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun execute(id: UUID) {
        scope.launch {
            try {
                val backtest = repository.findById(id)
                    ?: throw NotFoundException("Backtest $id not found")

                log.info("Starting backtest {}", id)
                val running = if (backtest.status == BacktestStatus.RUNNING) {
                    backtest
                } else {
                    val transitioned = backtest.transitionTo(BacktestStatus.RUNNING)
                    repository.save(transitioned)
                }

                delay(5_000)

                val fakeResult = BacktestResult(pnl = BigDecimal("1542.37"))
                val completed = running
                    .transitionTo(BacktestStatus.COMPLETED)
                    .copy(result = fakeResult)
                repository.save(completed)
                log.info("Backtest {} completed with pnl={}", id, fakeResult.pnl)
            } catch (e: Exception) {
                log.error("Backtest {} failed", id, e)
                try {
                    val current = repository.findById(id) ?: return@launch
                    val failed = current.transitionTo(BacktestStatus.FAILED)
                    repository.save(failed)
                } catch (inner: Exception) {
                    log.error("Could not transition backtest {} to FAILED", id, inner)
                }
            }
        }
    }
}
