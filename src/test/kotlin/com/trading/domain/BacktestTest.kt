package com.trading.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.test.assertEquals

class BacktestTest {

    private fun aBacktest(status: BacktestStatus = BacktestStatus.PENDING) = Backtest(
        name = "Test",
        startDate = LocalDate.of(2025, 1, 1),
        endDate = LocalDate.of(2025, 12, 31),
        status = status
    )

    @Test
    fun `PENDING can transition to RUNNING`() {
        val backtest = aBacktest(BacktestStatus.PENDING)
        val result = backtest.transitionTo(BacktestStatus.RUNNING)
        assertEquals(BacktestStatus.RUNNING, result.status)
    }

    @Test
    fun `RUNNING can transition to COMPLETED`() {
        val backtest = aBacktest(BacktestStatus.RUNNING)
        val result = backtest.transitionTo(BacktestStatus.COMPLETED)
        assertEquals(BacktestStatus.COMPLETED, result.status)
    }

    @Test
    fun `RUNNING can transition to FAILED`() {
        val backtest = aBacktest(BacktestStatus.RUNNING)
        val result = backtest.transitionTo(BacktestStatus.FAILED)
        assertEquals(BacktestStatus.FAILED, result.status)
    }

    @Test
    fun `PENDING cannot transition to COMPLETED`() {
        val backtest = aBacktest(BacktestStatus.PENDING)
        val ex = assertThrows<IllegalStateException> {
            backtest.transitionTo(BacktestStatus.COMPLETED)
        }
        assertEquals("Cannot transition from PENDING to COMPLETED", ex.message)
    }

    @Test
    fun `RUNNING can transition to CANCELED`() {
        val backtest = aBacktest(BacktestStatus.RUNNING)
        val result = backtest.transitionTo(BacktestStatus.CANCELED)
        assertEquals(BacktestStatus.CANCELED, result.status)
    }

    @Test
    fun `PENDING cannot transition to CANCELED`() {
        val backtest = aBacktest(BacktestStatus.PENDING)
        val ex = assertThrows<IllegalStateException> {
            backtest.transitionTo(BacktestStatus.CANCELED)
        }
        assertEquals("Cannot transition from PENDING to CANCELED", ex.message)
    }

    @Test
    fun `FAILED can transition to RUNNING`() {
        val backtest = aBacktest(BacktestStatus.FAILED)
        val result = backtest.transitionTo(BacktestStatus.RUNNING)
        assertEquals(BacktestStatus.RUNNING, result.status)
    }

    @Test
    fun `COMPLETED cannot transition to any status`() {
        val backtest = aBacktest(BacktestStatus.COMPLETED)
        assertThrows<IllegalStateException> { backtest.transitionTo(BacktestStatus.RUNNING) }
        assertThrows<IllegalStateException> { backtest.transitionTo(BacktestStatus.FAILED) }
        assertThrows<IllegalStateException> { backtest.transitionTo(BacktestStatus.PENDING) }
        assertThrows<IllegalStateException> { backtest.transitionTo(BacktestStatus.CANCELED) }
    }

    @Test
    fun `FAILED cannot transition to COMPLETED or PENDING`() {
        val backtest = aBacktest(BacktestStatus.FAILED)
        assertThrows<IllegalStateException> { backtest.transitionTo(BacktestStatus.COMPLETED) }
        assertThrows<IllegalStateException> { backtest.transitionTo(BacktestStatus.PENDING) }
        assertThrows<IllegalStateException> { backtest.transitionTo(BacktestStatus.CANCELED) }
    }

    @Test
    fun `CANCELED cannot transition to any status`() {
        val backtest = aBacktest(BacktestStatus.CANCELED)
        assertThrows<IllegalStateException> { backtest.transitionTo(BacktestStatus.RUNNING) }
        assertThrows<IllegalStateException> { backtest.transitionTo(BacktestStatus.COMPLETED) }
        assertThrows<IllegalStateException> { backtest.transitionTo(BacktestStatus.FAILED) }
        assertThrows<IllegalStateException> { backtest.transitionTo(BacktestStatus.PENDING) }
    }
}
