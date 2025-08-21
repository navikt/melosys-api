package no.nav.melosys.domain.person

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class NavnTest {

    @Test
    fun navnEtternavnFørst() {
        Navn.navnEtternavnFørst(NAVN_ETTERNAVN_SIST) shouldBe NAVN_ETTERNAVN_FØRST
    }

    @Test
    fun navnEtternavnSist() {
        Navn.navnEtternavnSist(NAVN_ETTERNAVN_FØRST) shouldBe NAVN_ETTERNAVN_SIST
    }

    @Test
    fun riktigFormatNavnKommerUtRiktig() {
        Navn.navnEtternavnSist(NAVN_ETTERNAVN_SIST) shouldBe NAVN_ETTERNAVN_SIST
    }

    companion object {
        private const val NAVN_ETTERNAVN_SIST = "Per Olav Mellomnavn Etternavn"
        private const val NAVN_ETTERNAVN_FØRST = "Etternavn, Per Olav Mellomnavn"
    }
}