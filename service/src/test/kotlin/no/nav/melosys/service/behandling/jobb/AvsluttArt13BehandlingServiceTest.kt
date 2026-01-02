package no.nav.melosys.service.behandling.jobb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.medl.MedlPeriodeService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@ExtendWith(MockKExtension::class)
class AvsluttArt13BehandlingServiceTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var medlPeriodeService: MedlPeriodeService

    @MockK
    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    private lateinit var avsluttArt13BehandlingService: AvsluttArt13BehandlingService

    private val behandlingID = 11L

    @BeforeEach
    fun setup() {
        avsluttArt13BehandlingService = AvsluttArt13BehandlingService(
            behandlingService, fagsakService,
            behandlingsresultatService, medlPeriodeService, lovvalgsperiodeService
        )

        every { fagsakService.avsluttFagsakOgBehandling(any(), any(), any()) } returns Unit
        every { medlPeriodeService.oppdaterPeriodeEndelig(any()) } returns Unit
    }

    private fun lagBehandling(
        init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit = {}
    ) = Behandling.forTest {
        id = behandlingID
        status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        tema = Behandlingstema.ARBEID_FLERE_LAND
        init()
    }

    private fun lagLovvalgsperiode(
        init: LovvalgsperiodeTestFactory.Builder.() -> Unit = {}
    ) = lovvalgsperiodeForTest {
        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A
        medlPeriodeID = 123L
        init()
    }

    private fun lagBehandlingsresultat(
        behandling: Behandling,
        lovvalgsperiode: Lovvalgsperiode,
        vedtaksdato: Instant? = null,
        harVedtakMetadata: Boolean = true,
        init: BehandlingsresultatTestFactory.Builder.() -> Unit = {}
    ) = Behandlingsresultat.forTest {
        id = behandlingID
        this.behandling = behandling
        lovvalgsperioder.add(lovvalgsperiode)
        if (harVedtakMetadata) {
            vedtakMetadata {
                if (vedtaksdato != null) {
                    this.vedtaksdato = vedtaksdato
                }
            }
        }
        init()
    }

    private fun setupMocks(behandling: Behandling, behandlingsresultat: Behandlingsresultat) {
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
    }

    @Test
    fun `avsluttBehandlingArt13 ikke art13 kaster exception`() {
        val behandling = lagBehandling()
        val lovvalgsperiode = lagLovvalgsperiode {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        }
        val behandlingsresultat = lagBehandlingsresultat(
            behandling = behandling,
            lovvalgsperiode = lovvalgsperiode,
            vedtaksdato = månederOgDagerSiden(2, 1)
        )
        setupMocks(behandling, behandlingsresultat)


        val exception = shouldThrow<FunksjonellException> {
            avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)
        }


        exception.message shouldContain "Behandling skal ikke avsluttes automatisk da perioden er av bestemmelse"
    }

    @Test
    fun `avsluttBehandlingArt13 søknad 1 måned siden vedtak behandling ikke avsluttet`() {
        val behandling = lagBehandling { tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE }
        val lovvalgsperiode = lagLovvalgsperiode()
        val behandlingsresultat = lagBehandlingsresultat(
            behandling = behandling,
            lovvalgsperiode = lovvalgsperiode,
            vedtaksdato = månederOgDagerSiden(1, 0)
        )
        setupMocks(behandling, behandlingsresultat)


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any(), any()) }
    }

    @Test
    fun `avsluttBehandlingArt13 norge utpekt 2 måneder 1 dag siden vedtak behandling blir avsluttet`() {
        val behandling = lagBehandling { tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE }
        val lovvalgsperiode = lagLovvalgsperiode()
        val behandlingsresultat = lagBehandlingsresultat(
            behandling = behandling,
            lovvalgsperiode = lovvalgsperiode,
            vedtaksdato = månederOgDagerSiden(2, 1)
        )
        setupMocks(behandling, behandlingsresultat)


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify { fagsakService.avsluttFagsakOgBehandling(behandling.fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode) }
    }

    @Test
    fun `avsluttBehandlingArt13 norge utpekt vedtak ikke lagret kaster exception`() {
        val behandling = lagBehandling { tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE }
        val lovvalgsperiode = lagLovvalgsperiode()
        val behandlingsresultat = lagBehandlingsresultat(
            behandling = behandling,
            lovvalgsperiode = lovvalgsperiode,
            harVedtakMetadata = false
        )
        setupMocks(behandling, behandlingsresultat)


        val exception = shouldThrow<IllegalArgumentException> {
            avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)
        }


        exception.message shouldContain "skal ha vedtak men mangler vedtak. Status kan ikke settes til AVSLUTTET"
    }

    @Test
    fun `avsluttBehandlingArt13 søknad 2 måneder 1 dag siden endret dato medl oppdatert og behandling blir avsluttet`() {
        val behandling = lagBehandling()
        val lovvalgsperiode = lagLovvalgsperiode()
        val behandlingsresultat = lagBehandlingsresultat(
            behandling = behandling,
            lovvalgsperiode = lovvalgsperiode,
            vedtaksdato = månederOgDagerSiden(2, 1)
        ) {
            endretDato = månederOgDagerSiden(2, 1)
        }
        setupMocks(behandling, behandlingsresultat)


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify { fagsakService.avsluttFagsakOgBehandling(behandling.fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode) }
    }

    @Test
    fun `avsluttBehandlingArt13 søknad 3 måneder siden endret dato utpeking uten vedtak lovvalgsperiode opprettet og behandling avsluttet`() {
        val behandling = lagBehandling()
        val lovvalgsperiode = lagLovvalgsperiode()
        val behandlingsresultat = lagBehandlingsresultat(
            behandling = behandling,
            lovvalgsperiode = lovvalgsperiode,
            harVedtakMetadata = false
        ) {
            endretDato = månederOgDagerSiden(3, 0)
            utpekingsperiode {
                fom = LocalDate.now()
                tom = LocalDate.now()
                lovvalgsland = Land_iso2.SE
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1
                medlPeriodeID = 123L
            }
        }
        setupMocks(behandling, behandlingsresultat)
        every { lovvalgsperiodeService.lagreLovvalgsperioder(any(), any()) } answers { secondArg() }


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify { lovvalgsperiodeService.lagreLovvalgsperioder(eq(behandlingID), any()) }
        verify { fagsakService.avsluttFagsakOgBehandling(behandling.fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { medlPeriodeService.oppdaterPeriodeEndelig(any()) }
    }

    @Test
    fun `avsluttBehandlingHvisToMndPassert skal ikke avslutte behandling hvis saksstatus ikke er OPPRETTET`() {
        val behandling = lagBehandling {
            fagsak { status = Saksstatuser.ANNULLERT }
        }
        val lovvalgsperiode = lagLovvalgsperiode()
        val behandlingsresultat = lagBehandlingsresultat(
            behandling = behandling,
            lovvalgsperiode = lovvalgsperiode,
            vedtaksdato = månederOgDagerSiden(2, 1)
        )
        setupMocks(behandling, behandlingsresultat)


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any(), any()) }
        verify(exactly = 0) { medlPeriodeService.oppdaterPeriodeEndelig(any()) }
    }

    @Test
    fun `avsluttBehandlingHvisToMndPassert skal ikke avslutte behandling hvis det finnes nyere relevant behandling på samme fagsak`() {
        val behandling = lagBehandling()
        val lovvalgsperiode = lagLovvalgsperiode()
        val behandlingsresultat = lagBehandlingsresultat(
            behandling = behandling,
            lovvalgsperiode = lovvalgsperiode,
            vedtaksdato = månederOgDagerSiden(2, 1)
        )
        setupMocks(behandling, behandlingsresultat)

        val nyereBehandlingID = 999L
        val nyereBehandling = Behandling.forTest {
            id = nyereBehandlingID
            status = Behandlingsstatus.AVSLUTTET
            tema = Behandlingstema.ARBEID_FLERE_LAND
            type = Behandlingstyper.NY_VURDERING
            this.fagsak = behandling.fagsak
        }
        nyereBehandling.registrertDato = behandling.registrertDato.plus(Duration.ofDays(1))
        behandling.fagsak.leggTilBehandling(nyereBehandling)

        val nyereBehandlingsresultat = Behandlingsresultat.forTest {
            id = nyereBehandlingID
            vedtakMetadata {
                vedtaksdato = månederOgDagerSiden(1, 0)
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(nyereBehandlingID) } returns nyereBehandlingsresultat


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any(), any()) }
        verify(exactly = 0) { medlPeriodeService.oppdaterPeriodeEndelig(any()) }
    }

    @Test
    fun `avsluttBehandlingHvisToMndPassert skal avslutte når nyere behandling er henvendelse`() {
        val behandling = lagBehandling()
        val lovvalgsperiode = lagLovvalgsperiode()
        val behandlingsresultat = lagBehandlingsresultat(
            behandling = behandling,
            lovvalgsperiode = lovvalgsperiode,
            vedtaksdato = månederOgDagerSiden(2, 1)
        )
        setupMocks(behandling, behandlingsresultat)

        val henvendelse = Behandling.forTest {
            id = 1000L
            status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
            tema = Behandlingstema.ARBEID_FLERE_LAND
            type = Behandlingstyper.HENVENDELSE
            this.fagsak = behandling.fagsak
        }
        henvendelse.registrertDato = behandling.registrertDato.plus(Duration.ofDays(1))
        behandling.fagsak.leggTilBehandling(henvendelse)


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify { fagsakService.avsluttFagsakOgBehandling(behandling.fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { medlPeriodeService.oppdaterPeriodeEndelig(any()) }
    }

    @Test
    fun `avsluttBehandlingHvisToMndPassert skal ikke avslutte hvis nyere behandling har registrert unntak`() {
        val behandling = lagBehandling()
        val lovvalgsperiode = lagLovvalgsperiode()
        val behandlingsresultat = lagBehandlingsresultat(
            behandling = behandling,
            lovvalgsperiode = lovvalgsperiode,
            vedtaksdato = månederOgDagerSiden(2, 1)
        )
        setupMocks(behandling, behandlingsresultat)

        val nyereBehandlingId = 666L
        val nyereBehandling = Behandling.forTest {
            id = nyereBehandlingId
            status = Behandlingsstatus.AVSLUTTET
            tema = Behandlingstema.ARBEID_FLERE_LAND
            type = Behandlingstyper.NY_VURDERING
            this.fagsak = behandling.fagsak
        }
        nyereBehandling.registrertDato = behandling.registrertDato.plus(Duration.ofDays(1))
        behandling.fagsak.leggTilBehandling(nyereBehandling)

        val nyereBehandlingsresultat = Behandlingsresultat.forTest {
            id = nyereBehandlingId
            utfallRegistreringUnntak = Utfallregistreringunntak.GODKJENT
        }
        every { behandlingsresultatService.hentBehandlingsresultat(nyereBehandlingId) } returns nyereBehandlingsresultat


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any(), any()) }
        verify(exactly = 0) { medlPeriodeService.oppdaterPeriodeEndelig(any()) }
    }

    @Test
    fun `avsluttBehandlingHvisToMndPassert skal avslutte når nyere avsluttet behandling mangler vedtak og utfall`() {
        val behandling = lagBehandling()
        val lovvalgsperiode = lagLovvalgsperiode()
        val behandlingsresultat = lagBehandlingsresultat(
            behandling = behandling,
            lovvalgsperiode = lovvalgsperiode,
            vedtaksdato = månederOgDagerSiden(2, 1)
        )
        setupMocks(behandling, behandlingsresultat)

        val nyereBehandlingId = 777L
        val nyereBehandling = Behandling.forTest {
            id = nyereBehandlingId
            status = Behandlingsstatus.AVSLUTTET
            tema = Behandlingstema.ARBEID_FLERE_LAND
            type = Behandlingstyper.NY_VURDERING
            this.fagsak = behandling.fagsak
        }
        nyereBehandling.registrertDato = behandling.registrertDato.plus(Duration.ofDays(1))
        behandling.fagsak.leggTilBehandling(nyereBehandling)

        val nyereBehandlingsresultat = Behandlingsresultat.forTest {
            id = nyereBehandlingId
            // Mangler både vedtakMetadata og utfallRegistreringUnntak
        }
        every { behandlingsresultatService.hentBehandlingsresultat(nyereBehandlingId) } returns nyereBehandlingsresultat


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify { fagsakService.avsluttFagsakOgBehandling(behandling.fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { medlPeriodeService.oppdaterPeriodeEndelig(any()) }
    }

    private fun månederOgDagerSiden(mnd: Long, dager: Long): Instant =
        LocalDate.now().minusMonths(mnd).minusDays(dager).atStartOfDay(ZoneId.systemDefault()).toInstant()
}
