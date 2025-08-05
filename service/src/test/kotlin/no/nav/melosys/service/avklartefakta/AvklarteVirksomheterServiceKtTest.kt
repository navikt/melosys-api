package no.nav.melosys.service.avklartefakta

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.FellesKodeverk
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

    @MockK
    private lateinit var behandlingService: BehandlingService

    private lateinit var behandling: Behandling
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    private val orgnr1 = "111111111"
    private val orgnr2 = "222222222"
    private val orgnr3 = "333333333"
    private val orgnr4 = "444444444"
    private val uuid1 = "a2k2jf-a3khs"
    private val uuid2 = "0dkf93-kj701"

    @BeforeEach
    fun setUp() {
        behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .build()
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns setOf(orgnr1, uuid1)
        every { mockKodeverkService.dekod(any(), any()) } returns "Poststed"
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } answers { 
            // Return the current behandling instance which may have been modified in individual tests
            behandling 
        }
        every { organisasjonOppslagService.hentOrganisasjoner(any()) } returns emptySet()

        avklarteVirksomheterService = AvklarteVirksomheterService(
            avklartefaktaService,
            organisasjonOppslagService,
            behandlingService,
            mockKodeverkService
        )
    }

    @Test
    fun hentUtenlandskeVirksomheter_girListeMedKunAvklarteForetak() {
        val foretakUtland1 = lagForetakUtland("Utland1", uuid1, null)
        val foretakUtlandListe = listOf(foretakUtland1)
        val saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), foretakUtlandListe, emptyList())

        val utenlandskeVirksomheter = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling)

        utenlandskeVirksomheter.shouldHaveSize(1)
        utenlandskeVirksomheter.first().navn shouldBe "Utland1"
    }

    @Test
    fun hentUtenlandskeVirksomheter_girListeAvklartVirksomhetMedOrgnrIkkeUuid() {
        val foretakUtland = lagForetakUtland("Utland1", uuid1, "SE-123456789")
        val foretakUtlandListe = listOf(foretakUtland)
        val saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), foretakUtlandListe, emptyList())

        val utenlandskeVirksomheter = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling)

        utenlandskeVirksomheter.shouldHaveSize(1)
        utenlandskeVirksomheter.first().orgnr shouldBe "SE-123456789"
    }

    @Test
    fun harOpphørtAvklartVirksomhet_ingenOpphørsdato_girFalse() {
        val foretakUtland = lagForetakUtland("Test Foretak", uuid1, "SE-123456789")
        val foretakUtlandListe = listOf(foretakUtland)
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), foretakUtlandListe, emptyList())

        val harOpphørt = avklarteVirksomheterService.harOpphørtAvklartVirksomhet(behandling)

        harOpphørt shouldBe false
    }

    @Test
    fun lagreVirksomheterSomAvklartefakta_virksomhetIDerGyldig_virksomheterLagret() {
        every { avklartefaktaService.slettAvklarteFakta(any(), any()) } returns Unit
        every { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) } returns Unit
        val foretakUtland = lagForetakUtland("Test", uuid1, null)
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), listOf(foretakUtland), emptyList())
        val virksomhetIDer = listOf(uuid1)

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer)

        verify { avklartefaktaService.slettAvklarteFakta(any(), any()) }
        verify { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `hentAntallAvklarteVirksomheter summererArbeidsgivereOgSelvstendigNæringsdrivendeINorgeOgUtenlandskeVirksomheter`() {
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns setOf(orgnr1, orgnr2, orgnr3, orgnr4, uuid1)
        val foretakUtland1 = lagForetakUtland("Utland1", uuid1, null)
        val foretakUtland2 = lagForetakUtland("Utland2", uuid1, "SE-123456789")
        val foretakUtlandListe = listOf(foretakUtland1, foretakUtland2)
        val selvstendigeForetak = listOf(orgnr1, orgnr2)
        val arbeidgivendeEkstraOrgnumre = listOf(orgnr3, orgnr4)
        val saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(selvstendigeForetak, foretakUtlandListe, arbeidgivendeEkstraOrgnumre)

        val antall = avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling)

        antall shouldBe 6 // 2 selvstendige + 2 arbeidsgivere + 2 utenlandske (both have uuid1)
    }




    @Test
    fun `lagreVirksomheterSomAvklartefakta virksomhetErForetakUtland valideringOKOgVirksomhetLagret`() {
        every { avklartefaktaService.slettAvklarteFakta(any(), any()) } returns Unit
        every { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) } returns Unit
        val foretakUtland = lagForetakUtland("Utlandsk AS", uuid1, null)
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), listOf(foretakUtland), emptyList())
        behandling.saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        val virksomhetIDer = listOf(uuid1)

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer)

        verify { avklartefaktaService.leggTilAvklarteFakta(1L, any(), any(), eq(uuid1), any()) }
    }

    @Test
    fun `lagreVirksomheterSomAvklartefakta virksomhetErSelvstendigForetak valideringOKOgVirksomhetLagret`() {
        every { avklartefaktaService.slettAvklarteFakta(any(), any()) } returns Unit
        every { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) } returns Unit
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(listOf(orgnr1), emptyList(), emptyList())
        behandling.saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        val virksomhetIDer = listOf(orgnr1)

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer)

        verify { avklartefaktaService.leggTilAvklarteFakta(1L, any(), any(), eq(orgnr1), any()) }
    }

    @Test
    fun `lagreVirksomheterSomAvklartefakta virksomhetErLagtInnManuelt valideringOKOgVirksomhetLagret`() {
        every { avklartefaktaService.slettAvklarteFakta(any(), any()) } returns Unit
        every { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) } returns Unit
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), emptyList(), listOf(orgnr3))
        behandling.saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        val virksomhetIDer = listOf(orgnr3)

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer)

        verify { avklartefaktaService.leggTilAvklarteFakta(1L, any(), any(), eq(orgnr3), any()) }
    }

    @Test
    fun `lagreVirksomheterSomAvklartefakta virksomhetErArbeidNorge valideringOKOgVirksomhetLagret`() {
        every { avklartefaktaService.slettAvklarteFakta(any(), any()) } returns Unit
        every { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) } returns Unit
        val saksopplysninger = lagArbeidsforholdOpplysninger(listOf(orgnr1))
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), emptyList(), listOf(orgnr1))
        val virksomhetIDer = listOf(orgnr1)

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer)

        verify { avklartefaktaService.leggTilAvklarteFakta(1L, any(), any(), eq(orgnr1), any()) }
    }

    @Test
    fun `lagreVirksomheterSomAvklartefakta virksomhetErUgyldig valideringFailerOgVirksomhetIkkeLagret`() {
        every { avklartefaktaService.slettAvklarteFakta(any(), any()) } returns Unit
        // Ensure the behandling is properly set up with mottatte opplysninger and saksopplysninger
        val behandlingWithData = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .build()
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


    private fun lagForetakUtland(navn: String, uuid: String, orgnr: String?): ForetakUtland = ForetakUtland().apply {
        this.navn = navn
        this.uuid = uuid
        this.orgnr = orgnr
    }

    private fun leggTilIRegisterOppslag(orgnumre: Collection<String>) {
        every { organisasjonOppslagService.hentOrganisasjoner(setOf(*orgnumre.toTypedArray())) } returns lagOrganisasjonDokumenter(orgnumre)
    }

    companion object {
        val INGEN_ADRESSE: Function<OrganisasjonDokument, Adresse?> = Function { null }
    }

    @Test
    fun `utfyllManglendeAdressefelter gyldigForretningsadresse girForretningsadresse`() {
        val organisasjonDokument = lagOrganisasjonDokument("2345", "Forretningsgatenavn")
        
        val adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(organisasjonDokument)
        
        adresse.gatenavn shouldBe "Forretningsgatenavn"
        adresse.postnummer shouldBe "2345"
        adresse.poststed shouldBe "Poststed"
        adresse.landkode shouldBe "NO"
        
        verify { mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "2345") }
    }

    @Test
    fun `utfyllManglendeAdressefelter forretningsadresseManglerGatenavn girForretningsadresseMedBlanktGatenavn`() {
        val organisasjonDokument = lagOrganisasjonDokument("2345", null)
        
        val adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(organisasjonDokument)
        
        adresse.gatenavn shouldBe " "
        adresse.postnummer shouldBe "2345"
        adresse.poststed shouldBe "Poststed"
        adresse.landkode shouldBe "NO"
        
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
        
        adresse.gatenavn shouldBe "Postgatenavn"
        adresse.postnummer shouldBe " "
        adresse.poststed shouldBe "Postpoststed"
        adresse.landkode shouldBe "DK"
        
        verify(exactly = 0) { mockKodeverkService.dekod(any(), any()) }
    }

    @Test
    fun `utfyllManglendeAdressefelter forretningsadresseManglerPostnr girPostadresse`() {
        val organisasjonDokument = lagOrganisasjonDokument(null, null)
        
        val adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(organisasjonDokument)
        
        adresse.gatenavn shouldBe "Postgatenavn"
        adresse.postnummer shouldBe "6789"
        adresse.poststed shouldBe "Poststed"
        adresse.landkode shouldBe "NO"
        
        verify { mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "6789") }
    }

    @Test
    fun `hentSelvstendigeForetakOrgnumre girListeMedKunAvklarteOrgnumre`() {
        val selvstendigeForetak = listOf(orgnr1, orgnr2)
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(selvstendigeForetak, emptyList(), emptyList())
        
        val avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentNorskeSelvstendigeForetakOrgnumre(behandling)
        
        avklarteSelvstendigeOrgnumre shouldContainExactlyInAnyOrder setOf(orgnr1)
    }

    @Test
    fun `hentArbeidsgivendeEkstraOrgnumre girListeMedKunAvklarteOrgnumre`() {
        val arbeidgivendeEkstraOrgnumre = listOf(orgnr2, orgnr1)
        val saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), emptyList(), arbeidgivendeEkstraOrgnumre)
        
        val avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)
        
        avklarteSelvstendigeOrgnumre shouldContainExactlyInAnyOrder setOf(orgnr1)
    }

    @Test
    fun `hentArbeidsgivendeRegistreOrgnumre girListeMedKunAvklarteOrgnumre`() {
        val arbeidgivendeOrgnumreEkstra = listOf(orgnr1, orgnr2, orgnr3)
        val saksopplysninger = lagArbeidsforholdOpplysninger(arbeidgivendeOrgnumreEkstra)
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), emptyList(), emptyList())
        
        val avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)
        
        avklarteSelvstendigeOrgnumre shouldContainExactlyInAnyOrder setOf(orgnr1)
    }

    @Test
    fun `testHentAvklarteNorskeForetak girAvklarteArbeidsgivere`() {
        val arbeidsgivereEkstra = listOf(orgnr2)
        val arbeidsgivereRegister = listOf(orgnr3)
        
        val saksopplysninger = lagArbeidsforholdOpplysninger(arbeidsgivereRegister)
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), emptyList(), arbeidsgivereEkstra)
        
        val avklarteOrganisasjoner = setOf(orgnr2, orgnr3)
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns avklarteOrganisasjoner
        
        leggTilIRegisterOppslag(listOf(orgnr2, orgnr3))
        
        val norskeVirksomheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, INGEN_ADRESSE)
        
        norskeVirksomheter.map { it.orgnr } shouldContainExactlyInAnyOrder listOf(orgnr2, orgnr3)
    }

    @Test
    fun `hentAlleNorskeVirksomheter SammeOrgNummerFraNorskeArbeidsgivereOgNorskeSelvstendigeForetak UnngåDuplikater`() {
        val arbeidsgivereRegister = listOf(orgnr3)
        
        val saksopplysninger = lagArbeidsforholdOpplysninger(arbeidsgivereRegister)
        behandling.saksopplysninger = saksopplysninger
        val mottatteOpplysninger = lagMottatteOpplysninger(arbeidsgivereRegister, emptyList(), emptyList())
        behandling.mottatteOpplysninger = mottatteOpplysninger
        
        val avklarteOrganisasjoner = setOf(orgnr3)
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns avklarteOrganisasjoner
        
        leggTilIRegisterOppslag(arbeidsgivereRegister)
        
        val norskeVirksomheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, INGEN_ADRESSE)
        
        norskeVirksomheter shouldHaveSize 1
        norskeVirksomheter.first().orgnr shouldBe orgnr3
    }

    @Test
    fun `hentAvklarteNorskeForetak girAvklarteSelvstendigeForetak`() {
        val selvstendigeForetak = listOf(orgnr1)
        
        val saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(selvstendigeForetak, emptyList(), emptyList())
        
        val avklarteOrganisasjoner = setOf(orgnr1)
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns avklarteOrganisasjoner
        
        leggTilIRegisterOppslag(selvstendigeForetak)
        
        val norskeVirksomheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, INGEN_ADRESSE)
        
        norskeVirksomheter.map { it.orgnr } shouldContainExactlyInAnyOrder listOf(orgnr1)
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
}
