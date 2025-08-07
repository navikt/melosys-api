package no.nav.melosys.service.dokument.brev

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.brev.NorskMyndighet
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ATTEST_A1
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.UtenlandskMyndighetRepository
import no.nav.melosys.service.bruker.SaksbehandlerService
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class BrevDataServiceKtTest {

    @MockK
    private lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @MockK
    private lateinit var saksbehandlerService: SaksbehandlerService

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    @MockK
    private lateinit var utenlandskMyndighetRepository: UtenlandskMyndighetRepository

    private lateinit var service: BrevDataService

    companion object {
        private const val FNR = "Fnr"
        private const val ORGNR = "Org-Nr"
        private const val INSTITUSJON_ID = "HR:Zxcd"
        private const val sammensattNavn = "ALTFOR SAMMENSATT"
    }

    @BeforeEach
    fun setUp() {
        service = spyk(
            BrevDataService(
                behandlingsresultatRepository,
                persondataFasade,
                saksbehandlerService,
                utenlandskMyndighetRepository
            )
        )

        every { behandlingsresultatRepository.findById(any()) } returns Optional.of(Behandlingsresultat())
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Joe Moe"
        every { persondataFasade.hentFolkeregisterident(any()) } returns FNR
        every { persondataFasade.hentSammensattNavn(any()) } returns sammensattNavn
        lagUtenlandskMyndighet()
    }

    private fun lagUtenlandskMyndighet(): UtenlandskMyndighet {
        val myndighet = UtenlandskMyndighet().apply {
            navn = "navn"
            gateadresse1 = "gateadresse 123"
            gateadresse2 = "institusjon ABC"
            land = "HR"
        }
        every { utenlandskMyndighetRepository.findByLandkode(Land_iso2.HR) } returns Optional.of(myndighet)
        return myndighet
    }

    private fun lagBehandling(soeknad: Soeknad): Behandling {
        return mockk<Behandling>(relaxed = true) {
            every { fagsak } returns FagsakTestFactory.lagFagsak()
            every { mottatteOpplysninger } returns soeknad
        }
    }

    private fun lagSøknadDokument(): Soeknad {
        return Soeknad()
    }

    private fun lagAktoerMyndighet(): Aktoer {
        return Aktoer().apply {
            rolle = Aktoersroller.UTENLANDSK_MYNDIGHET
            institusjonID = INSTITUSJON_ID
        }
    }

    private fun lagMottakerMyndighet(): Mottaker {
        return Mottaker.medRolle(no.nav.melosys.domain.kodeverk.Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET).apply {
            institusjonID = INSTITUSJON_ID
        }
    }

    @Test
    fun `lag A1 til utenlandsk myndighet`() {
        val behandling = lagBehandling(lagSøknadDokument())
        val aktoerMyndighet = lagAktoerMyndighet()
        behandling.fagsak.leggTilAktør(aktoerMyndighet)
        val brevData = BrevDataVedlegg("Z123456")
        val myndighet = lagUtenlandskMyndighet()
        val mottakerMyndighet = lagMottakerMyndighet()
        
        val metadata = service.lagBestillingMetadata(
            ATTEST_A1,
            mottakerMyndighet,
            null,
            behandling,
            brevData
        )

        metadata.brukerID shouldBe FNR
        metadata.mottakerID shouldBe INSTITUSJON_ID
        metadata.utenlandskMyndighet shouldBe myndighet
        metadata.brukerNavn shouldBe sammensattNavn

        val element = service.lagBrevXML(ATTEST_A1, mottakerMyndighet, null, behandling, brevData)

        element.shouldNotBeNull()
    }

    @Test
    fun `lag BrevXML til norsk myndighet`() {
        val behandling = lagBehandling(lagSøknadDokument())
        val brevData = BrevDataVedlegg("Z123456")
        val mottakerNorskMyndighet = Mottaker.av(NorskMyndighet.SKATTEETATEN)
        
        val metadata = service.lagBestillingMetadata(
            ATTEST_A1,
            mottakerNorskMyndighet,
            null,
            behandling,
            brevData
        )

        metadata.mottakerID shouldBe mottakerNorskMyndighet.orgnr

        val element = service.lagBrevXML(ATTEST_A1, mottakerNorskMyndighet, null, behandling, brevData)

        element.shouldNotBeNull()
    }
}