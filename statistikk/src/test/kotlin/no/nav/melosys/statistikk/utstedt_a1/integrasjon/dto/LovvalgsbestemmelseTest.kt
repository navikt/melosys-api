package no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LovvalgsbestemmelseTest {

    @ParameterizedTest
    @MethodSource("gyldigeBestemmelser")
    fun `av gyldigBestemmelse forventNotNull`(bestemmelse: LovvalgBestemmelse) {
        Lovvalgsbestemmelse.av(bestemmelse) shouldNotBe null
    }

    @Test
    fun `av ugyldigBestemmelse forventException`() {
        val ugyldigBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4

        shouldThrow<UnsupportedOperationException> {
            Lovvalgsbestemmelse.av(ugyldigBestemmelse)
        }.message shouldContain "støttes ikke for melding om utstedt A1"
    }

    fun gyldigeBestemmelser() = listOf(
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2
    )
}
