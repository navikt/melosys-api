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
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsaarsak
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.*
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.repository.MottatteOpplysningerRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.UtledMottaksdato
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

@ExtendWith(MockKExtension::class)
internal class MottatteOpplysningerServiceTest {
    @MockK
    private lateinit var mottatteOpplysningerRepositoryMock: MottatteOpplysningerRepository

    @MockK
    private lateinit var behandlingServiceMock: BehandlingService

    @MockK
    private lateinit var joarkFasadeMock: JoarkFasade

    private lateinit var mottatteOpplysningerServiceSpy: MottatteOpplysningerService

    private val unleash = FakeUnleash()

    @BeforeEach
    fun setup() {
        mottatteOpplysningerServiceSpy = spyk(
            MottatteOpplysningerService(
                mottatteOpplysningerRepositoryMock,
                behandlingServiceMock,
                UtledMottaksdato(joarkFasadeMock),
                unleash
            )
        )
        unleash.enableAll()
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
    fun opprettSøknadEllerAnmodningEllerAttest_toggleErAv_lagerIkkeAnmodningEllerAttest() {
        unleash.disableAll()
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
            mottaksdato.shouldBe(mottatDato)
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
            setMottatteOpplysningerdata(MottatteOpplysningerData().apply {
                periode = Periode(
                    LocalDate.of(2000, 1, 1),
                    LocalDate.of(2010, 1, 1)
                )
            })
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
            setMottatteOpplysningerdata(MottatteOpplysningerData())
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
            mottaksdato.shouldBe(mottatDato)
            mottatteOpplysningerData.shouldBeInstanceOf<SedGrunnlag>()
        }
    }

    @Test
    fun opprettSøknadFolketrygden_harPeriodeOgLand_setterPeriodeOgLandOgHarRettType() {
        val behandling = setupMock(
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.ARBEID_I_UTLANDET
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
            mottatteOpplysningerData.shouldBeInstanceOf<SoeknadFtrl>()
            this.type.shouldBe(Mottatteopplysningertyper.SØKNAD_FOLKETRYGDEN)
            this.behandling.shouldBe(behandling)
            mottaksdato.shouldBe(mottatDato)
            mottatteOpplysningerData.apply {
                this.periode.shouldBe(periode)
                this.soeknadsland.shouldBe(soeknadsland)
            }
        }
    }

    @Test
    fun opprettSøknadTrygdeavtale_harPeriodeOgLand_setterPeriodeOgLandOgHarRettType() {
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
            mottatteOpplysningerData.shouldBeInstanceOf<SoeknadTrygdeavtale>()
            this.type.shouldBe(Mottatteopplysningertyper.SØKNAD_TRYGDEAVTALE)
            this.behandling.shouldBe(behandling)
            mottaksdato.shouldBe(mottatDato)
            mottatteOpplysningerData.apply {
                this.periode.shouldBe(periode)
                this.soeknadsland.shouldBe(soeknadsland)
            }
        }
    }

    @Test
    fun opprettSøknad_tomFlyt_mottatteOpplysningerBlirIkkeOpprettet() {
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
    fun opprettSøknad_FTRL_brukbBehandlingsårsak() {
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
            mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            this.type.shouldBe(Mottatteopplysningertyper.SØKNAD_FOLKETRYGDEN)
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


        mottatteOpplysningerServiceSpy.opprettSøknadEllerAnmodningEllerAttest(Prosessinstans().apply { this.behandling = behandling })


        verify {
            mottatteOpplysningerServiceSpy.opprettSøknadEllerAnmodningEllerAttest(any(), isNull(inverse = true), isNull(inverse = true))
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
            forsendelseMottatt = mottatDato.atStartOfDay().toInstant(ZoneOffset.UTC)
        }

    private fun lagBehandling(sakstype: Sakstyper, sakstemaer: Sakstemaer, tema: Behandlingstema) =
        Behandling().apply {
            fagsak = Fagsak().apply {
                type = sakstype
                this.tema = sakstemaer
            }
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
        private val mottatDato = LocalDate.of(2003, 3, 3)
    }
}
