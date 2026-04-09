package com.trading.domain

enum class BacktestStatus {
    PENDING, RUNNING, COMPLETED, FAILED, CANCELED;

    fun canTransitionTo(target: BacktestStatus): Boolean = when (this) {
        PENDING   -> target == RUNNING
        RUNNING   -> target == COMPLETED || target == FAILED || target == CANCELED
        COMPLETED -> false
        FAILED    -> target == RUNNING
        CANCELED  -> false
    }
}
