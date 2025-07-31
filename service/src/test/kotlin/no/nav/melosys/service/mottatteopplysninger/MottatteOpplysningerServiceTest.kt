package no.nav.melosys.service.mottatteopplysninger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsaarsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.*
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.repository.MottatteOpplysningerRepository
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.UtledMottaksdato
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

@ExtendWith(MockKExtension::class)
@ExtendWith(MockitoExtension::class)
internal class MottatteOpplysningerServiceTest {
    @MockK
    private lateinit var mottatteOpplysningerRepositoryMock: MottatteOpplysningerRepository

    @MockK
    private lateinit var behandlingServiceMock: BehandlingService

    @MockK
    private lateinit var joarkFasadeMock: JoarkFasade

    @MockK
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler

    private lateinit var mottatteOpplysningerServiceSpy: MottatteOpplysningerService

    @BeforeEach
    fun setup() {
        mottatteOpplysningerServiceSpy = spyk(
            MottatteOpplysningerService(
                mottatteOpplysningerRepositoryMock,
                behandlingServiceMock,
                UtledMottaksdato(joarkFasadeMock),
                saksbehandlingRegler
            )
        )
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
    }

    @Test
    fun hentMottatteOpplysningerForBehandlingID_finnes_returnerMottatteOpplysninger() {
        every { mottatteOpplysningerRepositoryMock.findByBehandling_Id(behandlingID) } returns Optional.of(
            MottatteOpplysninger()
        )

        mottatteOpplysningerServiceSpy.hentMottatteOpplysninger(behandlingID).shouldNotBeNull()
    }

    @Test
    fun hentMottatteOpplysningerForBehandlingID_finnesIkke_kastException() {
        every { mottatteOpplysningerRepositoryMock.findByBehandling_Id(1) } returns Optional.empty()

        shouldThrow<IkkeFunnetException> {
            mottatteOpplysningerServiceSpy.hentMottatteOpplysninger(1)
        }.shouldHaveMessage("Finner ikke mottatteOpplysninger for behandling 1")
    }

    @Test
    fun hentEllerOpprettMottatteOpplysninger_finnesIkkeAktivBehandling_opprettMottatteOpplysninger() {
        every { mottatteOpplysningerRepositoryMock.findByBehandling_Id(1) } returns Optional.empty()
    }

    @Test
    fun hentEllerOpprettMottatteOpplysninger_finnesIkkeIngenFlyt_kastException() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns true
        every { mottatteOpplysningerRepositoryMock.findByBehandling_Id(behandlingID) } returns Optional.empty()
        every { behandlingServiceMock.hentBehandling(behandlingID) } returns lagBehandling(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        ).apply {
            status = Behandlingsstatus.OPPRETTET
        }

