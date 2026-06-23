package no.nav.melosys.saksflytapi.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ProsessPrioritetTest {

    @Test
    fun `høyesteAv velger høyeste prioritet uavhengig av rekkefølge`() {
        (ProsessPrioritet.NORMAL høyesteAv ProsessPrioritet.HØY) shouldBe ProsessPrioritet.HØY
        (ProsessPrioritet.HØY høyesteAv ProsessPrioritet.NORMAL) shouldBe ProsessPrioritet.HØY
        (ProsessPrioritet.LAV høyesteAv ProsessPrioritet.NORMAL) shouldBe ProsessPrioritet.NORMAL
        (ProsessPrioritet.NORMAL høyesteAv ProsessPrioritet.LAV) shouldBe ProsessPrioritet.NORMAL
    }

    @Test
    fun `høyesteAv av lik prioritet returnerer samme prioritet`() {
        (ProsessPrioritet.LAV høyesteAv ProsessPrioritet.LAV) shouldBe ProsessPrioritet.LAV
        (ProsessPrioritet.HØY høyesteAv ProsessPrioritet.HØY) shouldBe ProsessPrioritet.HØY
    }
}
