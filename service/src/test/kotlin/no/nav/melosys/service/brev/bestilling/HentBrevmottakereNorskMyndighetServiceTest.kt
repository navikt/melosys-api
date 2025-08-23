package no.nav.melosys.service.brev.bestilling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.brev.NorskMyndighet.HELFO
import no.nav.melosys.domain.brev.NorskMyndighet.SKATTEETATEN
import no.nav.melosys.domain.kodeverk.Mottakerroller.NORSK_MYNDIGHET
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.FRITEKSTBREV
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.ereg.EregFasade
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class HentBrevmottakereNorskMyndighetServiceTest {

    @RelaxedMockK
    lateinit var eregFasade: EregFasade

    @InjectMockKs
    lateinit var hentBrevmottakereNorskMyndighetService: HentBrevmottakereNorskMyndighetService

    @Test
    fun `hentMuligeBrevmottakereNorskMyndighet skal returnere Skatteetaten og Helfo mottakere når spurt etter begge`() {
        every { eregFasade.hentOrganisasjonNavn(SKATTEETATEN.orgnr) } returns "Skatteetaten"
        every { eregFasade.hentOrganisasjonNavn(HELFO.orgnr) } returns "Helfo"
        val orgnrNorskeMyndigheter = listOf(SKATTEETATEN.orgnr, HELFO.orgnr)


        val muligeBrevmottakereForNorskMyndighet =
            hentBrevmottakereNorskMyndighetService.hentMuligeBrevmottakereNorskMyndighet(orgnrNorskeMyndigheter)


        muligeBrevmottakereForNorskMyndighet shouldHaveSize 2
        muligeBrevmottakereForNorskMyndighet.first().run {
            rolle shouldBe NORSK_MYNDIGHET
            dokumentNavn shouldBe FRITEKSTBREV.beskrivelse
            orgnr shouldBe SKATTEETATEN.orgnr
            mottakerNavn shouldBe "Skatteetaten"
        }
        muligeBrevmottakereForNorskMyndighet.last().run {
            rolle shouldBe NORSK_MYNDIGHET
            dokumentNavn shouldBe FRITEKSTBREV.beskrivelse
            orgnr shouldBe HELFO.orgnr
            mottakerNavn shouldBe "Helfo"
        }
    }

    @Test
    fun `hentMuligeBrevmottakereNorskMyndighet skal kaste IkkeFunnetException når orgnr ikke finnes`() {
        every { eregFasade.hentOrganisasjonNavn("111111111") } throws IkkeFunnetException("Fant ikke orgnr i testen :)")
        val orgnrNorskeMyndigheter = listOf("111111111")


        shouldThrow<IkkeFunnetException> {
            hentBrevmottakereNorskMyndighetService.hentMuligeBrevmottakereNorskMyndighet(orgnrNorskeMyndigheter)
        }
    }
}