        shouldThrow<IkkeFunnetException> {
            mottatteOpplysningerServiceSpy.hentEllerOpprettMottatteOpplysninger(behandlingID, true)
        }.shouldHaveMessage("Finner ikke mottatteOpplysninger for behandling ${behandlingID}")
    }

    @Test
    fun hentEllerOpprettMottatteOpplysninger_saksbehandlerKanIkkeRedigereBehandling_kastException() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { mottatteOpplysningerRepositoryMock.findByBehandling_Id(behandlingID) } returns Optional.empty()
        every { behandlingServiceMock.hentBehandling(behandlingID) } returns lagBehandling(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        ).apply {
            status = Behandlingsstatus.AVSLUTTET
        }

        shouldThrow<IkkeFunnetException> {
            mottatteOpplysningerServiceSpy.hentEllerOpprettMottatteOpplysninger(behandlingID, false)
        }.shouldHaveMessage("Finner ikke mottatteOpplysninger for behandling ${behandlingID}")
    }

    @Test
    fun opprettSøknadEllerAnmodningEllerAttest_ingenFlyt_lagerIkkeAnmodningEllerAttest() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns true
        val prosessinstans = Prosessinstans().apply {
            behandling = lagBehandling(
                Sakstyper.EU_EOS,
                Sakstemaer.UNNTAK,
                Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR
            )
        }


        mottatteOpplysningerServiceSpy.opprettSøknadEllerAnmodningEllerAttest(prosessinstans)


        verify(exactly = 0) {
            mottatteOpplysningerRepositoryMock.save(any())
        }
    }

    @Test
    fun opprettSøknadEllerAnmodningEllerAttest_erAnmodningOmUnntakEllerRegistreringUnntak_lagerAnmodningEllerAttest() {
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns true
        val prosessinstans = Prosessinstans().apply {
            behandling = setupMock(
                Sakstyper.EU_EOS,
                Sakstemaer.UNNTAK,
                Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR
            )
        }


        mottatteOpplysningerServiceSpy.opprettSøknadEllerAnmodningEllerAttest(prosessinstans)


        val slot = slot<MottatteOpplysninger>()
        verify {
            mottatteOpplysningerRepositoryMock.save(capture(slot))
        }
        slot.captured.apply {
            type.shouldBe(Mottatteopplysningertyper.ANMODNING_ELLER_ATTEST)
            mottatteOpplysningerData.shouldBeInstanceOf<AnmodningEllerAttest>()
        }
    }

    @Test
    fun opprettSøknadEllerAnmodningEllerAttest_erIkkeAnmodningOmUnntakEllerRegistreringUnntak_lagerIkkeAnmodningEllerAttest() {
        val prosessinstans = Prosessinstans().apply {
            behandling = setupMock(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                Behandlingstema.UTSENDT_ARBEIDSTAKER
            )
        }


        mottatteOpplysningerServiceSpy.opprettSøknadEllerAnmodningEllerAttest(prosessinstans)


        val slot = slot<MottatteOpplysninger>()
        verify {
            mottatteOpplysningerRepositoryMock.save(capture(slot))
        }
        slot.captured.apply {
            type.shouldNotBe(Mottatteopplysningertyper.ANMODNING_ELLER_ATTEST)
            mottatteOpplysningerData.shouldNotBeInstanceOf<AnmodningEllerAttest>()
        }
    }

    @Test
    fun opprettEøsSøknadGrunnlag_finnesIkkeFraFør_blirOpprettet() {
        val behandling = setupMock(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.UTSENDT_ARBEIDSTAKER
        )
        val periode = Periode()
        val soeknadsland = Soeknadsland()


        mottatteOpplysningerServiceSpy.opprettSøknadEllerAnmodningEllerAttest(behandling, periode, soeknadsland)


        val slot = slot<MottatteOpplysninger>()
        verify {
            mottatteOpplysningerRepositoryMock.save(capture(slot))
        }
        slot.captured.apply {
            type.shouldBe(Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS)
            mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            mottatteOpplysningerData.apply {
                periode.shouldBe(periode)
                soeknadsland.shouldBe(soeknadsland)
            }
            mottaksdato.shouldBe(mottattDato)
        }
    }

    @Test
    fun oppdaterMottatteOpplysningerJson_mottatteopplysningerEksisterer_oppdatererMottatteOpplysningerData() {
        val originalJsonData = "Dette skal erstattes"
        val mottatteOpplysninger = MottatteOpplysninger().apply {
            jsonData = originalJsonData
        }
        val soeknadJsonNode = Soeknad().toJsonNode
        every { mottatteOpplysningerRepositoryMock.findByBehandling_Id(behandlingID) } returns Optional.of(
            mottatteOpplysninger
        )
        every { mottatteOpplysningerRepositoryMock.saveAndFlush(any()) } returns mockk()


        mottatteOpplysningerServiceSpy.oppdaterMottatteOpplysninger(behandlingID, soeknadJsonNode)


        verify(exactly = 1) { mottatteOpplysningerRepositoryMock.saveAndFlush(any()) }
        mottatteOpplysninger.jsonData.shouldNotBe(originalJsonData)
        mottatteOpplysninger.jsonData.shouldBe(soeknadJsonNode.toPrettyString())
    }

    @Test
    fun oppdaterMottatteOpplysninger_mottatteopplysningerJsonDataIkkeSatt_setterJsonDataOgLagrerMottatteOpplysninger() {
        val mottatteOpplysninger = MottatteOpplysninger().apply {
            mottatteOpplysningerData = MottatteOpplysningerData().apply {
                periode = Periode(
                    LocalDate.of(2000, 1, 1),
                    LocalDate.of(2010, 1, 1)
                )
            }
        }
        every { mottatteOpplysningerRepositoryMock.saveAndFlush(any()) } returns mockk()


        mottatteOpplysningerServiceSpy.oppdaterMottatteOpplysninger(mottatteOpplysninger)


        val slot = slot<MottatteOpplysninger>()
        verify {
            mottatteOpplysningerRepositoryMock.saveAndFlush(capture(slot))
        }
        slot.captured.apply {
            jsonData.toJsonNode["periode"].apply {
                this["fom"].toString().shouldBe("[2000,1,1]")
                this["tom"].toString().shouldBe("[2010,1,1]")
            }
        }
    }

    @Test
    fun oppdaterMottatteOpplysningerPeriodeOgLand_eksisterer_oppdatererPeriodeOgLand() {
        val mottatteOpplysninger = MottatteOpplysninger().apply {
            mottatteOpplysningerData = MottatteOpplysningerData()
        }
        val periode = Periode(
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 12, 31)
        )
        val soeknadsland = Soeknadsland(listOf("UK"), false)
        every { mottatteOpplysningerRepositoryMock.findByBehandling_Id(behandlingID) } returns Optional.of(
            mottatteOpplysninger
        )
        every { mottatteOpplysningerRepositoryMock.saveAndFlush(any()) } returns mockk()


        mottatteOpplysningerServiceSpy.oppdaterMottatteOpplysningerPeriodeOgLand(behandlingID, periode, soeknadsland)


        val slot = slot<MottatteOpplysninger>()
        verify {
            mottatteOpplysningerRepositoryMock.saveAndFlush(capture(slot))
        }
        slot.captured.apply {
            mottatteOpplysningerData.apply {
                this.periode.shouldBe(periode)
                this.soeknadsland.shouldBe(soeknadsland)
            }
        }
    }

    @Test
    fun opprettSedGrunnlag_harRettType() {
        val behandling = setupMock(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )


        mottatteOpplysningerServiceSpy.opprettSedGrunnlag(behandlingID, SedGrunnlag())


        val slot = slot<MottatteOpplysninger>()
        verify {
            mottatteOpplysningerRepositoryMock.save(capture(slot))
        }
        slot.captured.apply {
            type.shouldBe(Mottatteopplysningertyper.SED)
            behandling.shouldBe(behandling)
            mottaksdato.shouldBe(mottattDato)
            mottatteOpplysningerData.shouldBeInstanceOf<SedGrunnlag>()
        }
    }

    @Test
    fun opprettSøknadFolketrygden_harPeriodeOgLand_setterPeriodeOgLandOgHarRettType() {
        val behandling = setupMock(
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        )
        val periode = Periode(
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 12, 31)
        )
        val soeknadsland = Soeknadsland(listOf("UK"), false)


        mottatteOpplysningerServiceSpy.opprettSøknadEllerAnmodningEllerAttest(behandling, periode, soeknadsland)


        val slot = slot<MottatteOpplysninger>()
        verify {
            mottatteOpplysningerRepositoryMock.save(capture(slot))
        }
        slot.captured.apply {
            mottatteOpplysningerData.shouldBeInstanceOf<SøknadNorgeEllerUtenforEØS>()
            this.type.shouldBe(Mottatteopplysningertyper.SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS)
            this.behandling.shouldBe(behandling)
            mottaksdato.shouldBe(mottattDato)
            mottatteOpplysningerData.apply {
                this.periode.shouldBe(periode)
                this.soeknadsland.shouldBe(soeknadsland)
            }
        }
    }

    @Test
    fun opprettSøknadForTrygdeavtale_harPeriodeOgLand_setterPeriodeOgLandOgHarRettType() {
        val behandling = lagBehandling(
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        )
        val periode = Periode(
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 12, 31)
        )
        val soeknadsland = Soeknadsland(listOf("UK"), false)
        setupMock(behandling)


        mottatteOpplysningerServiceSpy.opprettSøknadEllerAnmodningEllerAttest(behandling, periode, soeknadsland)


        val slot = slot<MottatteOpplysninger>()
        verify {
            mottatteOpplysningerRepositoryMock.save(capture(slot))
        }
        slot.captured.apply {
            mottatteOpplysningerData.shouldBeInstanceOf<SøknadNorgeEllerUtenforEØS>()
            this.type.shouldBe(Mottatteopplysningertyper.SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS)
            this.behandling.shouldBe(behandling)
            mottaksdato.shouldBe(mottattDato)
            mottatteOpplysningerData.apply {
                this.periode.shouldBe(periode)
                this.soeknadsland.shouldBe(soeknadsland)
            }
        }
    }

    @Test
    fun opprettSøknad_ingenFlyt_mottatteOpplysningerBlirIkkeOpprettet() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns true
        val behandling = lagBehandling(
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )


        mottatteOpplysningerServiceSpy.opprettSøknadEllerAnmodningEllerAttest(behandling, null, null)


        verify {
            behandlingServiceMock wasNot Called
            mottatteOpplysningerRepositoryMock wasNot Called
            joarkFasadeMock wasNot Called
        }
    }

    @Test
    fun opprettSøknad_mottatteOpplysningerBlirOpprettet() {
        val behandling = setupMock(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        )


        mottatteOpplysningerServiceSpy.opprettSøknadEllerAnmodningEllerAttest(behandling, null, null)


        val slot = slot<MottatteOpplysninger>()
        verify {
            mottatteOpplysningerRepositoryMock.save(capture(slot))
        }
        slot.captured.apply {
            mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            this.type.shouldBe(Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS)
            this.behandling.shouldBe(behandling)
        }
    }

    @Test
    fun opprettSøknadForFTRL_brukBehandlingsårsak() {
        val dagensDato = LocalDate.now()
        val behandling = setupMock(
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        ).apply {
            behandlingsårsak = Behandlingsaarsak().apply {
                mottaksdato = dagensDato
            }
        }


        mottatteOpplysningerServiceSpy.opprettSøknadEllerAnmodningEllerAttest(behandling, null, null)


        val slot = slot<MottatteOpplysninger>()
        verify {
            mottatteOpplysningerRepositoryMock.save(capture(slot))
            joarkFasadeMock wasNot Called
        }
        slot.captured.apply {
            mottatteOpplysningerData.shouldBeInstanceOf<SøknadNorgeEllerUtenforEØS>()
            this.type.shouldBe(Mottatteopplysningertyper.SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS)
            this.behandling.shouldBe(behandling)
            mottaksdato.shouldBe(dagensDato)
        }
    }

    @Test
    fun `default objekter skal lages for periode og land`() {
        val behandling = setupMock(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        )


        mottatteOpplysningerServiceSpy.opprettSøknadEllerAnmodningEllerAttest(
            Prosessinstans().apply {
                this.behandling = behandling
            })


        verify {
            mottatteOpplysningerServiceSpy.opprettSøknadEllerAnmodningEllerAttest(
                any(),
                isNull(inverse = true),
                isNull(inverse = true)
            )
        }
    }

    @Test
    fun opprettMottatteOpplysninger_aarsavergninger_mottatteOpplysningerBlirOpprettet() {
        val behandling = lagBehandling(
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV
        )
        setupMock(behandling)
        every { behandlingServiceMock.hentBehandling(behandlingID) } returns behandling


        mottatteOpplysningerServiceSpy.opprettMottatteopplysningerForAarsavregning(behandling.id)


        val slot = slot<MottatteOpplysninger>()
        verify {
            mottatteOpplysningerRepositoryMock.save(capture(slot))
        }
        slot.captured.apply {
            mottatteOpplysningerData.shouldBeInstanceOf<SøknadNorgeEllerUtenforEØS>()
            this.type.shouldBe(Mottatteopplysningertyper.SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS)
            this.behandling.shouldBe(behandling)
        }
    }

    private fun setupMock(behandling: Behandling) {
        every { behandlingServiceMock.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { joarkFasadeMock.hentJournalpost(behandling.initierendeJournalpostId) } returns lagJournalpost(behandling)
        every { mottatteOpplysningerRepositoryMock.save(any()) } returns mockk()
    }

    private fun setupMock(sakstype: Sakstyper, sakstemaer: Sakstemaer, tema: Behandlingstema): Behandling =
        lagBehandling(sakstype, sakstemaer, tema).apply {
            setupMock(this)
        }


    private fun lagJournalpost(behandling: Behandling) =
        Journalpost(behandling.initierendeJournalpostId).apply {
            forsendelseMottatt = mottattDato.atStartOfDay().toInstant(ZoneOffset.UTC)
        }

    private fun lagBehandling(sakstype: Sakstyper, sakstemaer: Sakstemaer, tema: Behandlingstema) =
        Behandling.forTest {
            fagsak = FagsakTestFactory.builder()
                .type(sakstype)
                .tema(sakstemaer)
                .build()
            id = behandlingID
            initierendeJournalpostId = "123321"
            type = Behandlingstyper.FØRSTEGANG
            this.tema = tema
        }

    private val String.toJsonNode: JsonNode
        get() {
            return ObjectMapper().readTree(this)
        }

    private val Any.toJsonNode: JsonNode
        get() {
            return ObjectMapper().valueToTree(this)
        }

    companion object {
        private const val behandlingID: Long = 123332211
        private val mottattDato = LocalDate.of(2003, 3, 3)
    }
}
