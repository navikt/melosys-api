package no.nav.melosys.service.brev

import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.service.brev.bestilling.HentBrevAdresseTilMottakereService
import no.nav.melosys.service.brev.bestilling.HentMuligeProduserbaredokumenterService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BrevmalListeServiceTest {

    @MockK
    private lateinit var hentMuligeProduserbaredokumenterService: HentMuligeProduserbaredokumenterService

    @MockK
    private lateinit var hentBrevAdresseTilMottakereService: HentBrevAdresseTilMottakereService

    private lateinit var brevmalListeService: BrevmalListeService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        brevmalListeService = BrevmalListeService(
            hentMuligeProduserbaredokumenterService,
            hentBrevAdresseTilMottakereService
        )
    }

    @Test
    fun `hentMuligeProduserbaredokumenter skal returnere liste fra service`() {
        val behandlingId = 123L
        val rolle = Mottakerroller.BRUKER
        val forventetListe = listOf(
            Produserbaredokumenter.MANGELBREV_BRUKER,
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER
        )
        every { hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(behandlingId, rolle) } returns forventetListe


        val resultat = brevmalListeService.hentMuligeProduserbaredokumenter(behandlingId, rolle)


        resultat shouldBe forventetListe
        verify { hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(behandlingId, rolle) }
    }

    @Test
    fun `hentBrevAdresseTilMottakere skal returnere liste fra service`() {
        val behandlingId = 123L
        val rolle = Mottakerroller.BRUKER
        val forventetListe = listOf(
            BrevAdresse("Test Person", null, listOf("Testveien 1"), "0123", "Oslo", null, "NO"),
            BrevAdresse("Test Bedrift AS", "123456789", listOf("Bedriftsveien 2"), "0456", "Bergen", null, "NO")
        )
        every { hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(behandlingId, rolle) } returns forventetListe


        val resultat = brevmalListeService.hentBrevAdresseTilMottakere(behandlingId, rolle)


        resultat shouldBe forventetListe
        verify { hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(behandlingId, rolle) }
    }

    @Test
    fun `hentMuligeProduserbaredokumenter med arbeidsgiver rolle skal returnere korrekt liste`() {
        val behandlingId = 123L
        val rolle = Mottakerroller.ARBEIDSGIVER
        val forventetListe = listOf(
            Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER,
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER
        )
        every { hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(behandlingId, rolle) } returns forventetListe


        val resultat = brevmalListeService.hentMuligeProduserbaredokumenter(behandlingId, rolle)


        resultat shouldBe forventetListe
        verify { hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(behandlingId, rolle) }
    }

    @Test
    fun `hentBrevAdresseTilMottakere med arbeidsgiver rolle skal returnere korrekt liste`() {
        val behandlingId = 123L
        val rolle = Mottakerroller.ARBEIDSGIVER
        val forventetListe = listOf(
            BrevAdresse("Arbeidsgiver AS", "987654321", listOf("Arbeidsgiverveien 1"), "0789", "Trondheim", null, "NO")
        )
        every { hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(behandlingId, rolle) } returns forventetListe


        val resultat = brevmalListeService.hentBrevAdresseTilMottakere(behandlingId, rolle)


        resultat shouldBe forventetListe
        verify { hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(behandlingId, rolle) }
    }
}
