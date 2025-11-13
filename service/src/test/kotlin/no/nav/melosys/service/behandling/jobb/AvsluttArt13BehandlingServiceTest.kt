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

    private val behandlingsresultat = Behandlingsresultat()
    private lateinit var behandling: Behandling
    private val lovvalgsperiode = Lovvalgsperiode()
    private val vedtakMetadata = VedtakMetadata()
    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun setup() {
        avsluttArt13BehandlingService = AvsluttArt13BehandlingService(
            behandlingService, fagsakService,
            behandlingsresultatService, medlPeriodeService, lovvalgsperiodeService
        )

        behandling = Behandling.forTest {
            id = behandlingID
            status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
            tema = Behandlingstema.ARBEID_FLERE_LAND
        }
        fagsak = behandling.fagsak

        behandlingsresultat.apply {
            id = behandlingID
            this.behandling = this@AvsluttArt13BehandlingServiceTest.behandling
            lovvalgsperioder.add(lovvalgsperiode)
            this.vedtakMetadata = this@AvsluttArt13BehandlingServiceTest.vedtakMetadata
        }
        lovvalgsperiode.apply {
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A
            medlPeriodeID = 123L
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.avsluttFagsakOgBehandling(any(), any(), any()) } returns Unit
        every { medlPeriodeService.oppdaterPeriodeEndelig(any()) } returns Unit
    }

    @Test
    fun `avsluttBehandlingArt13 ikke art13 kaster exception`() {
        vedtakMetadata.vedtaksdato = månederOgDagerSiden(2, 1)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1


        val exception = shouldThrow<FunksjonellException> {
            avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)
        }


        exception.message shouldContain "Behandling skal ikke avsluttes automatisk da perioden er av bestemmelse"
    }

    @Test
    fun `avsluttBehandlingArt13 søknad 1 måned siden vedtak behandling ikke avsluttet`() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        vedtakMetadata.vedtaksdato = månederOgDagerSiden(1, 0)


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any(), any()) }
    }

    @Test
    fun `avsluttBehandlingArt13 norge utpekt 2 måneder 1 dag siden vedtak behandling blir avsluttet`() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        vedtakMetadata.vedtaksdato = månederOgDagerSiden(2, 1)


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode) }
    }

    @Test
    fun `avsluttBehandlingArt13 norge utpekt vedtak ikke lagret kaster exception`() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        behandlingsresultat.vedtakMetadata = null


        val exception = shouldThrow<IllegalArgumentException> {
            avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)
        }


        exception.message shouldContain "skal ha vedtak men mangler vedtak. Status kan ikke settes til AVSLUTTET"
    }

    @Test
    fun `avsluttBehandlingArt13 søknad 2 måneder 1 dag siden endret dato medl oppdatert og behandling blir avsluttet`() {
        behandlingsresultat.endretDato = månederOgDagerSiden(2, 1)
        vedtakMetadata.vedtaksdato = månederOgDagerSiden(2, 1)


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode) }
    }

    @Test
    fun `avsluttBehandlingArt13 søknad 3 måneder siden endret dato utpeking uten vedtak lovvalgsperiode opprettet og behandling avsluttet`() {
        val utpekingsperiode = Utpekingsperiode(
            LocalDate.now(), LocalDate.now(), Land_iso2.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, null
        ).apply {
            medlPeriodeID = 123L
        }
        behandlingsresultat.apply {
            endretDato = månederOgDagerSiden(3, 0)
            vedtakMetadata = null
            utpekingsperioder.add(utpekingsperiode)
        }
        every { lovvalgsperiodeService.lagreLovvalgsperioder(any(), any()) } answers { secondArg() }


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify { lovvalgsperiodeService.lagreLovvalgsperioder(eq(behandlingID), any()) }
        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { medlPeriodeService.oppdaterPeriodeEndelig(any()) }
    }

    @Test
    fun `avsluttBehandlingHvisToMndPassert skal ikke avslutte behandling hvis saksstatus ikke er OPPRETTET`() {
        vedtakMetadata.vedtaksdato = månederOgDagerSiden(2, 1)
        fagsak.status = Saksstatuser.ANNULLERT


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any(), any()) }
        verify(exactly = 0) { medlPeriodeService.oppdaterPeriodeEndelig(any()) }
    }

    @Test
    fun `avsluttBehandlingHvisToMndPassert skal ikke avslutte behandling hvis det finnes nyere relevant behandling på samme fagsak`() {
        vedtakMetadata.vedtaksdato = månederOgDagerSiden(2, 1)
        val nyereBehandlingID = 999L
        val nyereBehandling = Behandling.forTest {
            id = nyereBehandlingID
            status = Behandlingsstatus.AVSLUTTET // Nyere behandling er avsluttet
            tema = Behandlingstema.ARBEID_FLERE_LAND
            type = Behandlingstyper.NY_VURDERING // relevant type
            this.fagsak = behandling.fagsak
        }
        nyereBehandling.registrertDato = behandling.registrertDato.plus(Duration.ofDays(1))
        behandling.fagsak.leggTilBehandling(nyereBehandling)

        // Nyere behandling har vedtak
        val nyereBehandlingsresultat = Behandlingsresultat().apply {
            id = nyereBehandlingID
            vedtakMetadata = VedtakMetadata().apply {
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
        // Oppfyller 2 mnd krav
        vedtakMetadata.vedtaksdato = månederOgDagerSiden(2, 1)
        // Nyere HENVENDELSE skal ikke blokkere
        val henvendelse = Behandling.forTest {
            id = 1000L
            status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING // lik status men type gjør den irrelevant
            tema = Behandlingstema.ARBEID_FLERE_LAND
            type = Behandlingstyper.HENVENDELSE
            this.fagsak = behandling.fagsak
        }
        henvendelse.registrertDato = behandling.registrertDato.plus(Duration.ofDays(1))
        behandling.fagsak.leggTilBehandling(henvendelse)


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { medlPeriodeService.oppdaterPeriodeEndelig(any()) }
    }

    @Test
    fun `avsluttBehandlingHvisToMndPassert skal ikke avslutte hvis nyere behandling har registrert unntak`() {
        vedtakMetadata.vedtaksdato = månederOgDagerSiden(2, 1)
        val nyereBehandlingId = 666L
        val nyereBehandling = Behandling.forTest {
            id = nyereBehandlingId
            status = Behandlingsstatus.AVSLUTTET
            tema = Behandlingstema.ARBEID_FLERE_LAND
            type = Behandlingstyper.NY_VURDERING // relevant type
            this.fagsak = behandling.fagsak
        }
        nyereBehandling.registrertDato = behandling.registrertDato.plus(Duration.ofDays(1))
        behandling.fagsak.leggTilBehandling(nyereBehandling)

        // Nyere behandling har utfallRegistreringUnntak (men ikke vedtak)
        val nyereBehandlingsresultat = Behandlingsresultat().apply {
            id = nyereBehandlingId
            vedtakMetadata = null
            utfallRegistreringUnntak = Utfallregistreringunntak.GODKJENT
        }
        every { behandlingsresultatService.hentBehandlingsresultat(nyereBehandlingId) } returns nyereBehandlingsresultat


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any(), any()) }
        verify(exactly = 0) { medlPeriodeService.oppdaterPeriodeEndelig(any()) }
    }

    @Test
    fun `avsluttBehandlingHvisToMndPassert skal avslutte når nyere avsluttet behandling mangler vedtak og utfall`() {
        vedtakMetadata.vedtaksdato = månederOgDagerSiden(2, 1)
        val nyereBehandlingId = 777L
        val nyereBehandling = Behandling.forTest {
            id = nyereBehandlingId
            status = Behandlingsstatus.AVSLUTTET
            tema = Behandlingstema.ARBEID_FLERE_LAND
            type = Behandlingstyper.NY_VURDERING // relevant type
            this.fagsak = behandling.fagsak
        }
        nyereBehandling.registrertDato = behandling.registrertDato.plus(Duration.ofDays(1))
        behandling.fagsak.leggTilBehandling(nyereBehandling)

        // Nyere behandling mangler både vedtak og utfallRegistreringUnntak
        val nyereBehandlingsresultat = Behandlingsresultat().apply {
            id = nyereBehandlingId
            vedtakMetadata = null
            utfallRegistreringUnntak = null
        }
        every { behandlingsresultatService.hentBehandlingsresultat(nyereBehandlingId) } returns nyereBehandlingsresultat


        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { medlPeriodeService.oppdaterPeriodeEndelig(any()) }
    }

    private fun månederOgDagerSiden(mnd: Long, dager: Long): Instant =
        LocalDate.now().minusMonths(mnd).minusDays(dager).atStartOfDay(ZoneId.systemDefault()).toInstant()
}
