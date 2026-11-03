package com.liangji.bodymeasure.core.common

import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface AppClock {
    fun now(): Instant

    fun today(): LocalDate
}

@Singleton
class SystemAppClock @Inject constructor() : AppClock {
    override fun now(): Instant = Instant.now()

    override fun today(): LocalDate = LocalDate.now()
}
