package no.nav.melosys.tjenester.gui.util

import org.apache.commons.lang3.RandomStringUtils
import org.jeasy.random.api.Randomizer

class NumericStringRandomizer(private val length: Int) : Randomizer<String> {
    override fun getRandomValue(): String = RandomStringUtils.randomNumeric(length)
}