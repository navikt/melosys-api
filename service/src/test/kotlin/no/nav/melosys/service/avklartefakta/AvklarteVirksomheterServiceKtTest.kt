package no.nav.melosys.service.avklartefakta

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.adresse.Adresse
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.service.MottatteOpplysningerStub.lagMottatteOpplysninger
import no.nav.melosys.service.SaksopplysningStubs.lagArbeidsforholdOpplysninger
import no.nav.melosys.service.SaksopplysningStubs.lagOrganisasjonDokumenter
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.function.Function

@ExtendWith(MockKExtension::class)
class AvklarteVirksomheterServiceKtTest {

    @MockK
    private lateinit var avklartefaktaService: AvklartefaktaService

    @MockK
    private lateinit var organisasjonOppslagService: OrganisasjonOppslagService

    @MockK
    private lateinit var mockKodeverkService: KodeverkService

    @RelaxedMockK
    private lateinit var behandlingService: BehandlingService

    private lateinit var behandling: Behandling
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    @BeforeEach
    fun setUp() {
        behandling = createTestBehandling()
        setupDefaultMocks()
        avklarteVirksomheterService = AvklarteVirksomheterService(
            avklartefaktaService,
            organisasjonOppslagService,
            behandlingService,
            mockKodeverkService
        )
    }

    private fun createTestBehandling(id: Long = 1L) = Behandling.forTest {
        this.id = id
    }

    private fun setupDefaultMocks() {
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns setOf(ORGNR_1, UUID_1)
        every { mockKodeverkService.dekod(any(), any()) } returns "Poststed"
        every { organisasjonOppslagService.hentOrganisasjoner(any()) } returns emptySet()
        // Default mock for behandlingService to return the current behandling
        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
    }

