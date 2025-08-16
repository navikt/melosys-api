package no.nav.melosys.service.persondata.familie

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.integrasjon.pdl.PDLConsumer
import no.nav.melosys.service.persondata.familie.FamiliemedlemObjectFactory.*
import no.nav.melosys.service.persondata.familie.medlem.EktefelleEllerPartnerFamiliemedlemFilter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class EktefelleEllerPartnerFamiliemedlemFilterTest {

    @MockK
    private lateinit var pdlConsumer: PDLConsumer

    @InjectMockKs
    private lateinit var ektefelleEllerPartnerFamiliemedlemFilter: EktefelleEllerPartnerFamiliemedlemFilter

    @Test
    fun `hentEktefelleEllerPartnerFraSivilstander fårGiftSivilstandTilbake`() {
        every { pdlConsumer.hentEktefelleEllerPartner(IDENT_PERSON_GIFT) } returns lagPersonGift()
        val sivilstandTilHovedperson = lagSivilstandForHovedperson()


        val result = ektefelleEllerPartnerFamiliemedlemFilter.hentEktefelleEllerPartnerFraSivilstander(
            sivilstandTilHovedperson
        )


        result shouldHaveSize 1
        val sivilstand = result.first()
        sivilstand.run {
            erRelatertVedSivilstand() shouldBe true
            navn().fornavn() shouldBe PERSON_GIFT_FORNAVN
        }
    }
}
