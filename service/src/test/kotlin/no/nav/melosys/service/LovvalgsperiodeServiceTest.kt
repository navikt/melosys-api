package no.nav.melosys.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.optional.shouldNotBePresent
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.medl.GrunnlagMedl
import no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter.Companion.tilLovvalgBestemmelse
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.LovvalgsperiodeRepository
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.util.*
import java.util.List
import java.util.stream.StreamSupport

@ExtendWith(MockitoExtension::class)
internal class LovvalgsperiodeServiceTest {
    @RelaxedMockK
    lateinit var lovvalgsperiodeRepository: LovvalgsperiodeRepository

    @RelaxedMockK
    lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @RelaxedMockK
    lateinit var tidligereMedlemsperiodeRepository: TidligereMedlemsperiodeRepository

    @RelaxedMockK
    lateinit var behandlingRepository: BehandlingRepository

    @InjectMockKs
    lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun hentLovvalgsperioder_ingenLovvalgsperioder_girTomListe() {
        every { lovvalgsperiodeRepository.findByBehandlingsresultatId(BEH_ID) } returns emptyList()

        lovvalgsperiodeService.hentLovvalgsperioder(BEH_ID).shouldBeEmpty()
    }

    @Test
    fun lagreLovvalgsperioderReturnererLovvalgsperiodeMedBehandlingsresultat() {
        val lagretBehandlingsresultat = Behandlingsresultat().apply { id = BEH_ID }
        every { lovvalgsperiodeRepository.deleteByBehandlingsresultatId(BEH_ID) } just Runs
        every { behandlingsresultatRepository.findById(BEH_ID) } returns Optional.of(lagretBehandlingsresultat)
        every { lovvalgsperiodeRepository.saveAllAndFlush(any<kotlin.collections.List<Lovvalgsperiode>>()) } answers { firstArg() }


        val lovvalgsPerioder = listOf(Lovvalgsperiode())
        lovvalgsperiodeService.lagreLovvalgsperioder(BEH_ID, lovvalgsPerioder).run {
            shouldHaveSize(1)
            harBehandlingsResultatMedRiktigId(this) shouldBe true
        }

        harBehandlingsResultatMedRiktigId(lovvalgsPerioder) shouldBe false
    }

    @Test
    fun lagreLovvalgsperioderUtenBehandlingsresultatKasterException() {
        val lovvalgsperioder = List.of(Lovvalgsperiode())
        every { behandlingsresultatRepository.findById(BEH_ID) } returns Optional.empty()

        shouldThrow<IllegalStateException> {
            lovvalgsperiodeService
                .lagreLovvalgsperioder(BEH_ID, lovvalgsperioder)
        }.message shouldBe "Behandling 1 fins ikke."
    }

    @Test
    fun oppdaterLovvalgsperiode_lovvalgsperiodeFinnes_oppdatererFelt() {
        val lovvalgsCaptor = slot<Lovvalgsperiode>()

        val lovvalgsperiodeId = 3L
        val eksisterendeLovvalgsperiode = Lovvalgsperiode().apply {
            id = lovvalgsperiodeId
        }

        every { lovvalgsperiodeRepository.findById(lovvalgsperiodeId) } returns Optional.of(eksisterendeLovvalgsperiode)
        every { lovvalgsperiodeRepository.save(capture(lovvalgsCaptor)) } answers { firstArg() }

        val request = Lovvalgsperiode().apply {
            fom = LocalDate.now()
            tom = LocalDate.now()
            lovvalgsland = Land_iso2.BA
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E
            tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
            innvilgelsesresultat = InnvilgelsesResultat.DELVIS_INNVILGET
            dekning = Trygdedekninger.FULL_DEKNING
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            medlPeriodeID = 23L
        }


        lovvalgsperiodeService.oppdaterLovvalgsperiode(3L, request)


        Assertions.assertThat(lovvalgsCaptor.captured)
            .isNotNull()
            .extracting(
                "fom", "tom", "lovvalgsland", "bestemmelse",
                "tilleggsbestemmelse", "innvilgelsesresultat",
                "medlemskapstype", "dekning", "medlPeriodeID"
            )
            .containsExactly(
                request.fom, request.tom, request.lovvalgsland, request.bestemmelse,
                request.tilleggsbestemmelse, request.innvilgelsesresultat,
                request.medlemskapstype, request.dekning, request.medlPeriodeID
            )
    }