    @Test
    fun `hentUtenlandskeVirksomheter gir liste med kun avklarte foretak`() {
        val foretakUtland1 = lagForetakUtland("Utland1", UUID_1, null)
        val foretakUtlandListe = listOf(foretakUtland1)
        val saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), foretakUtlandListe, emptyList())

        val utenlandskeVirksomheter = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling)

        utenlandskeVirksomheter.shouldHaveSize(1)
        utenlandskeVirksomheter.first().navn shouldBe "Utland1"
    }

    @Test
    fun `hentUtenlandskeVirksomheter gir liste avklart virksomhet med orgnr ikke uuid`() {
        val foretakUtland = lagForetakUtland("Utland1", UUID_1, "SE-123456789")
        val foretakUtlandListe = listOf(foretakUtland)
        val saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), foretakUtlandListe, emptyList())

        val utenlandskeVirksomheter = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling)

        utenlandskeVirksomheter.shouldHaveSize(1)
        utenlandskeVirksomheter.first().orgnr shouldBe "SE-123456789"
    }

    @Test
    fun `harOpphørtAvklartVirksomhet ingen opphorsdato gir false`() {
        val foretakUtland = lagForetakUtland("Test Foretak", UUID_1, "SE-123456789")
        val foretakUtlandListe = listOf(foretakUtland)
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), foretakUtlandListe, emptyList())

        val harOpphørt = avklarteVirksomheterService.harOpphørtAvklartVirksomhet(behandling)

        harOpphørt shouldBe false
    }

    @Test
    fun `lagreVirksomheterSomAvklartefakta virksomhetIDer gyldig virksomheter lagret`() {
        every { avklartefaktaService.slettAvklarteFakta(any(), any()) } returns Unit
        every { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) } returns Unit
        val foretakUtland = lagForetakUtland("Test", UUID_1, null)
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), listOf(foretakUtland), emptyList())
        val virksomhetIDer = listOf(UUID_1)

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer)

        verify { avklartefaktaService.slettAvklarteFakta(any(), any()) }
        verify { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `hentAntallAvklarteVirksomheter summererArbeidsgivereOgSelvstendigNæringsdrivendeINorgeOgUtenlandskeVirksomheter`() {
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns setOf(ORGNR_1, ORGNR_2, ORGNR_3, ORGNR_4, UUID_1)
        val foretakUtland1 = lagForetakUtland("Utland1", UUID_1, null)
        val foretakUtland2 = lagForetakUtland("Utland2", UUID_1, "SE-123456789")
        val foretakUtlandListe = listOf(foretakUtland1, foretakUtland2)
        val selvstendigeForetak = listOf(ORGNR_1, ORGNR_2)
        val arbeidgivendeEkstraOrgnumre = listOf(ORGNR_3, ORGNR_4)
        val saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(selvstendigeForetak, foretakUtlandListe, arbeidgivendeEkstraOrgnumre)

        val antall = avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling)

        antall shouldBe EXPECTED_TOTAL_VIRKSOMHETER
    }




    @Test
    fun `lagreVirksomheterSomAvklartefakta virksomhetErForetakUtland valideringOKOgVirksomhetLagret`() {
        every { avklartefaktaService.slettAvklarteFakta(any(), any()) } returns Unit
        every { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) } returns Unit
        val foretakUtland = lagForetakUtland("Utlandsk AS", UUID_1, null)
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), listOf(foretakUtland), emptyList())
        behandling.saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        val virksomhetIDer = listOf(UUID_1)

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer)

        verify { avklartefaktaService.leggTilAvklarteFakta(1L, any(), any(), UUID_1, any()) }
    }

    @Test
    fun `lagreVirksomheterSomAvklartefakta virksomhetErSelvstendigForetak valideringOKOgVirksomhetLagret`() {
        every { avklartefaktaService.slettAvklarteFakta(any(), any()) } returns Unit
        every { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) } returns Unit
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(listOf(ORGNR_1), emptyList(), emptyList())
        behandling.saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        val virksomhetIDer = listOf(ORGNR_1)

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer)

        verify { avklartefaktaService.leggTilAvklarteFakta(1L, any(), any(), ORGNR_1, any()) }
    }

    @Test
    fun `lagreVirksomheterSomAvklartefakta virksomhetErLagtInnManuelt valideringOKOgVirksomhetLagret`() {
        every { avklartefaktaService.slettAvklarteFakta(any(), any()) } returns Unit
        every { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) } returns Unit
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), emptyList(), listOf(ORGNR_3))
        behandling.saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        val virksomhetIDer = listOf(ORGNR_3)

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer)

        verify { avklartefaktaService.leggTilAvklarteFakta(1L, any(), any(), ORGNR_3, any()) }
    }

    @Test
    fun `lagreVirksomheterSomAvklartefakta virksomhetErArbeidNorge valideringOKOgVirksomhetLagret`() {
        every { avklartefaktaService.slettAvklarteFakta(any(), any()) } returns Unit
        every { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) } returns Unit
        val saksopplysninger = lagArbeidsforholdOpplysninger(listOf(ORGNR_1))
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), emptyList(), listOf(ORGNR_1))
        val virksomhetIDer = listOf(ORGNR_1)

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer)

        verify { avklartefaktaService.leggTilAvklarteFakta(1L, any(), any(), ORGNR_1, any()) }
    }

    @Test
    fun `lagreVirksomheterSomAvklartefakta virksomhetErUgyldig valideringFailerOgVirksomhetIkkeLagret`() {
        every { avklartefaktaService.slettAvklarteFakta(any(), any()) } returns Unit
        // Ensure the behandling is properly set up with mottatte opplysninger and saksopplysninger
        val behandlingWithData = Behandling.forTest {
            this.id = 1L
        }
        behandlingWithData.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), emptyList(), emptyList())
        behandlingWithData.saksopplysninger = lagArbeidsforholdOpplysninger(emptyList()) // Add empty arbeidsforhold
        every { behandlingService.hentBehandlingMedSaksopplysninger(1L) } returns behandlingWithData

        val ugyldigVirksomhetId = "999999999"
        val virksomhetIDer = listOf(ugyldigVirksomhetId)

        shouldThrow<no.nav.melosys.exception.FunksjonellException> {
            avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer)
        }.message shouldContain "VirksomhetID $ugyldigVirksomhetId hører ikke til noen av arbeidsforholdene"

        verify(exactly = 0) { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) }
    }


    private fun lagForetakUtland(navn: String, uuid: String, orgnr: String?) = ForetakUtland().apply {
        this.navn = navn
        this.uuid = uuid
        this.orgnr = orgnr
    }

    private fun leggTilIRegisterOppslag(orgnumre: Collection<String>) {
        every { organisasjonOppslagService.hentOrganisasjoner(setOf(*orgnumre.toTypedArray())) } returns lagOrganisasjonDokumenter(orgnumre)
    }


    @Test
    fun `utfyllManglendeAdressefelter gyldigForretningsadresse girForretningsadresse`() {
        val organisasjonDokument = lagOrganisasjonDokument("2345", "Forretningsgatenavn")

        val adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(organisasjonDokument)

        adresse.run {
            gatenavn shouldBe "Forretningsgatenavn"
            postnummer shouldBe "2345"
            poststed shouldBe "Poststed"
            landkode shouldBe "NO"
        }

        verify { mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "2345") }
    }

    @Test
    fun `utfyllManglendeAdressefelter forretningsadresseManglerGatenavn girForretningsadresseMedBlanktGatenavn`() {
        val organisasjonDokument = lagOrganisasjonDokument("2345", null)

        val adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(organisasjonDokument)

        adresse.run {
            gatenavn shouldBe " "
            postnummer shouldBe "2345"
            poststed shouldBe "Poststed"
            landkode shouldBe "NO"
        }

        verify { mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "2345") }
    }

    @Test
    fun `utfyllManglendeAdressefelter utenlandskIngenForretningsadressePostadresseUtenPostnummer postnummerTomString`() {
        val organisasjonDokument = lagOrganisasjonDokument(null, null, null, "DK")
        organisasjonDokument.organisasjonDetaljer.forretningsadresse = emptyList()
        organisasjonDokument.organisasjonDetaljer.postadresse.firstOrNull()?.let { adresse ->
            (adresse as no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse).postnr = null
        }

        val adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(organisasjonDokument)

        adresse.run {
            gatenavn shouldBe "Postgatenavn"
            postnummer shouldBe " "
            poststed shouldBe "Postpoststed"
            landkode shouldBe "DK"
        }

        verify(exactly = 0) { mockKodeverkService.dekod(any(), any()) }
    }

    @Test
    fun `utfyllManglendeAdressefelter forretningsadresseManglerPostnr girPostadresse`() {
        val organisasjonDokument = lagOrganisasjonDokument(null, null)

        val adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(organisasjonDokument)

        adresse.run {
            gatenavn shouldBe "Postgatenavn"
            postnummer shouldBe "6789"
            poststed shouldBe "Poststed"
            landkode shouldBe "NO"
        }

        verify { mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "6789") }
    }

    @Test
    fun `hentSelvstendigeForetakOrgnumre girListeMedKunAvklarteOrgnumre`() {
        val selvstendigeForetak = listOf(ORGNR_1, ORGNR_2)
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(selvstendigeForetak, emptyList(), emptyList())

        val avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentNorskeSelvstendigeForetakOrgnumre(behandling)

        avklarteSelvstendigeOrgnumre shouldContainExactlyInAnyOrder setOf(ORGNR_1)
    }

    @Test
    fun `hentArbeidsgivendeEkstraOrgnumre girListeMedKunAvklarteOrgnumre`() {
        val arbeidgivendeEkstraOrgnumre = listOf(ORGNR_2, ORGNR_1)
        val saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), emptyList(), arbeidgivendeEkstraOrgnumre)

        val avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)

        avklarteSelvstendigeOrgnumre shouldContainExactlyInAnyOrder setOf(ORGNR_1)
    }

    @Test
    fun `hentArbeidsgivendeRegistreOrgnumre girListeMedKunAvklarteOrgnumre`() {
        val arbeidgivendeOrgnumreEkstra = listOf(ORGNR_1, ORGNR_2, ORGNR_3)
        val saksopplysninger = lagArbeidsforholdOpplysninger(arbeidgivendeOrgnumreEkstra)
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), emptyList(), emptyList())

        val avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)

        avklarteSelvstendigeOrgnumre shouldContainExactlyInAnyOrder setOf(ORGNR_1)
    }

    @Test
    fun `testHentAvklarteNorskeForetak girAvklarteArbeidsgivere`() {
        val arbeidsgivereEkstra = listOf(ORGNR_2)
        val arbeidsgivereRegister = listOf(ORGNR_3)

        val saksopplysninger = lagArbeidsforholdOpplysninger(arbeidsgivereRegister)
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), emptyList(), arbeidsgivereEkstra)

        val avklarteOrganisasjoner = setOf(ORGNR_2, ORGNR_3)
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns avklarteOrganisasjoner

        leggTilIRegisterOppslag(listOf(ORGNR_2, ORGNR_3))

        val norskeVirksomheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, INGEN_ADRESSE)

        norskeVirksomheter.map { it.orgnr } shouldContainExactlyInAnyOrder listOf(ORGNR_2, ORGNR_3)
    }

    @Test
    fun `hentAlleNorskeVirksomheter SammeOrgNummerFraNorskeArbeidsgivereOgNorskeSelvstendigeForetak UnngåDuplikater`() {
        val arbeidsgivereRegister = listOf(ORGNR_3)

        val saksopplysninger = lagArbeidsforholdOpplysninger(arbeidsgivereRegister)
        behandling.saksopplysninger = saksopplysninger
        val mottatteOpplysninger = lagMottatteOpplysninger(arbeidsgivereRegister, emptyList(), emptyList())
        behandling.mottatteOpplysninger = mottatteOpplysninger

        val avklarteOrganisasjoner = setOf(ORGNR_3)
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns avklarteOrganisasjoner

        leggTilIRegisterOppslag(arbeidsgivereRegister)

        val norskeVirksomheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, INGEN_ADRESSE)

        norskeVirksomheter shouldHaveSize 1
        norskeVirksomheter.first().orgnr shouldBe ORGNR_3
    }

    @Test
    fun `hentAvklarteNorskeForetak girAvklarteSelvstendigeForetak`() {
        val selvstendigeForetak = listOf(ORGNR_1)

        val saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(selvstendigeForetak, emptyList(), emptyList())

        val avklarteOrganisasjoner = setOf(ORGNR_1)
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns avklarteOrganisasjoner

        leggTilIRegisterOppslag(selvstendigeForetak)

        val norskeVirksomheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, INGEN_ADRESSE)

        norskeVirksomheter.map { it.orgnr } shouldContainExactlyInAnyOrder listOf(ORGNR_1)
    }

    @Test
    fun `harOpphørtAvklartVirksomhet opphoersdatoTilbakeITid girTrue`() {
        val orgDok = lagOrganisasjonDokument("0011", "Gatenavn 1")
        orgDok.organisasjonDetaljer.opphoersdato = java.time.LocalDate.now().minusYears(1)
        every { organisasjonOppslagService.hentOrganisasjoner(any()) } returns setOf(orgDok)

        behandling.saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), emptyList(), emptyList())

        val harOpphørtAvklartVirksomhet = avklarteVirksomheterService.harOpphørtAvklartVirksomhet(behandling)

        harOpphørtAvklartVirksomhet shouldBe true
    }

    private fun lagOrganisasjonDokument(
        forretningsPostnr: String?,
        forretningsGatenavn: String?,
        postadressePostnr: String? = "6789",
        postadresseLand: String = "NO"
    ): no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument {
        val detaljer = no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer().apply {
            forretningsadresse = listOf(
                no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse().apply {
                    adresselinje1 = forretningsGatenavn
                    postnr = forretningsPostnr
                    poststed = "Forretningspoststed"
                    landkode = "NO"
                    gyldighetsperiode = no.nav.melosys.domain.dokument.felles.Periode(
                        java.time.LocalDate.now().minusDays(1),
                        java.time.LocalDate.now().plusDays(1)
                    )
                }
            )
            postadresse = listOf(
                no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse().apply {
                    adresselinje1 = "Postgatenavn"
                    postnr = postadressePostnr
                    poststed = "Postpoststed"
                    landkode = postadresseLand
                    gyldighetsperiode = no.nav.melosys.domain.dokument.felles.Periode(
                        java.time.LocalDate.now().minusDays(1),
                        java.time.LocalDate.now().plusDays(1)
                    )
                }
            )
        }
        return no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument(
            orgnummer = "123456789",
            navn = "Test Org",
            organisasjonDetaljer = detaljer,
            sektorkode = "1"
        )
    }
    
    companion object {
        private const val ORGNR_1 = "111111111"
        private const val ORGNR_2 = "222222222"
        private const val ORGNR_3 = "333333333"
        private const val ORGNR_4 = "444444444"
        private const val UUID_1 = "a2k2jf-a3khs"

        // Constants for test expectations
        private const val EXPECTED_SELVSTENDIGE = 2
        private const val EXPECTED_ARBEIDSGIVERE = 2
        private const val EXPECTED_UTENLANDSKE = 2
        private const val EXPECTED_TOTAL_VIRKSOMHETER = EXPECTED_SELVSTENDIGE + EXPECTED_ARBEIDSGIVERE + EXPECTED_UTENLANDSKE

        val INGEN_ADRESSE: Function<OrganisasjonDokument, Adresse?> = Function { null }
    }
}
