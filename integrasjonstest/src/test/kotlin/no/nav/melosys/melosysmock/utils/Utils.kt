package no.nav.melosys.melosysmock.utils

import kotlin.random.Random

fun lagRandomId() = lagRandomIntId().toString()

fun lagRandomIntId(): Int = Random.nextInt(1000, 1000000)

fun lagRandomLongId(): Long = lagRandomIntId().toLong()
