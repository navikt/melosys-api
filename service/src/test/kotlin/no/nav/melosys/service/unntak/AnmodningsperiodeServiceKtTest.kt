package no.nav.melosys.service.unntak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.AnmodningsperiodeSvar
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.AnmodningsperiodeRepository
import no.nav.melosys.repository.AnmodningsperiodeSvarRepository
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class AnmodningsperiodeServiceKtTest {

    private val anmodningsperiodeRepository = mockk<AnmodningsperiodeRepository>()
    private val behandlingsresultatService = mockk<BehandlingsresultatService>()
    private val lovvalgsperiodeService = mockk<LovvalgsperiodeService>()
    private val anmodningsperiodeSvarRepository = mockk<AnmodningsperiodeSvarRepository>()

    private lateinit var anmodningsperiodeService: AnmodningsperiodeService

    @BeforeEach
    fun setUp() {
        anmodningsperiodeService = AnmodningsperiodeService(
            anmodningsperiodeRepository,
            lovvalgsperiodeService,
            anmodningsperiodeSvarRepository,
            behandlingsresultatService
        )

        // Set up common mocks that are used across multiple tests
        every { anmodningsperiodeRepository.deleteByBehandlingsresultat(any()) } returns emptyList()
        every { anmodningsperiodeRepository.flush() } returns Unit
        every { anmodningsperiodeSvarRepository.save(any()) } returnsArgument 0
    }

    @Test
    fun hentAnmodningsperiode() {
        every { anmodningsperiodeRepository.findById(ANMODNINGSPERIODE_ID) } returns Optional.empty()

        anmodningsperiodeService.finnAnmodningsperiode(ANMODNINGSPERIODE_ID)

        verify { anmodningsperiodeRepository.findById(ANMODNINGSPERIODE_ID) }
    }

    @Test
    fun hentAnmodningsperioder() {
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(BEHANDLINGS_ID) } returns emptyList()

        anmodningsperiodeService.hentAnmodningsperioder(BEHANDLINGS_ID)

        verify { anmodningsperiodeRepository.findByBehandlingsresultatId(BEHANDLINGS_ID) }
    }

    @Test
    fun `lagreAnmodningsperioder - ingen svar registrert - mottar lagrede perioder`() {
        val anmodningsperiode = lagAnmodningsperiode()
        val anmodningperioder = listOf(anmodningsperiode)
        val behandlingsresultat = Behandlingsresultat()
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(BEHANDLINGS_ID) } returns listOf(anmodningsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGS_ID) } returns behandlingsresultat
        every { anmodningsperiodeRepository.saveAll(anmodningperioder) } returns anmodningperioder


        anmodningsperiodeService.lagreAnmodningsperioder(BEHANDLINGS_ID, anmodningperioder)


        verify { anmodningsperiodeRepository.saveAll(anmodningperioder) }
        anmodningsperiode.behandlingsresultat shouldBe behandlingsresultat
    }

    @Test
    fun `lagreAnmodningsperioder - svar er registrert - forvent funksjonell exception`() {
        val anmodningsperiode = lagAnmodningsperiode().apply {
            anmodningsperiodeSvar = AnmodningsperiodeSvar()
        }
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(BEHANDLINGS_ID) } returns listOf(anmodningsperiode)


        val exception = shouldThrow<FunksjonellException> {
            anmodningsperiodeService.lagreAnmodningsperioder(BEHANDLINGS_ID, listOf(anmodningsperiode))
        }


        exception.message shouldBe "Kan ikke oppdatere anmodningsperiode etter at svar er registrert!"
    }

    @Test
    fun `lagreAnmodningsperiodeSvar - svar er innvilgelse - lagrer anmodningsperiode svar og lovvalgsperiode`() {
        val anmodningsperiode = mockAnmodningsperiodeIdPaaFindById()
        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
            this.anmodningsperiode = anmodningsperiode
        }
        every { anmodningsperiodeSvarRepository.save(any<AnmodningsperiodeSvar>()) } returns svar
        every { lovvalgsperiodeService.lagreLovvalgsperioder(eq(BEHANDLINGS_ID), any()) } returns emptyList()


        anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)


        verify { anmodningsperiodeSvarRepository.save(any<AnmodningsperiodeSvar>()) }
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(eq(BEHANDLINGS_ID), any()) }
    }

    @Test
    fun `lagreAnmodningsperiodeSvar - svar er avslag - lagrer anmodningsperiode svar og lovvalgsperiode`() {
        val anmodningsperiode = mockAnmodningsperiodeIdPaaFindById()
        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
            this.anmodningsperiode = anmodningsperiode
        }
        every { anmodningsperiodeSvarRepository.save(svar) } returns svar
        every { lovvalgsperiodeService.lagreLovvalgsperioder(eq(BEHANDLINGS_ID), any()) } returns emptyList()


        anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)


        verify { anmodningsperiodeSvarRepository.save(svar) }
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(eq(BEHANDLINGS_ID), any()) }
    }

    @Test
    fun `lagreAnmodningsperiodeSvar - svar er delvis innvilgelse ingen periode - forvent funksjonell exception`() {
        val anmodningsperiode = mockAnmodningsperiodeIdPaaFindById()
        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.DELVIS_INNVILGELSE
            this.anmodningsperiode = anmodningsperiode
        }


        val exception = shouldThrow<FunksjonellException> {
            anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)
        }


        exception.message shouldBe "Periode må være fyllt ut ved ${Anmodningsperiodesvartyper.DELVIS_INNVILGELSE}"
    }

    @Test
    fun `lagreAnmodningsperiodeSvar - mangler behandlingsresultat - forvent funksjonell exception`() {
        val anmodningsperiode = mockAnmodningsperiodeIdPaaFindById().apply {
            behandlingsresultat = null
        }
        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
            this.anmodningsperiode = anmodningsperiode
        }


        val exception = shouldThrow<IllegalStateException> {
            anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)
        }


        exception.message shouldBe Anmodningsperiode.FEIL_VED_HENT_BEHANDLINGSRESULTAT_ID.format(ANMODNINGSPERIODE_ID)
    }

    @Test
    fun `lagreAnmodningsperiodeSvar - svar mangler type - forvent funksjonell exception`() {
        mockAnmodningsperiodeIdPaaFindById()
        val svar = AnmodningsperiodeSvar()


        val exception = shouldThrow<FunksjonellException> {
            anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)
        }


        exception.message shouldBe "Må spesifiseres svarType for svar på anmodningsperiode"
    }

    @Test
    fun `lagreAnmodningsperiodeSvar - ugyldig periode for delvis innvilgelse - forvent funksjonell exception`() {
        mockAnmodningsperiodeIdPaaFindById()
        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.DELVIS_INNVILGELSE
            innvilgetFom = LocalDate.now()
            innvilgetTom = LocalDate.now().minusYears(2)
        }


        val exception = shouldThrow<FunksjonellException> {
            anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)
        }


        exception.message shouldBe "Periode er ikke gyldig"
    }

    @Test
    fun `oppdaterAnmodningsperiodeSendtForBehandling - verifiser oppdatert`() {
        val anmodningsperiode = Anmodningsperiode()
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(any<Long>()) } returns listOf(anmodningsperiode)
        every { anmodningsperiodeRepository.save(anmodningsperiode) } returns anmodningsperiode


        anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(1L)


        anmodningsperiode.erSendtUtland().shouldBeTrue()
        verify { anmodningsperiodeRepository.save(anmodningsperiode) }
    }

    @Test
    fun `oppdaterAnmodetAvForBehandling - er ikke satt fra før - oppdateres`() {
        val anmodetAv = "MEG"
        val anmodningsperiode = Anmodningsperiode()
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(any<Long>()) } returns listOf(anmodningsperiode)
        every { anmodningsperiodeRepository.save(anmodningsperiode) } returns anmodningsperiode


        anmodningsperiodeService.oppdaterAnmodetAvForBehandling(1L, anmodetAv)


        anmodningsperiode.anmodetAv shouldBe anmodetAv
        verify { anmodningsperiodeRepository.save(anmodningsperiode) }
    }

    @Test
    fun `oppdaterAnmodetAvForBehandling - er satt fra før - kaster exception`() {
        val anmodningsperiode = Anmodningsperiode().apply {
            anmodetAv = "DEG"
        }
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(any<Long>()) } returns listOf(anmodningsperiode)


        val exception = shouldThrow<FunksjonellException> {
            anmodningsperiodeService.oppdaterAnmodetAvForBehandling(1L, "MEG")
        }


        exception.message shouldBe "Anmodningsperiode for behandling 1 er allerede anmodet av DEG"
    }

    private fun lagAnmodningsperiode(): Anmodningsperiode = Anmodningsperiode(
        LocalDate.now(), LocalDate.now().plusYears(2),
        Land_iso2.NO,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
        null, Land_iso2.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO
    ).apply {
        id = ANMODNINGSPERIODE_ID
        behandlingsresultat = Behandlingsresultat().apply {
            id = BEHANDLINGS_ID
        }
    }

    private fun mockAnmodningsperiodeIdPaaFindById(): Anmodningsperiode {
        val anmodningsperiode = lagAnmodningsperiode()
        every { anmodningsperiodeRepository.findById(ANMODNINGSPERIODE_ID) } returns Optional.of(anmodningsperiode)
        return anmodningsperiode
    }

    companion object {
        private const val ANMODNINGSPERIODE_ID = 11L
        private const val BEHANDLINGS_ID = 22L
    }
}
