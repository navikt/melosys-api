package no.nav.melosys.service.dokument.brev.datagrunnlag

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagForetakUtland
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagNorskVirksomhet
import no.nav.melosys.service.kodeverk.KodeverkService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvklarteVirksomheterGrunnlagKtTest {

    @MockK
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    @MockK
    private lateinit var kodeverkService: KodeverkService

    private lateinit var dataGrunnlag: AvklarteVirksomheterGrunnlag

    @BeforeEach
    fun setUp() {
        dataGrunnlag = AvklarteVirksomheterGrunnlag(mockk<Behandling>(), avklarteVirksomheterService)
    }

    @Test
    fun `hentAlleNorskeVirksomheter forventer en virksomhet`() {
        every { avklarteVirksomheterService.hentAlleNorskeVirksomheter(any()) } returns listOf(lagNorskVirksomhet())
        
        val norskeVirksomheter = dataGrunnlag.hentAlleNorskeVirksomheterMedAdresse()
        norskeVirksomheter shouldHaveSize 1
        
        dataGrunnlag.hentAlleNorskeVirksomheterMedAdresse()
        verify(exactly = 1) { avklarteVirksomheterService.hentAlleNorskeVirksomheter(any()) }
    }

    @Test
    fun `hentUtenlandskeArbeidsgivere med utenlandsk arbeidsgiver og selvstendig henter kun arbeidsgivere`() {
        val utenlandskSelvstendigForetak = AvklartVirksomhet(lagForetakUtland(true))
        val utenlandskArbeidsgiver = AvklartVirksomhet(lagForetakUtland(false))

        val utenlandskeVirksomheter = listOf(utenlandskSelvstendigForetak, utenlandskArbeidsgiver)
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns utenlandskeVirksomheter

        val utenlandskeArbeidsgivere = dataGrunnlag.hentUtenlandskeArbeidsgivere()
        utenlandskeArbeidsgivere shouldContainExactly listOf(utenlandskArbeidsgiver)
    }

    @Test
    fun `hentUtenlandskeSelvstendige med utenlandsk arbeidsgiver og selvstendig henter kun selvstendige`() {
        val utenlandskSelvstendigForetak = AvklartVirksomhet(lagForetakUtland(true))
        val utenlandskArbeidsgiver = AvklartVirksomhet(lagForetakUtland(false))

        val utenlandskeVirksomheter = listOf(utenlandskSelvstendigForetak, utenlandskArbeidsgiver)
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns utenlandskeVirksomheter

        val utenlandskeSelvstendige = dataGrunnlag.hentUtenlandskeSelvstendige()
        utenlandskeSelvstendige shouldContainExactly listOf(utenlandskSelvstendigForetak)
    }

    @Test
    fun `hentHovedvirksomhet med en norsk virksomhet gir norsk hovedvirksomhet`() {
        val norskVirksomhet = lagNorskVirksomhet()
        every { avklarteVirksomheterService.hentAlleNorskeVirksomheter(any()) } returns listOf(norskVirksomhet)
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns emptyList()

        val avklartVirksomhet = dataGrunnlag.hentHovedvirksomhet()
        avklartVirksomhet shouldBe norskVirksomhet
    }

    @Test
    fun `hentHovedvirksomhet med norsk og utenlandsk virksomhet gir norsk hovedvirksomhet`() {
        val norskVirksomhet = lagNorskVirksomhet()
        every { avklarteVirksomheterService.hentAlleNorskeVirksomheter(any()) } returns listOf(norskVirksomhet)

        val utenlandskAvklartVirksomhet = AvklartVirksomhet(lagForetakUtland(false))
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns listOf(utenlandskAvklartVirksomhet)

        val hovedvirksomhet = dataGrunnlag.hentHovedvirksomhet()
        dataGrunnlag.hentBivirksomheter()
        hovedvirksomhet shouldBe norskVirksomhet
    }

    @Test
    fun `hentHovedvirksomhet med kun utenlandsk virksomhet gir utenlandsk virksomhet`() {
        val forventetUtenlandskVirksomhet = AvklartVirksomhet(lagForetakUtland(false))
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns listOf(forventetUtenlandskVirksomhet)
        every { avklarteVirksomheterService.hentAlleNorskeVirksomheter(any()) } returns emptyList()

        val hovedvirksomhet = dataGrunnlag.hentHovedvirksomhet()
        
        // Compare the fields that matter - using specific field comparison
        hovedvirksomhet.navn shouldBe forventetUtenlandskVirksomhet.navn
        hovedvirksomhet.orgnr shouldBe forventetUtenlandskVirksomhet.orgnr
        hovedvirksomhet.adresse shouldBe forventetUtenlandskVirksomhet.adresse
        hovedvirksomhet.yrkesaktivitet shouldBe forventetUtenlandskVirksomhet.yrkesaktivitet
    }

    @Test
    fun `hentBivirksomheter med en norsk virksomhet gir ingen bivirksomheter`() {
        val norskVirksomhet = lagNorskVirksomhet()
        every { avklarteVirksomheterService.hentAlleNorskeVirksomheter(any()) } returns listOf(norskVirksomhet)
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns emptyList()

        val bivirksomheter = dataGrunnlag.hentBivirksomheter()
        bivirksomheter.shouldBeEmpty()
    }

    @Test
    fun `hentBivirksomheter med en utenlandsk virksomhet gir ingen bivirksomheter`() {
        every { avklarteVirksomheterService.hentAlleNorskeVirksomheter(any()) } returns emptyList()

        val forventetUtenlandskVirksomhet = AvklartVirksomhet(lagForetakUtland(false))
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns listOf(forventetUtenlandskVirksomhet)

        val bivirksomheter = dataGrunnlag.hentBivirksomheter()
        bivirksomheter.shouldBeEmpty()
    }

    @Test
    fun `hentBivirksomheter med to norske virksomheter gir en norsk bivirksomhet`() {
        val norskVirksomhet = lagNorskVirksomhet()
        every { avklarteVirksomheterService.hentAlleNorskeVirksomheter(any()) } returns listOf(norskVirksomhet, norskVirksomhet)
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns emptyList()

        val bivirksomheter = dataGrunnlag.hentBivirksomheter()
        bivirksomheter shouldContainExactly listOf(norskVirksomhet)
    }

    @Test
    fun `hentHovedvirksomhet med norsk og utenlandsk virksomhet gir utenlandsk bivirksomhet`() {
        val forventetUtenlandskVirksomhet = AvklartVirksomhet(lagForetakUtland(false))

        val norskVirksomhet = lagNorskVirksomhet()
        every { avklarteVirksomheterService.hentAlleNorskeVirksomheter(any()) } returns listOf(norskVirksomhet)
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns listOf(forventetUtenlandskVirksomhet)

        val bivirksomheter = dataGrunnlag.hentBivirksomheter()
        bivirksomheter shouldHaveSize 1

        val bivirksomhet = bivirksomheter.iterator().next()
        bivirksomhet.navn shouldBe forventetUtenlandskVirksomhet.navn
        bivirksomhet.orgnr shouldBe forventetUtenlandskVirksomhet.orgnr
        bivirksomhet.adresse shouldBe forventetUtenlandskVirksomhet.adresse
        bivirksomhet.yrkesaktivitet shouldBe forventetUtenlandskVirksomhet.yrkesaktivitet
    }
}