    @Test
    fun oppdaterLovvalgsperiode_lovvalgsperiodeFinnesIkke_kasterException() {
        val lovvalgsPeriodeId = 3L
        val request = Lovvalgsperiode()
        every { lovvalgsperiodeRepository.findById(lovvalgsPeriodeId) } returns Optional.empty()

        shouldThrow<FunksjonellException> {
            lovvalgsperiodeService.oppdaterLovvalgsperiode(lovvalgsPeriodeId, request)
        }.message shouldBe "Lovvalgsperioden 3 finnes ikke"
    }


    @Test
    fun hentTidligereLovvalgsperioder_enValgtMedlemsperiode_returnererEnTidligerLovvalgsperiode() {
        val medlemsperiode = lagMedlemsperiode(23L, GrunnlagMedl.FO_12_2.kode)
        val medlemsperiodeFeilId = lagMedlemsperiode(46L, GrunnlagMedl.FO_12_2.kode)

        val medlDokument = MedlemskapDokument()
        medlDokument.getMedlemsperiode().add(medlemsperiode)
        medlDokument.getMedlemsperiode().add(medlemsperiodeFeilId)

        val behandling = lagBehandlingMedMedlOpplysning(medlDokument)
        mockTidligereMedlemsperiodeRepository(medlemsperiode.id!!)


        lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling).shouldForAll {
            it.medlPeriodeID shouldBe medlemsperiode.id
            it.fom shouldBe medlemsperiode.periode!!.fom
            it.tom shouldBe medlemsperiode.periode!!.tom
            it.bestemmelse shouldBe tilLovvalgBestemmelse(GrunnlagMedl.valueOf(medlemsperiode.grunnlagstype!!))
        }.size shouldBe 1
    }


    @Test
    fun hentTidligereLovvalgsperioder_ukjentGrunnlagskodeMedl_grunnlagMappetTilAnnet() {
        val medlemsperiode = lagMedlemsperiode(23L, "MAPPING_SOM_MELOSYS_IKKE_KJENNER_TIL")

        val medlDokument = MedlemskapDokument()
        medlDokument.getMedlemsperiode().add(medlemsperiode)

        val behandling = lagBehandlingMedMedlOpplysning(medlDokument)
        mockTidligereMedlemsperiodeRepository(medlemsperiode.id!!)


        lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling).shouldForAll {
            it.medlPeriodeID shouldBe medlemsperiode.id
            it.bestemmelse shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET
        }.size shouldBe 1
    }

    @Test
    fun hentTidligereLovvalgsperioder_ingenPerioderValgt_returnererTomCollection() {
        val behandling = Behandling()
        behandling.id = BEH_ID
        every { tidligereMedlemsperiodeRepository.findById_BehandlingId(BEH_ID) } returns emptyList()
        lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling).shouldBeEmpty()
    }

    @Test
    fun hentOpprinneligLovvalgsperiode_finnerOpprinneligBehandlingMedTidligerePeriode_returnererPeriode() {
        val opprinneligBehandling = Behandling().apply {
            id = 2L
        }

        val behandling = Behandling()
        behandling.opprinneligBehandling = opprinneligBehandling

        every { behandlingRepository.findById(BEH_ID) } returns Optional.of(behandling)

        val opprinneligLovvalgsperiode = Lovvalgsperiode()
        every { lovvalgsperiodeRepository.findByBehandlingsresultatId(opprinneligBehandling.id) } returns listOf(opprinneligLovvalgsperiode)

        lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(BEH_ID) shouldBe opprinneligLovvalgsperiode
    }

    @Test
    fun hentOpprinneligLovvalgsperiode_finnerIngenBehandling_kasterException() {
        every { behandlingRepository.findById(BEH_ID) } returns Optional.empty()


        shouldThrow<IkkeFunnetException> {
            lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(BEH_ID)
        }.message shouldBe "Fant ingen behandling for 1"
    }

    @Test
    fun hentOpprinneligLovvalgsperiode_finnerIkkeOpprinneligBehandling_kasterException() {
        every { behandlingRepository.findById(BEH_ID) } returns Optional.of(Behandling())


        shouldThrow<IkkeFunnetException> {
            lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(BEH_ID)
        }.message shouldBe "Fant ingen opprinnelig behandling for 1"
    }

    @Test
    fun hentOpprinneligLovvalgsperiode_finnerOpprinneligBehandlingUtenTidligerePeriode_kasterException() {
        val opprinneligBehandling = Behandling().apply {
            id = 2L
        }

        val behandling = Behandling()
        behandling.opprinneligBehandling = opprinneligBehandling

        every { behandlingRepository.findById(BEH_ID) } returns Optional.of(behandling)


        shouldThrow<IkkeFunnetException> {
            lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(BEH_ID)
        }.message shouldBe "Fant ingen opprinnelig lovvalgsperiode for 1"
    }

    @Test
    fun finnOpprinneligLovvalgsperiode_finnerOpprinneligBehandlingMedTidligerePeriode_returnererPeriode() {
        val opprinneligBehandling = Behandling().apply {
            id = 2L
        }

        val behandling = Behandling()
        behandling.opprinneligBehandling = opprinneligBehandling

        every { behandlingRepository.findById(BEH_ID) } returns Optional.of(behandling)

        val opprinneligLovvalgsperiode = Lovvalgsperiode().apply { id = 3000 }
        every { lovvalgsperiodeRepository.findByBehandlingsresultatId(opprinneligBehandling.id) } returns listOf(opprinneligLovvalgsperiode)


        lovvalgsperiodeService.finnOpprinneligLovvalgsperiode(BEH_ID).get() shouldBe opprinneligLovvalgsperiode
    }

    @Test
    fun finnOpprinneligLovvalgsperiode_finnerOpprinneligBehandlingUtenTidligerePeriode_optionalEmpty() {
        val opprinneligBehandling = Behandling().apply {
            id = 2L
        }

        val behandling = Behandling()
        behandling.opprinneligBehandling = opprinneligBehandling
        every { behandlingRepository.findById(BEH_ID) } returns Optional.of(behandling)


        lovvalgsperiodeService.finnOpprinneligLovvalgsperiode(BEH_ID).shouldNotBePresent()
    }


    private fun mockTidligereMedlemsperiodeRepository(periodeID: Long) {
        val tidligereMedlemsperiodeId = TidligereMedlemsperiodeId().apply {
            periodeId = periodeID
        }

        val tidligereMedlemsperiode = TidligereMedlemsperiode().apply {
            id = tidligereMedlemsperiodeId
        }

        every { tidligereMedlemsperiodeRepository.findById_BehandlingId(BEH_ID) } returns listOf(tidligereMedlemsperiode)
    }

    private fun lagBehandlingMedMedlOpplysning(medlDokument: MedlemskapDokument): Behandling {
        val medl = Saksopplysning()
        medl.dokument = medlDokument
        medl.type = SaksopplysningType.MEDL

        val behandling = Behandling()
        behandling.id = BEH_ID
        behandling.saksopplysninger.add(medl)
        return behandling
    }

    private fun lagMedlemsperiode(id: Long, grunnlagMedlKode: String): Medlemsperiode {
        val periode = Periode(LocalDate.now(), LocalDate.now())

        return Medlemsperiode(
            id, periode, null,
            PeriodestatusMedl.GYLD.kode, grunnlagMedlKode, null, null, null, null, null
        )
    }

    private fun harBehandlingsResultatMedRiktigId(lovvalgsperioder: Iterable<Lovvalgsperiode>): Boolean {
        return StreamSupport.stream(lovvalgsperioder.spliterator(), false)
            .allMatch { item: Lovvalgsperiode -> item.behandlingsresultat != null && item.behandlingsresultat.id == BEH_ID }
    }

    companion object {
        private const val BEH_ID = 1L
    }
}
