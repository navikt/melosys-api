package no.nav.melosys.service.sak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Bostedsland
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.arkiv.DokumentReferanse
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class VideresendSoknadServiceKtTest {

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var eessiService: EessiService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var joarkFasade: JoarkFasade

    @RelaxedMockK
    lateinit var landvelgerService: LandvelgerService

    @RelaxedMockK
    lateinit var oppgaveService: OppgaveService

    @RelaxedMockK
    lateinit var persondataFasade: PersondataFasade

    @RelaxedMockK
    lateinit var prosessinstansService: ProsessinstansService

    private lateinit var videresendSoknadService: VideresendSoknadService
    private lateinit var bostedsland: Bostedsland
    private lateinit var fagsak: Fagsak
    private lateinit var mottatteOpplysningerData: MottatteOpplysningerData
    private lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        videresendSoknadService = VideresendSoknadService(
            eessiService,
            fagsakService,
            behandlingsresultatService,
            joarkFasade,
            landvelgerService,
            oppgaveService,
            persondataFasade,
            prosessinstansService
        )

        bostedsland = Bostedsland(Landkoder.ES)
        fagsak = SaksbehandlingDataFactory.lagFagsak()
        mottatteOpplysningerData = MottatteOpplysningerData()
        behandling = SaksbehandlingDataFactory.lagBehandling(mottatteOpplysningerData)

        behandling.fagsak = fagsak
        fagsak.leggTilBehandling(behandling)

        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
    }

    @Test
    fun `henlegg og videresend bostedsland spania er søknad prosessinstans blir opprettet`() {
        behandling.tema = Behandlingstema.ARBEID_FLERE_LAND
        val validerteMottakere = setOf("ES:mottakerID123")
        every { landvelgerService.hentBostedsland(behandling) } returns bostedsland
        every {
            eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
                any(),
                eq(listOf(Land_iso2.ES)),
                eq(BucType.LA_BUC_03)
            )
        } returns validerteMottakere
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        val dokumentReferanse = DokumentReferanse("jpID", "dokID")

        videresendSoknadService.videresend(SAKSNUMMER, "", "fritekst", setOf(dokumentReferanse))

        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.VIDERESENDT) }
        verify {
            prosessinstansService.opprettProsessinstansVideresendSoknad(
                behandling,
                validerteMottakere.first(),
                "fritekst",
                setOf(dokumentReferanse)
            )
        }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(behandling.id) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.id, Behandlingsresultattyper.HENLEGGELSE) }
    }

    @Test
    fun `henlegg og videresend ikke søknad kaster exception`() {
        every { landvelgerService.hentBostedsland(behandling) } returns bostedsland
        behandling.tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING

        val exception = shouldThrow<FunksjonellException> {
            videresendSoknadService.videresend(SAKSNUMMER, "", "", emptySet())
        }
        exception.message shouldContain "har ikke behandlingstema 'ARBEID_FLERE_LAND' og kan ikke videresendes"
    }

    @Test
    fun `henlegg og videresend bostedsland norge er søknad kaster exception`() {
        every { landvelgerService.hentBostedsland(behandling) } returns Bostedsland(Landkoder.NO)
        behandling.tema = Behandlingstema.ARBEID_FLERE_LAND

        val exception = shouldThrow<FunksjonellException> {
            videresendSoknadService.videresend(SAKSNUMMER, "", "", emptySet())
        }
        exception.message shouldContain "til Norge"
    }

    @Test
    fun `henlegg og videresend bostedsland ikke avklart er søknad kaster exception`() {
        every { landvelgerService.hentBostedsland(behandling) } returns null
        behandling.tema = Behandlingstema.ARBEID_FLERE_LAND

        val exception = shouldThrow<FunksjonellException> {
            videresendSoknadService.videresend(SAKSNUMMER, "", "", emptySet())
        }
        exception.message shouldContain "Bostedsland ikke avklart"
    }

    @Test
    fun `henlegg og videresend ingen adresse kaster exception`() {
        every { landvelgerService.hentBostedsland(behandling) } returns bostedsland
        behandling.tema = Behandlingstema.ARBEID_FLERE_LAND
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()

        val exception = shouldThrow<FunksjonellException> {
            videresendSoknadService.videresend(SAKSNUMMER, "", "", emptySet())
        }
        exception.message shouldContain "mangler adresse"
    }

    companion object {
        private const val SAKSNUMMER = "MEL-123"
    }
}
