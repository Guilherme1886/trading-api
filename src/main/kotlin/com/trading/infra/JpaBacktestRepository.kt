package com.trading.infra

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaBacktestRepository : JpaRepository<BacktestEntity, UUID>
