package no.nav.melosys.service.unntak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class AnmodningsperiodeServiceKtTest {

    companion object {
        private const val ANMODNINGSPERIODE_ID = 11L
        private const val BEHANDLINGS_ID = 22L
    }

    private val anmodningsperiodeRepository: AnmodningsperiodeRepository = mockk()
    private val behandlingsresultatService: BehandlingsresultatService = mockk()
    private val lovvalgsperiodeService: LovvalgsperiodeService = mockk()
    private val anmodningsperiodeSvarRepository: AnmodningsperiodeSvarRepository = mockk()

    private lateinit var anmodningsperiodeService: AnmodningsperiodeService

    @BeforeEach
    fun setUp() {
        anmodningsperiodeService = AnmodningsperiodeService(
            anmodningsperiodeRepository, lovvalgsperiodeService,
            anmodningsperiodeSvarRepository, behandlingsresultatService
        )
    }

    @Test
    fun `hentAnmodningsperiode`() {
        every { anmodningsperiodeRepository.findById(any()) } returns Optional.empty()

        anmodningsperiodeService.finnAnmodningsperiode(ANMODNINGSPERIODE_ID)
        
        verify { anmodningsperiodeRepository.findById(ANMODNINGSPERIODE_ID) }
    }

    @Test
    fun `hentAnmodningsperioder`() {
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(any()) } returns emptyList()

        anmodningsperiodeService.hentAnmodningsperioder(BEHANDLINGS_ID)
        
        verify { anmodningsperiodeRepository.findByBehandlingsresultatId(BEHANDLINGS_ID) }
    }

    @Test
    fun `lagreAnmodningsperioder ingenSvarRegistrert mottarLagredePerioder`() {
        val anmodningsperiode = lagAnmodningsperiode()
        val anmodningperioder = listOf(anmodningsperiode)
        val behandlingsresultat = Behandlingsresultat()

        every { anmodningsperiodeRepository.findByBehandlingsresultatId(BEHANDLINGS_ID) } returns listOf(anmodningsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGS_ID) } returns behandlingsresultat
        every { anmodningsperiodeRepository.deleteByBehandlingsresultat(any()) } just Runs
        every { anmodningsperiodeRepository.flush() } just Runs
        every { anmodningsperiodeRepository.saveAll(any<Collection<Anmodningsperiode>>()) } returns anmodningperioder

        anmodningsperiodeService.lagreAnmodningsperioder(BEHANDLINGS_ID, anmodningperioder)

        verify { anmodningsperiodeRepository.saveAll(anmodningperioder) }
        anmodningsperiode.behandlingsresultat shouldBe behandlingsresultat
    }

    @Test
    fun `lagreAnmodningsperioder svarErRegistrert forventFunksjonellException`() {
        val anmodningsperiode = lagAnmodningsperiode().apply {
            anmodningsperiodeSvar = AnmodningsperiodeSvar()
        }

        every { anmodningsperiodeRepository.findByBehandlingsresultatId(BEHANDLINGS_ID) } returns listOf(anmodningsperiode)

        val exception = assertThrows<FunksjonellException> {
            anmodningsperiodeService.lagreAnmodningsperioder(BEHANDLINGS_ID, listOf(anmodningsperiode))
        }

        exception.message shouldNotBe null
        exception.message?.contains("svar er registrert") shouldBe true
    }

    @Test
    fun `lagreAnmodningsperiodeSvar svarErInnvilgelse lagrerAnmodningsperiodeSvarOgLovvalgsperiode`() {
        val anmodningsperiode = mockAnmodningsperiodeIdPåFindById()

        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
            setAnmodningsperiode(anmodningsperiode)
        }

        every { anmodningsperiodeSvarRepository.save(any()) } returns svar
        every { lovvalgsperiodeService.lagreLovvalgsperioder(any(), any()) } just Runs

        anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)

        verify { anmodningsperiodeSvarRepository.save(any<AnmodningsperiodeSvar>()) }
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(eq(BEHANDLINGS_ID), any()) }
    }

    @Test
    fun `lagreAnmodningsperiodeSvar svarErAvslag lagrerAnmodningsperiodeSvarOgLovvalgsperiode`() {
        val anmodningsperiode = mockAnmodningsperiodeIdPåFindById()

        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
            setAnmodningsperiode(anmodningsperiode)
        }

        every { anmodningsperiodeSvarRepository.save(any()) } returns svar
        every { lovvalgsperiodeService.lagreLovvalgsperioder(any(), any()) } just Runs

        anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)

        verify { anmodningsperiodeSvarRepository.save(svar) }
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(eq(BEHANDLINGS_ID), any()) }
    }

    @Test
    fun `lagreAnmodningsperiodeSvar svarErDelvisInnvilgelseIngenPeriode forventFunksjonellException`() {
        val anmodningsperiode = mockAnmodningsperiodeIdPåFindById()

        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.DELVIS_INNVILGELSE
            setAnmodningsperiode(anmodningsperiode)
        }

        val exception = assertThrows<FunksjonellException> {
            anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)
        }

        exception.message shouldNotBe null
        exception.message?.contains("Periode må være fyllt ut ved ${Anmodningsperiodesvartyper.DELVIS_INNVILGELSE}") shouldBe true
    }

    @Test
    fun `lagreAnmodningsperiodeSvar manglerBehandlingsresultat forventFunksjonellException`() {
        val anmodningsperiode = mockAnmodningsperiodeIdPåFindById().apply {
            behandlingsresultat = null
        }

        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
            setAnmodningsperiode(anmodningsperiode)
        }

        val exception = assertThrows<IllegalStateException> {
            anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)
        }

        exception.message shouldNotBe null
        exception.message?.contains(Anmodningsperiode.FEIL_VED_HENT_BEHANDLINGSRESULTAT_ID.format(ANMODNINGSPERIODE_ID)) shouldBe true
    }

    @Test
    fun `lagreAnmodningsperiodeSvar svarManglerType forventFunksjonellException`() {
        mockAnmodningsperiodeIdPåFindById()
        val svar = AnmodningsperiodeSvar()

        val exception = assertThrows<FunksjonellException> {
            anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)
        }

        exception.message shouldNotBe null
        exception.message?.contains("Må spesifiseres svarType for svar på anmodningsperiode") shouldBe true
    }

    @Test
    fun `lagreAnmodningsperiodeSvar ugyldigPeriodeForDelvisInnvilgelse forventFunksjonellException`() {
        mockAnmodningsperiodeIdPåFindById()
        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.DELVIS_INNVILGELSE
            innvilgetFom = LocalDate.now()
            innvilgetTom = LocalDate.now().minusYears(2)
        }

        val exception = assertThrows<FunksjonellException> {
            anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar)
        }

        exception.message shouldNotBe null
        exception.message?.contains("Periode er ikke gyldig") shouldBe true
    }

    @Test
    fun `oppdaterAnmodningsperiodeSendtForBehandling verifiserOppdatert`() {
        val anmodningsperiode = Anmodningsperiode()
        
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(any()) } returns listOf(anmodningsperiode)
        every { anmodningsperiodeRepository.save(any()) } returns anmodningsperiode

        anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(1L)

        anmodningsperiode.erSendtUtland() shouldBe true
        verify { anmodningsperiodeRepository.save(anmodningsperiode) }
    }

    @Test
    fun `oppdaterAnmodetAvForBehandling erIkkeSattFraFør oppdateres`() {
        val anmodetAv = "MEG"
        val anmodningsperiode = Anmodningsperiode()
        
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(any()) } returns listOf(anmodningsperiode)
        every { anmodningsperiodeRepository.save(any()) } returns anmodningsperiode

        anmodningsperiodeService.oppdaterAnmodetAvForBehandling(1L, anmodetAv)

        anmodningsperiode.anmodetAv shouldBe anmodetAv
        verify { anmodningsperiodeRepository.save(anmodningsperiode) }
    }

    @Test
    fun `oppdaterAnmodetAvForBehandling erSattFraFør kasterException`() {
        val anmodningsperiode = Anmodningsperiode().apply {
            anmodetAv = "DEG"
        }
        
        every { anmodningsperiodeRepository.findByBehandlingsresultatId(any()) } returns listOf(anmodningsperiode)

        val exception = assertThrows<FunksjonellException> {
            anmodningsperiodeService.oppdaterAnmodetAvForBehandling(1L, "MEG")
        }

        exception.message shouldNotBe null
        exception.message?.contains("allerede anmodet av DEG") shouldBe true
    }

    private fun lagAnmodningsperiode(): Anmodningsperiode {
        val anmodningsperiode = Anmodningsperiode(
            LocalDate.now(), LocalDate.now().plusYears(2),
            Land_iso2.NO,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Land_iso2.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO
        ).apply {
            id = ANMODNINGSPERIODE_ID
        }
        
        val behandlingsresultat = Behandlingsresultat().apply {
            id = BEHANDLINGS_ID
        }
        anmodningsperiode.behandlingsresultat = behandlingsresultat
        return anmodningsperiode
    }

    private fun mockAnmodningsperiodeIdPåFindById(): Anmodningsperiode {
        val anmodningsperiode = lagAnmodningsperiode()
        every { anmodningsperiodeRepository.findById(ANMODNINGSPERIODE_ID) } returns Optional.of(anmodningsperiode)
        return anmodningsperiode
    }
}