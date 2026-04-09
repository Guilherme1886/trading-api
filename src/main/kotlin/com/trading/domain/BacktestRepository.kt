package com.trading.domain

import java.util.UUID

interface BacktestRepository {
    fun save(backtest: Backtest): Backtest
    fun findById(id: UUID): Backtest?
    fun findAll(): List<Backtest>
}
