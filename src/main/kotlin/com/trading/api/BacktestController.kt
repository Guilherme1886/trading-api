package com.trading.api

import com.trading.application.CancelBacktestUseCase
import com.trading.application.CreateBacktestUseCase
import com.trading.application.GetBacktestUseCase
import com.trading.application.ListBacktestsUseCase
import com.trading.application.RetryBacktestUseCase
import com.trading.domain.Backtest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/backtests")
class BacktestController(
    private val createBacktest: CreateBacktestUseCase,
    private val getBacktest: GetBacktestUseCase,
    private val listBacktests: ListBacktestsUseCase,
    private val cancelBacktest: CancelBacktestUseCase,
    private val retryBacktest: RetryBacktestUseCase
) {
    data class CreateBacktestRequest(
        val name: String,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    data class CreateBacktestResponse(
        val id: UUID,
        val status: String
    )

    @PostMapping
    fun create(@RequestBody request: CreateBacktestRequest): ResponseEntity<CreateBacktestResponse> {
        val input = CreateBacktestUseCase.Input(
            name = request.name,
            startDate = request.startDate,
            endDate = request.endDate
        )

        val backtest = createBacktest.execute(input)

        val response = CreateBacktestResponse(
            id = backtest.id,
            status = backtest.status.name
        )

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response)
    }

    data class BacktestResponse(
        val id: UUID,
        val name: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val status: String,
        val result: ResultResponse?,
        val createdAt: Instant
    )

    data class ResultResponse(
        val pnl: BigDecimal
    )

    @GetMapping
    fun list(): ResponseEntity<List<BacktestResponse>> {
        val backtests = listBacktests.execute()
        return ResponseEntity.ok(backtests.map { toResponse(it) })
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<BacktestResponse> {
        val backtest = getBacktest.execute(id)
        return ResponseEntity.ok(toResponse(backtest))
    }

    @DeleteMapping("/{id}")
    fun cancel(@PathVariable id: UUID): ResponseEntity<BacktestResponse> {
        val backtest = cancelBacktest.execute(id)
        return ResponseEntity.ok(toResponse(backtest))
    }

    @PostMapping("/{id}/retry")
    fun retry(@PathVariable id: UUID): ResponseEntity<BacktestResponse> {
        val backtest = retryBacktest.execute(id)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(toResponse(backtest))
    }

    private fun toResponse(backtest: Backtest) = BacktestResponse(
        id = backtest.id,
        name = backtest.name,
        startDate = backtest.startDate,
        endDate = backtest.endDate,
        status = backtest.status.name,
        result = backtest.result?.let { ResultResponse(pnl = it.pnl) },
        createdAt = backtest.createdAt
    )
}
