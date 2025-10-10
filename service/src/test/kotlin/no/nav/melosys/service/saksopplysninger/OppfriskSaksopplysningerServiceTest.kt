package no.nav.melosys.service.saksopplysninger

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.kontroll.feature.ufm.UfmKontrollService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import no.nav.melosys.service.vilkaar.InngangsvilkaarService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class OppfriskSaksopplysningerServiceTest {

    private val anmodningsperiodeService = mockk<AnmodningsperiodeService>()
    private val behandlingService = mockk<BehandlingService>()
    private val behandlingsresultatService = mockk<BehandlingsresultatService>()
    private val ufmKontrollService = mockk<UfmKontrollService>()
    private val inngangsvilkaarService = mockk<InngangsvilkaarService>()
    private val registeropplysningerService = mockk<RegisteropplysningerService>()
    private val persondataFasade = mockk<PersondataFasade>()
    private val registeropplysningerFactory = mockk<RegisteropplysningerFactory>()
    private val årsavregningService = mockk<ÅrsavregningService>()
    private val helseutgiftDekkesPeriodeService = mockk<HelseutgiftDekkesPeriodeService>()

    private lateinit var oppfriskSaksopplysningerService: OppfriskSaksopplysningerService

    private val behandlingId = 1234L
    private val aktørId = "12345678910"
    private val fnr = "11223344556"

    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun setUp() {
        clearAllMocks()

        fagsak = Fagsak(
            saksnummer = "test",
            status = Saksstatuser.OPPRETTET,
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG,
            type = Sakstyper.FTRL
        )

        val behandling = Behandling.forTest {
            id = behandlingId
            this.fagsak = this@OppfriskSaksopplysningerServiceTest.fagsak
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        }

        this.behandling = spyk(behandling)
        fagsak = spyk(fagsak)

        every { this@OppfriskSaksopplysningerServiceTest.behandling.fagsak } returns fagsak

        oppfriskSaksopplysningerService = OppfriskSaksopplysningerService(
            anmodningsperiodeService,
            behandlingService,
            behandlingsresultatService,
            ufmKontrollService,
            inngangsvilkaarService,
            registeropplysningerService,
            persondataFasade,
            registeropplysningerFactory,
            årsavregningService,
            helseutgiftDekkesPeriodeService
        )

        every { behandlingService.hentBehandling(behandlingId) } returns this.behandling
        every { persondataFasade.hentFolkeregisterident(any()) } returns fnr
    }

    @Test
    fun `skal kaste exception når anmodning om unntak er sendt`() {
        every { behandling.erUtsending() } returns true
        every { anmodningsperiodeService.harSendtAnmodningsperiode(behandlingId) } returns true

        shouldThrow<FunksjonellException> {
            oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(behandlingId, false)
        }.message shouldBe "Anmodning om unntak er sendt for behandling 1234. Det er ikke lenger mulig å endre mottatteOpplysninger og saksopplysninger"

        verify { anmodningsperiodeService.harSendtAnmodningsperiode(behandlingId) }
        verify { behandling.erUtsending() }
    }

    @Test
    fun `skal oppdatere registeropplysninger og slette behandlingsresultat for oppfriskSaksopplysning`() {
        every { behandling.erUtsending() } returns false
        every { behandling.erBehandlingAvSed() } returns false
        every { behandling.erÅrsavregning() } returns false
        every { behandling.finnPeriode() } returns Optional.of(Periode())
        every { fagsak.finnBrukersAktørID() } returns aktørId

        every { inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) } returns false

        every { registeropplysningerFactory.utledSaksopplysningTyper(
            any(), any(), any(), any()
        ) } returns mockk(relaxed = true)

        every { registeropplysningerService.slettRegisterOpplysninger(behandlingId) } just runs
        every { registeropplysningerService.hentOgLagreOpplysninger(any()) } just runs
        every { behandlingsresultatService.tømBehandlingsresultat(behandlingId, false) } just runs

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(behandlingId, false)

        verify { registeropplysningerService.slettRegisterOpplysninger(behandlingId) }
        verify { registeropplysningerService.hentOgLagreOpplysninger(any()) }
        verify { behandlingsresultatService.tømBehandlingsresultat(behandlingId, false) }
        verify(exactly = 0) { ufmKontrollService.utførKontrollerOgRegistrerFeil(behandlingId) }
        verify(exactly = 0) { inngangsvilkaarService.vurderOgLagreInngangsvilkår(any(), any(), any(), any()) }
    }

    @Test
    fun `skal kun hente og lagre registeropplysninger i oppdaterSaksopplysninger for aarsavregning`() {
        every { behandling.erÅrsavregning() } returns false
        every { behandling.finnPeriode() } returns Optional.of(Periode())
        every { fagsak.finnBrukersAktørID() } returns aktørId

        every { registeropplysningerFactory.utledSaksopplysningTyper(
            any(), any(), any(), any()
        ) } returns mockk(relaxed = true)

        every { registeropplysningerService.slettRegisterOpplysninger(behandlingId) } just runs
        every { registeropplysningerService.hentOgLagreOpplysninger(any()) } just runs

        oppfriskSaksopplysningerService.oppdaterSaksopplysningerForAarsavregning(behandlingId)

        verify { registeropplysningerService.slettRegisterOpplysninger(behandlingId) }
        verify { registeropplysningerService.hentOgLagreOpplysninger(any()) }
        verify(exactly = 0) { behandlingsresultatService.tømBehandlingsresultat(any(), false) }
    }
}
