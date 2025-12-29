package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.*
import no.nav.melosys.domain.anmodningsperiodeForTest
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.kodeverk.Kodeverk
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.ERSTATTER_EN_ANNEN_UNDER_5_AAR
import no.nav.melosys.domain.kodeverk.begrunnelser.Direkte_til_anmodning_begrunnelser.SJOEMANNSKIRKEN
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser.UTSENDELSE_OVER_24_MN
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_naeringsdrivende_begrunnelser.NORMALT_IKKE_DRIFT_NORGE
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.mottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SoeknadTestFactory
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.Bosted
import no.nav.melosys.domain.mottatteopplysninger.soeknadForTest
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.dokument.brev.BrevDataA001
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagStrukturertAdresse
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagVilkaarsresultat
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysningerUtenBostedsadresse
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysningerUtenBostedsadresseOgKontaktadresse
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrevDataByggerA001Test {

    private val anmodningsperiodeService: AnmodningsperiodeService = mockk()
    private val avklartefaktaService: AvklartefaktaService = mockk()
    private val lovvalgsperiodeService: LovvalgsperiodeService = mockk()
    private val myndighetsService: UtenlandskMyndighetService = mockk()
    private val vilkaarsresultatService: VilkaarsresultatService = mockk()
    private val ereg: EregFasade = mockk()

    private val avklarteOrganisasjoner = mutableSetOf<String>()
    private lateinit var brevDataByggerA001: BrevDataByggerA001

    @BeforeEach
    fun setUp() {
        avklarteOrganisasjoner.clear()
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns avklarteOrganisasjoner
        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns emptyMap()

        val anmodningsperiode = anmodningsperiodeForTest {
            unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            unntakFraLovvalgsland = no.nav.melosys.domain.kodeverk.Land_iso2.SE
        }
        every { anmodningsperiodeService.hentAnmodningsperioder(any()) } returns listOf(anmodningsperiode)

        every { myndighetsService.hentUtenlandskMyndighet(any()) } returns UtenlandskMyndighet()

        lagUnntaksVilkaarResultat(Vilkaar.FO_883_2004_ART16_1, ERSTATTER_EN_ANNEN_UNDER_5_AAR)

        val detaljer: OrganisasjonsDetaljer = mockk()
        every { detaljer.hentStrukturertForretningsadresse() } returns lagStrukturertAdresse()
        every { detaljer.opphoersdato } returns null

        leggTilTestorganisasjon("navn1", ORGNR1, detaljer)
        leggTilTestorganisasjon("navn2", ORGNR2, detaljer)

        every { vilkaarsresultatService.harVilkaarForUtsending(any()) } answers { callOriginal() }
        every { vilkaarsresultatService.finnUtsendingArbeidstakerVilkaarsresultat(any()) } returns null
        every { vilkaarsresultatService.finnUtsendingNæringsdrivendeVilkaarsresultat(any()) } returns null
        every { lovvalgsperiodeService.hentTidligereLovvalgsperioder(any()) } returns emptyList()

        brevDataByggerA001 = BrevDataByggerA001(lovvalgsperiodeService, anmodningsperiodeService, myndighetsService, vilkaarsresultatService)
    }

    private fun lagUnntaksVilkaarResultat(vilkaarType: Vilkaar, begrunnelseKode: Kodeverk) {
        val vilkaarsresultat = lagVilkaarsresultat(vilkaarType, true, begrunnelseKode)
        every { vilkaarsresultatService.finnUnntaksVilkaarsresultat(any()) } returns vilkaarsresultat
    }

    /**
     * Data class for test setup results
     */
    private data class TestOppsett(
        val behandling: Behandling,
        val arbDokument: ArbeidsforholdDokument,
        val soeknad: Soeknad
    )

    /**
     * DSL builder for A001 test setup.
     *
     * Example:
     * ```
     * lagBehandling {
     *     selvstendigForetakOrgnr = listOf(ORGNR1, ORGNR2)
     *     soeknad { bosted(Bosted()) }
     * }
     * ```
     */
    @MelosysTestDsl
    private class TestOppsettBuilder {
        var selvstendigForetakOrgnr: List<String> = emptyList()
        var ekstraArbeidsgivere: List<String> = emptyList()

        private var soeknadBlock: (SoeknadTestFactory.Builder.() -> Unit)? = null

        fun soeknad(init: SoeknadTestFactory.Builder.() -> Unit) {
            soeknadBlock = init
        }

        fun build(arbDokument: ArbeidsforholdDokument): TestOppsett {
            val builder = this
            val soeknad = soeknadForTest {
                bostedAdresse(
                    landkode = Landkoder.NO.kode,
                    gatenavn = "HjemmeGata",
                    husnummer = "23B",
                    postnummer = "0165",
                    poststed = "Oslo"
                )
                builder.selvstendigForetakOrgnr.forEach { selvstendigForetak(it) }
                builder.ekstraArbeidsgivere.forEach { ekstraArbeidsgiver(it) }
                builder.soeknadBlock?.invoke(this)
            }

            val behandling = Behandling.forTest {
                id = 123L
                fagsak { medBruker() }
                saksopplysning { dokument = arbDokument; type = SaksopplysningType.ARBFORH }
                mottatteOpplysninger { mottatteOpplysningerData = soeknad }
            }

            return TestOppsett(behandling, arbDokument, soeknad)
        }
    }

    private fun lagBehandling(init: TestOppsettBuilder.() -> Unit = {}): TestOppsett {
        val arbDokument = ArbeidsforholdDokument()
        lagArbeidsforhold(arbDokument, ORGNR2, LocalDate.of(2005, 1, 11), LocalDate.of(2017, 8, 11))

        return TestOppsettBuilder().apply(init).build(arbDokument)
    }

    private fun lagBrevDataGrunnlag(behandling: Behandling): BrevDataGrunnlag =
        lagBrevDataGrunnlag(behandling, PersonopplysningerObjectFactory.lagPersonopplysninger())

    private fun lagBrevDataGrunnlag(behandling: Behandling, persondata: Persondata): BrevDataGrunnlag =
        lagBrevDataGrunnlag(DoksysBrevbestilling.Builder().medBehandling(behandling).build(), persondata)

    private fun lagBrevDataGrunnlag(brevbestilling: DoksysBrevbestilling): BrevDataGrunnlag =
        lagBrevDataGrunnlag(brevbestilling, PersonopplysningerObjectFactory.lagPersonopplysninger())

    private fun lagBrevDataGrunnlag(brevbestilling: DoksysBrevbestilling, persondata: Persondata): BrevDataGrunnlag {
        val registerOppslagService = OrganisasjonOppslagService(ereg)
        val avklarteVirksomheterService = AvklarteVirksomheterService(
            avklartefaktaService,
            registerOppslagService,
            mockk<BehandlingService>(),
            mockk<KodeverkService>()
        )
        return BrevDataGrunnlag(brevbestilling, mockk<KodeverkService>(), avklarteVirksomheterService, avklartefaktaService, persondata)
    }

    private fun leggTilTestorganisasjon(navn: String, orgnummer: String, detaljer: OrganisasjonsDetaljer) {
        val orgDok = OrganisasjonDokumentTestFactory.builder()
            .orgnummer(orgnummer)
            .navn(navn)
            .organisasjonsDetaljer(detaljer)
            .build()
        val saksopplysning = saksopplysningForTest {
            type = SaksopplysningType.ORG
            dokument = orgDok
        }
        every { ereg.hentOrganisasjon(orgnummer) } returns saksopplysning
    }

    private fun lagArbeidsforhold(arbDokument: ArbeidsforholdDokument, orgnr: String, fom: LocalDate, tom: LocalDate): Arbeidsforhold =
        Arbeidsforhold().apply {
            arbeidsgiverID = orgnr
            ansettelsesPeriode = Periode(fom, tom)
            arbDokument.arbeidsforhold = arbDokument.arbeidsforhold + this
        }

    @Test
    fun `hent avklarte selvstendige foretak`() {
        avklarteOrganisasjoner.add(ORGNR1)
        val oppsett = lagBehandling {
            selvstendigForetakOrgnr = listOf(ORGNR1, ORGNR2)
        }

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(oppsett.behandling), SAKSBEHANDLER_ID) as BrevDataA001
        brevDataA001.selvstendigeVirksomheter.map { it.orgnr } shouldContainExactly listOf(ORGNR1)
    }

    @Test
    fun `hent avklarte norske foretak`() {
        avklarteOrganisasjoner.add(ORGNR1)
        val oppsett = lagBehandling {
            selvstendigForetakOrgnr = listOf(ORGNR1)
            ekstraArbeidsgivere = listOf(ORGNR1)
        }

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(oppsett.behandling), SAKSBEHANDLER_ID) as BrevDataA001

        brevDataA001.run {
            selvstendigeVirksomheter.map { it.orgnr } shouldContainExactly listOf(ORGNR1)
            arbeidsgivendeVirksomheter.map { it.orgnr } shouldContainExactly listOf(ORGNR1)
        }
    }

    @Test
    fun `ingen avklarte foretak`() {
        val oppsett = lagBehandling {
            selvstendigForetakOrgnr = listOf(ORGNR1)
        }

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(oppsett.behandling), SAKSBEHANDLER_ID) as BrevDataA001
        brevDataA001.ansettelsesperiode shouldBe null
    }

    @Test
    fun `lag art16 med art121 har kun art16 begrunnelser`() {
        val oppsett = lagBehandling()
        val vilkaarsresultat = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_1, false, UTSENDELSE_OVER_24_MN)
        every { vilkaarsresultatService.finnUtsendingArbeidstakerVilkaarsresultat(any()) } returns vilkaarsresultat
        lagUnntaksVilkaarResultat(Vilkaar.FO_883_2004_ART16_1, ERSTATTER_EN_ANNEN_UNDER_5_AAR)

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(oppsett.behandling), SAKSBEHANDLER_ID) as BrevDataA001

        brevDataA001.run {
            anmodningUtenArt12Begrunnelser.shouldBeEmpty()
            anmodningBegrunnelser shouldHaveSize 1
            anmodningBegrunnelser.map { it.kode } shouldContainExactly listOf(ERSTATTER_EN_ANNEN_UNDER_5_AAR.kode)
        }
    }

    @Test
    fun `lag art16 med art122 har kun art16 begrunnelser`() {
        val oppsett = lagBehandling()
        val vilkaarsresultat = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_2, false, NORMALT_IKKE_DRIFT_NORGE)
        every { vilkaarsresultatService.finnUtsendingNæringsdrivendeVilkaarsresultat(any()) } returns vilkaarsresultat
        lagUnntaksVilkaarResultat(Vilkaar.FO_883_2004_ART16_1, ERSTATTER_EN_ANNEN_UNDER_5_AAR)

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(oppsett.behandling), SAKSBEHANDLER_ID) as BrevDataA001

        brevDataA001.run {
            anmodningUtenArt12Begrunnelser.shouldBeEmpty()
            anmodningBegrunnelser shouldHaveSize 1
            anmodningBegrunnelser.map { it.kode } shouldContainExactly listOf(ERSTATTER_EN_ANNEN_UNDER_5_AAR.kode)
        }
    }

    @Test
    fun `lag art18 med art141 har kun art18 begrunnelser`() {
        val oppsett = lagBehandling()
        val vilkaarsresultat = lagVilkaarsresultat(Vilkaar.KONV_EFTA_STORBRITANNIA_ART14_1, false, UTSENDELSE_OVER_24_MN)
        every { vilkaarsresultatService.finnUtsendingArbeidstakerVilkaarsresultat(any()) } returns vilkaarsresultat
        lagUnntaksVilkaarResultat(Vilkaar.KONV_EFTA_STORBRITANNIA_ART18_1, ERSTATTER_EN_ANNEN_UNDER_5_AAR)

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(oppsett.behandling), SAKSBEHANDLER_ID) as BrevDataA001

        brevDataA001.run {
            anmodningUtenArt12Begrunnelser.shouldBeEmpty()
            anmodningBegrunnelser shouldHaveSize 1
            anmodningBegrunnelser.map { it.kode } shouldContainExactly listOf(ERSTATTER_EN_ANNEN_UNDER_5_AAR.kode)
        }
    }

    @Test
    fun `lag art16 uten art12 har kun art16 uten art12 begrunnelser`() {
        val oppsett = lagBehandling()
        lagUnntaksVilkaarResultat(Vilkaar.FO_883_2004_ART16_1, SJOEMANNSKIRKEN)

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(oppsett.behandling), SAKSBEHANDLER_ID) as BrevDataA001

        brevDataA001.run {
            anmodningBegrunnelser.shouldBeEmpty()
            anmodningUtenArt12Begrunnelser shouldHaveSize 1
            anmodningUtenArt12Begrunnelser.map { it.kode } shouldContainExactly listOf(SJOEMANNSKIRKEN.kode)
        }
    }

    @Test
    fun `test ansettelsesperiode`() {
        avklarteOrganisasjoner.add(ORGNR1)
        val oppsett = lagBehandling()

        lagArbeidsforhold(oppsett.arbDokument, ORGNR1, LocalDate.of(1976, 10, 23), LocalDate.of(1978, 10, 23))
        val forventet = lagArbeidsforhold(oppsett.arbDokument, ORGNR1, LocalDate.of(2005, 1, 11), LocalDate.of(2018, 8, 11))
        // Senere arbeidsforhold, men ikke et valgt arbeidsforhold
        lagArbeidsforhold(oppsett.arbDokument, ORGNR2, LocalDate.of(2010, 10, 23), LocalDate.of(2017, 10, 23))

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(oppsett.behandling), SAKSBEHANDLER_ID) as BrevDataA001
        brevDataA001.ansettelsesperiode shouldBe forventet.ansettelsesPeriode
    }

    @Test
    fun `test ingen ansettelse periode`() {
        avklarteOrganisasjoner.add(ORGNR1)
        val oppsett = lagBehandling()

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(oppsett.behandling), SAKSBEHANDLER_ID) as BrevDataA001
        brevDataA001.ansettelsesperiode shouldBe null
    }

    @Test
    fun `lag brevdata ytterligere info fra bestilling info finnes`() {
        val oppsett = lagBehandling()
        val forventetInfo = "By the way..."
        val brevbestilling = DoksysBrevbestilling.Builder()
            .medBehandling(oppsett.behandling)
            .medYtterligereInformasjon(forventetInfo)
            .build()
        val brevdataGrunnlag = lagBrevDataGrunnlag(brevbestilling)

        val brevDataA001 = brevDataByggerA001.lag(brevdataGrunnlag, SAKSBEHANDLER_ID) as BrevDataA001
        brevDataA001.ytterligereInformasjon shouldBe forventetInfo
    }

    @Test
    fun `lag brevdata har ikke bostedsadresse bruker kontaktadresse`() {
        val oppsett = lagBehandling {
            soeknad { bosted(Bosted()) }
        }

        val doksysBrevbestilling = DoksysBrevbestilling.Builder().medBehandling(oppsett.behandling).build()
        val personopplysninger = lagPersonopplysningerUtenBostedsadresse()
        val brevdataGrunnlag = lagBrevDataGrunnlag(doksysBrevbestilling, personopplysninger)

        val brevDataA001 = brevDataByggerA001.lag(brevdataGrunnlag, SAKSBEHANDLER_ID) as BrevDataA001
        brevDataA001.bostedsadresse shouldBe personopplysninger.finnKontaktadresse().get().hentEllerLagStrukturertAdresse()
    }

    @Test
    fun `lag brevdata har ikke bostedsadresse eller kontaktadresse kaster feilmelding`() {
        val oppsett = lagBehandling {
            soeknad { bosted(Bosted()) }
        }

        val doksysBrevbestilling = DoksysBrevbestilling.Builder().medBehandling(oppsett.behandling).build()
        val personopplysninger = lagPersonopplysningerUtenBostedsadresseOgKontaktadresse()
        val brevdataGrunnlag = lagBrevDataGrunnlag(doksysBrevbestilling, personopplysninger)

        val exception = shouldThrow<FunksjonellException> {
            brevDataByggerA001.lag(brevdataGrunnlag, SAKSBEHANDLER_ID)
        }
        exception.message shouldBe "Finner verken bostedsadresse eller kontaktadresse"
    }

    companion object {
        private const val SAKSBEHANDLER_ID = "Z12345"
        private const val ORGNR1 = "123456789"
        private const val ORGNR2 = "987654321"
    }
}
