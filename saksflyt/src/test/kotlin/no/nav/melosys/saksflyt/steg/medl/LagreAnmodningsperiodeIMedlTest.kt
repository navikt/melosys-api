package no.nav.melosys.saksflyt.steg.medl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.medl.MedlPeriodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class LagreAnmodningsperiodeIMedlTest {

    private lateinit var behandlingsresultatService: BehandlingsresultatService
    private lateinit var medlPeriodeService: MedlPeriodeService

    private lateinit var lagreAnmodningsperiodeIMedl: LagreAnmodningsperiodeIMedl

    private lateinit var prosessinstans: Prosessinstans
    private lateinit var behandlingsresultat: Behandlingsresultat
    private lateinit var behandling: Behandling

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        behandlingsresultatService = mockk()
        medlPeriodeService = mockk(relaxed = true)

        lagreAnmodningsperiodeIMedl = LagreAnmodningsperiodeIMedl(behandlingsresultatService, medlPeriodeService)

        val anmodningsperiode = Anmodningsperiode(
            null, null, Land_iso2.CH,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, null, null, null, Trygdedekninger.FULL_DEKNING_EOSFO
        )

        behandlingsresultat = Behandlingsresultat().apply {
            anmodningsperioder = setOf(anmodningsperiode).toMutableSet()
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.ANMODNING_OM_UNNTAK
            status = ProsessStatus.KLAR
            behandling {
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.TRYGDETID
            }
        }
        behandling = prosessinstans.hentBehandling
    }

    @Test
    fun `utfør når behandlingsresultat type er anmodning om unntak`() {
        behandlingsresultat.type = Behandlingsresultattyper.ANMODNING_OM_UNNTAK
        behandlingsresultat.anmodningsperioder = lagAnmodningsperioderMedDato(NOW, NOW.plusMonths(1)).toMutableSet()


        lagreAnmodningsperiodeIMedl.utfør(prosessinstans)


        verify { medlPeriodeService.opprettPeriodeUnderAvklaring(any<Anmodningsperiode>(), any()) }
    }

    @Test
    fun `utfør når behandlingsresultat har ingen lovvalg periode`() {
        behandlingsresultat.anmodningsperioder = mutableSetOf()


        val exception = shouldThrow<NoSuchElementException> {
            lagreAnmodningsperiodeIMedl.utfør(prosessinstans)
        }


        exception.message!! shouldContain "Ingen anmodningsperioder finnes"
    }

    @Test
    fun `utfør ulogisk dato lagrer ikke`() {
        behandlingsresultat.anmodningsperioder = lagAnmodningsperioderMedDato(NOW, NOW.minusMonths(1)).toMutableSet()


        lagreAnmodningsperiodeIMedl.utfør(prosessinstans)


        verify(exactly = 0) { medlPeriodeService.opprettPeriodeUnderAvklaring(any<Anmodningsperiode>(), any()) }
    }

    @Test
    fun `utfør oppdater anmodningsperiode ok`() {
        val fagsak = Fagsak.forTest {
            medBruker()
        }
        val forrigeBehandling = Behandling.forTest {
            id = 2L
            medFagsak(fagsak)
            medType(Behandlingstyper.NY_VURDERING)
            medRegistrertDato(Instant.now().minusSeconds(10))
        }

        val førsteBehandling = Behandling.forTest {
            id = 3L
            medFagsak(fagsak)
            medType(Behandlingstyper.FØRSTEGANG)
            medRegistrertDato(Instant.now().minusSeconds(20))
        }
        val førsteAnmodningsperiode = Anmodningsperiode().apply {
            medlPeriodeID = 44L
        }

        behandling.fagsak = fagsak
        behandling.type = Behandlingstyper.NY_VURDERING
        behandling.registrertDato = Instant.now()

        fagsak.leggTilBehandling(behandling)
        fagsak.leggTilBehandling(forrigeBehandling)
        fagsak.leggTilBehandling(førsteBehandling)

        val anmodningsperiode = Anmodningsperiode(NOW, NOW.plusMonths(1), null, null, null, null, null, null)
        behandlingsresultat.anmodningsperioder = mutableSetOf(anmodningsperiode)

        every { behandlingsresultatService.hentBehandlingsresultat(forrigeBehandling.id) } returns lagBehandlingsresultat(
            Behandlingsresultattyper.IKKE_FASTSATT, null
        )
        every { behandlingsresultatService.hentBehandlingsresultat(førsteBehandling.id) } returns lagBehandlingsresultat(
            Behandlingsresultattyper.ANMODNING_OM_UNNTAK, førsteAnmodningsperiode
        )


        lagreAnmodningsperiodeIMedl.utfør(prosessinstans)


        verify { medlPeriodeService.oppdaterPeriodeUnderAvklaring(anmodningsperiode, behandling.id) }
    }

    private fun lagAnmodningsperioderMedDato(fom: LocalDate, tom: LocalDate) =
        setOf(
            Anmodningsperiode(
                fom, tom, null, null, null, null, null, null
            )
        )

    private fun lagBehandlingsresultat(
        behandlingsresultattyper: Behandlingsresultattyper,
        anmodningsperiode: Anmodningsperiode?
    ) = Behandlingsresultat().apply {
        this.type = behandlingsresultattyper
        if (anmodningsperiode != null) {
            this.anmodningsperioder = mutableSetOf(anmodningsperiode)
        }
    }

    companion object {
        val NOW: LocalDate = LocalDate.now()
    }
}
