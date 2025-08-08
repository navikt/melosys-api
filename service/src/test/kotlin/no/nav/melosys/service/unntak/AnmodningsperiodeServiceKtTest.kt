package no.nav.melosys.service.unntak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.*
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

    companion object {
        private const val ANMODNINGSPERIODE_ID = 11L
        private const val BEHANDLINGS_ID = 22L
    }

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
    }

    @Test
    fun `hentAnmodningsperiode`() {
        every { anmodningsperiodeRepository.findById(ANMODNINGSPERIODE_ID) } returns Optional.empty()

        anmodningsperiodeService.finnAnmodningsperiode(ANMODNINGSPERIODE_ID)
        
        verify { anmodningsperiodeRepository.findById(ANMODNINGSPERIODE_ID) }
    }

    @Test
    fun `hentAnmodningsperioder`() {
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(BEHANDLINGS_ID) } returns emptyList()

        anmodningsperiodeService.hentAnmodningsperioder(BEHANDLINGS_ID)
        
        verify { anmodningsperiodeRepository.findByBehandlingsresultatId(BEHANDLINGS_ID) }
    }

    @Test
    fun `lagreAnmodningsperioder - ingen svar registrert - mottar lagrede perioder`() {
        // Arrange
        val anmodningsperiode = lagAnmodningsperiode()
        val anmodningperioder = listOf(anmodningsperiode)
        val behandlingsresultat = Behandlingsresultat()

        every { anmodningsperiodeRepository.findByBehandlingsresultatId(BEHANDLINGS_ID) } returns listOf(anmodningsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGS_ID) } returns behandlingsresultat
        every { anmodningsperiodeRepository.saveAll(anmodningperioder) } returns anmodningperioder

        // Act
        anmodningsperiodeService.lagreAnmodningsperioder(BEHANDLINGS_ID, anmodningperioder)

        // Assert
        verify { anmodningsperiodeRepository.saveAll(anmodningperioder) }
        anmodningsperiode.behandlingsresultat shouldBe behandlingsresultat
    }

    @Test
    fun `lagreAnmodningsperioder - svar er registrert - forvent funksjonell exception`() {
        // Arrange
        val anmodningsperiode = lagAnmodningsperiode()
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(BEHANDLINGS_ID) } returns listOf(anmodningsperiode)
        anmodningsperiode.anmodningsperiodeSvar = AnmodningsperiodeSvar()

        // Act & Assert
        val exception = shouldThrow<FunksjonellException> {
            anmodningsperiodeService.lagreAnmodningsperioder(BEHANDLINGS_ID, listOf(anmodningsperiode))
        }
        exception.message shouldBe "svar er registrert"
    }

    @Test
    fun `lagreAnmodningsperiodeSvar - svar er innvilgelse - lagrer anmodningsperiode svar og lovvalgsperiode`() {
        // Arrange
        val anmodningsperiode = mockAnmodningsperiodeIdPaaFindById()
        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
            this.anmodningsperiode = anmodningsperiode
        }

        every { anmodningsperiodeSvarRepository.save(any<AnmodningsperiodeSvar>()) } returns svar
        every { lovvalgsperiodeService.lagreLovvalgsperioder(eq(BEHANDLINGS_ID), any()) } returns emptyList()

        // Act
        anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)

        // Assert
        verify { anmodningsperiodeSvarRepository.save(any<AnmodningsperiodeSvar>()) }
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(eq(BEHANDLINGS_ID), any()) }
    }

    @Test
    fun `lagreAnmodningsperiodeSvar - svar er avslag - lagrer anmodningsperiode svar og lovvalgsperiode`() {
        // Arrange
        val anmodningsperiode = mockAnmodningsperiodeIdPaaFindById()
        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
            this.anmodningsperiode = anmodningsperiode
        }

        every { anmodningsperiodeSvarRepository.save(svar) } returns svar
        every { lovvalgsperiodeService.lagreLovvalgsperioder(eq(BEHANDLINGS_ID), any()) } returns emptyList()

        // Act
        anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)

        // Assert
        verify { anmodningsperiodeSvarRepository.save(svar) }
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(eq(BEHANDLINGS_ID), any()) }
    }

    @Test
    fun `lagreAnmodningsperiodeSvar - svar er delvis innvilgelse ingen periode - forvent funksjonell exception`() {
        // Arrange
        val anmodningsperiode = mockAnmodningsperiodeIdPaaFindById()
        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.DELVIS_INNVILGELSE
            this.anmodningsperiode = anmodningsperiode
        }

        // Act & Assert
        val exception = shouldThrow<FunksjonellException> {
            anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)
        }
        exception.message shouldBe "Periode må være fyllt ut ved ${Anmodningsperiodesvartyper.DELVIS_INNVILGELSE}"
    }

    @Test
    fun `lagreAnmodningsperiodeSvar - mangler behandlingsresultat - forvent funksjonell exception`() {
        // Arrange
        val anmodningsperiode = mockAnmodningsperiodeIdPaaFindById()
        anmodningsperiode.behandlingsresultat = null

        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
            this.anmodningsperiode = anmodningsperiode
        }

        // Act & Assert
        val exception = shouldThrow<IllegalStateException> {
            anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)
        }
        exception.message shouldBe Anmodningsperiode.FEIL_VED_HENT_BEHANDLINGSRESULTAT_ID.format(ANMODNINGSPERIODE_ID)
    }

    @Test
    fun `lagreAnmodningsperiodeSvar - svar mangler type - forvent funksjonell exception`() {
        // Arrange
        mockAnmodningsperiodeIdPaaFindById()
        val svar = AnmodningsperiodeSvar()

        // Act & Assert
        val exception = shouldThrow<FunksjonellException> {
            anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)
        }
        exception.message shouldBe "Må spesifiseres svarType for svar på anmodningsperiode"
    }

    @Test
    fun `lagreAnmodningsperiodeSvar - ugyldig periode for delvis innvilgelse - forvent funksjonell exception`() {
        // Arrange
        mockAnmodningsperiodeIdPaaFindById()
        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.DELVIS_INNVILGELSE
            innvilgetFom = LocalDate.now()
            innvilgetTom = LocalDate.now().minusYears(2)
        }

        // Act & Assert
        val exception = shouldThrow<FunksjonellException> {
            anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)
        }
        exception.message shouldBe "Periode er ikke gyldig"
    }

    @Test
    fun `oppdaterAnmodningsperiodeSendtForBehandling - verifiser oppdatert`() {
        // Arrange
        val anmodningsperiode = Anmodningsperiode()
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(any<Long>()) } returns listOf(anmodningsperiode)
        every { anmodningsperiodeRepository.save(anmodningsperiode) } returns anmodningsperiode

        // Act
        anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(1L)

        // Assert
        anmodningsperiode.erSendtUtland().shouldBeTrue()
        verify { anmodningsperiodeRepository.save(anmodningsperiode) }
    }

    @Test
    fun `oppdaterAnmodetAvForBehandling - er ikke satt fra før - oppdateres`() {
        // Arrange
        val anmodetAv = "MEG"
        val anmodningsperiode = Anmodningsperiode()
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(any<Long>()) } returns listOf(anmodningsperiode)
        every { anmodningsperiodeRepository.save(anmodningsperiode) } returns anmodningsperiode

        // Act
        anmodningsperiodeService.oppdaterAnmodetAvForBehandling(1L, anmodetAv)

        // Assert
        anmodningsperiode.anmodetAv shouldBe anmodetAv
        verify { anmodningsperiodeRepository.save(anmodningsperiode) }
    }

    @Test
    fun `oppdaterAnmodetAvForBehandling - er satt fra før - kaster exception`() {
        // Arrange
        val anmodningsperiode = Anmodningsperiode()
        anmodningsperiode.anmodetAv = "DEG"
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(any<Long>()) } returns listOf(anmodningsperiode)

        // Act & Assert
        val exception = shouldThrow<FunksjonellException> {
            anmodningsperiodeService.oppdaterAnmodetAvForBehandling(1L, "MEG")
        }
        exception.message shouldBe "allerede anmodet av DEG"
    }

    private fun lagAnmodningsperiode(): Anmodningsperiode {
        val anmodningsperiode = Anmodningsperiode(
            LocalDate.now(), LocalDate.now().plusYears(2),
            Land_iso2.NO,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Land_iso2.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO
        )
        anmodningsperiode.id = ANMODNINGSPERIODE_ID
        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.id = BEHANDLINGS_ID
        anmodningsperiode.behandlingsresultat = behandlingsresultat
        return anmodningsperiode
    }

    private fun mockAnmodningsperiodeIdPaaFindById(): Anmodningsperiode {
        val anmodningsperiode = lagAnmodningsperiode()
        every { anmodningsperiodeRepository.findById(ANMODNINGSPERIODE_ID) } returns Optional.of(anmodningsperiode)
        return anmodningsperiode
    }
}