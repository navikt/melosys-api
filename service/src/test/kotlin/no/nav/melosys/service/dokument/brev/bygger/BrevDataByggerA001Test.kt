package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.kodeverk.Kodeverk
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.ERSTATTER_EN_ANNEN_UNDER_5_AAR
import no.nav.melosys.domain.kodeverk.begrunnelser.Direkte_til_anmodning_begrunnelser.SJOEMANNSKIRKEN
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser.UTSENDELSE_OVER_24_MN
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_naeringsdrivende_begrunnelser.NORMALT_IKKE_DRIFT_NORGE
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.Bosted
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigForetak
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

        val unntakFraLovvalgsland = Land_iso2.SE
        val periode = Anmodningsperiode().apply {
            this.unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            this.unntakFraLovvalgsland = unntakFraLovvalgsland
        }
        every { anmodningsperiodeService.hentAnmodningsperioder(any()) } returns listOf(periode)

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
     * Creates a test setup with Behandling configured with Soeknad and ArbeidsforholdDokument.
     * Returns a triple of (behandling, soeknad, arbeidsforholdDokument) for test access.
     */
    private fun lagTestoppsett(): Triple<Behandling, Soeknad, ArbeidsforholdDokument> {
        val oppgittAdresse = StrukturertAdresse().apply {
            gatenavn = "HjemmeGata"
            husnummerEtasjeLeilighet = "23B"
            postnummer = "0165"
            poststed = "Oslo"
            landkode = Landkoder.NO.kode
        }

        val soeknad = Soeknad().apply {
            bosted.oppgittAdresse = oppgittAdresse
            foretakUtland.add(ForetakUtland().apply { orgnr = ORGNR1 })
        }

        val medlDokument = MedlemskapDokument()
        val medl = saksopplysningForTest {
            dokument = medlDokument
            type = SaksopplysningType.MEDL
        }

        val arbDokument = ArbeidsforholdDokument()
        lagArbeidsforhold(arbDokument, ORGNR2, LocalDate.of(2005, 1, 11), LocalDate.of(2017, 8, 11))

        val aareg = saksopplysningForTest {
            dokument = arbDokument
            type = SaksopplysningType.ARBFORH
        }

        val mottatteOpplysninger = MottatteOpplysninger().apply {
            mottatteOpplysningerData = soeknad
        }

        val behandling = Behandling.forTest {
            id = 123L
            fagsak {
                medBruker()
            }
            saksopplysninger.add(medl)
            saksopplysninger.add(aareg)
            this.mottatteOpplysninger = mottatteOpplysninger
        }

        return Triple(behandling, soeknad, arbDokument)
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
        val (behandling, soeknad, _) = lagTestoppsett()
        avklarteOrganisasjoner.add(ORGNR1)

        val foretak = SelvstendigForetak().apply { orgnr = ORGNR1 }
        val foretak2 = SelvstendigForetak().apply { orgnr = ORGNR2 }
        soeknad.selvstendigArbeid.selvstendigForetak = mutableListOf(foretak, foretak2)

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(behandling), SAKSBEHANDLER_ID) as BrevDataA001
        brevDataA001.selvstendigeVirksomheter.map { it.orgnr } shouldContainExactly listOf(foretak.orgnr)
    }

    @Test
    fun `hent avklarte norske foretak`() {
        val (behandling, soeknad, _) = lagTestoppsett()
        avklarteOrganisasjoner.add(ORGNR1)

        val foretak = SelvstendigForetak().apply { orgnr = ORGNR1 }
        soeknad.selvstendigArbeid.selvstendigForetak = mutableListOf(foretak)
        soeknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere = mutableListOf(ORGNR1)

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(behandling), SAKSBEHANDLER_ID) as BrevDataA001

        brevDataA001.run {
            selvstendigeVirksomheter.map { it.orgnr } shouldContainExactly listOf(ORGNR1)
            arbeidsgivendeVirksomheter.map { it.orgnr } shouldContainExactly listOf(ORGNR1)
        }
    }

    @Test
    fun `ingen avklarte foretak`() {
        val (behandling, soeknad, _) = lagTestoppsett()
        val foretak = SelvstendigForetak().apply { orgnr = ORGNR1 }
        soeknad.selvstendigArbeid.selvstendigForetak = mutableListOf(foretak)

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(behandling), SAKSBEHANDLER_ID) as BrevDataA001
        brevDataA001.ansettelsesperiode shouldBe null
    }

    @Test
    fun `lag art16 med art121 har kun art16 begrunnelser`() {
        val (behandling, _, _) = lagTestoppsett()
        val vilkaarsresultat = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_1, false, UTSENDELSE_OVER_24_MN)
        every { vilkaarsresultatService.finnUtsendingArbeidstakerVilkaarsresultat(any()) } returns vilkaarsresultat
        lagUnntaksVilkaarResultat(Vilkaar.FO_883_2004_ART16_1, ERSTATTER_EN_ANNEN_UNDER_5_AAR)

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(behandling), SAKSBEHANDLER_ID) as BrevDataA001

        brevDataA001.run {
            anmodningUtenArt12Begrunnelser.shouldBeEmpty()
            anmodningBegrunnelser shouldHaveSize 1
            anmodningBegrunnelser.map { it.kode } shouldContainExactly listOf(ERSTATTER_EN_ANNEN_UNDER_5_AAR.kode)
        }
    }

    @Test
    fun `lag art16 med art122 har kun art16 begrunnelser`() {
        val (behandling, _, _) = lagTestoppsett()
        val vilkaarsresultat = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_2, false, NORMALT_IKKE_DRIFT_NORGE)
        every { vilkaarsresultatService.finnUtsendingNæringsdrivendeVilkaarsresultat(any()) } returns vilkaarsresultat
        lagUnntaksVilkaarResultat(Vilkaar.FO_883_2004_ART16_1, ERSTATTER_EN_ANNEN_UNDER_5_AAR)

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(behandling), SAKSBEHANDLER_ID) as BrevDataA001

        brevDataA001.run {
            anmodningUtenArt12Begrunnelser.shouldBeEmpty()
            anmodningBegrunnelser shouldHaveSize 1
            anmodningBegrunnelser.map { it.kode } shouldContainExactly listOf(ERSTATTER_EN_ANNEN_UNDER_5_AAR.kode)
        }
    }

    @Test
    fun `lag art18 med art141 har kun art18 begrunnelser`() {
        val (behandling, _, _) = lagTestoppsett()
        val vilkaarsresultat = lagVilkaarsresultat(Vilkaar.KONV_EFTA_STORBRITANNIA_ART14_1, false, UTSENDELSE_OVER_24_MN)
        every { vilkaarsresultatService.finnUtsendingArbeidstakerVilkaarsresultat(any()) } returns vilkaarsresultat
        lagUnntaksVilkaarResultat(Vilkaar.KONV_EFTA_STORBRITANNIA_ART18_1, ERSTATTER_EN_ANNEN_UNDER_5_AAR)

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(behandling), SAKSBEHANDLER_ID) as BrevDataA001

        brevDataA001.run {
            anmodningUtenArt12Begrunnelser.shouldBeEmpty()
            anmodningBegrunnelser shouldHaveSize 1
            anmodningBegrunnelser.map { it.kode } shouldContainExactly listOf(ERSTATTER_EN_ANNEN_UNDER_5_AAR.kode)
        }
    }

    @Test
    fun `lag art16 uten art12 har kun art16 uten art12 begrunnelser`() {
        val (behandling, _, _) = lagTestoppsett()
        lagUnntaksVilkaarResultat(Vilkaar.FO_883_2004_ART16_1, SJOEMANNSKIRKEN)

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(behandling), SAKSBEHANDLER_ID) as BrevDataA001

        brevDataA001.run {
            anmodningBegrunnelser.shouldBeEmpty()
            anmodningUtenArt12Begrunnelser shouldHaveSize 1
            anmodningUtenArt12Begrunnelser.map { it.kode } shouldContainExactly listOf(SJOEMANNSKIRKEN.kode)
        }
    }

    @Test
    fun `test ansettelsesperiode`() {
        val (behandling, _, arbDokument) = lagTestoppsett()
        avklarteOrganisasjoner.add(ORGNR1)

        lagArbeidsforhold(arbDokument, ORGNR1, LocalDate.of(1976, 10, 23), LocalDate.of(1978, 10, 23))

        val forventet = lagArbeidsforhold(arbDokument, ORGNR1, LocalDate.of(2005, 1, 11), LocalDate.of(2018, 8, 11))

        // Senere arbeidsforhold, men ikke et valgt arbeidsforhold
        lagArbeidsforhold(arbDokument, ORGNR2, LocalDate.of(2010, 10, 23), LocalDate.of(2017, 10, 23))

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(behandling), SAKSBEHANDLER_ID) as BrevDataA001
        brevDataA001.ansettelsesperiode shouldBe forventet.ansettelsesPeriode
    }

    @Test
    fun `test ingen ansettelse periode`() {
        val (behandling, _, _) = lagTestoppsett()
        avklarteOrganisasjoner.add(ORGNR1)

        val brevDataA001 = brevDataByggerA001.lag(lagBrevDataGrunnlag(behandling), SAKSBEHANDLER_ID) as BrevDataA001
        brevDataA001.ansettelsesperiode shouldBe null
    }

    @Test
    fun `lag brevdata ytterligere info fra bestilling info finnes`() {
        val (behandling, _, _) = lagTestoppsett()
        val forventetInfo = "By the way..."
        val brevbestilling = DoksysBrevbestilling.Builder()
            .medBehandling(behandling)
            .medYtterligereInformasjon(forventetInfo)
            .build()
        val brevdataGrunnlag = lagBrevDataGrunnlag(brevbestilling)

        val brevDataA001 = brevDataByggerA001.lag(brevdataGrunnlag, SAKSBEHANDLER_ID) as BrevDataA001
        brevDataA001.ytterligereInformasjon shouldBe forventetInfo
    }

    @Test
    fun `lag brevdata har ikke bostedsadresse bruker kontaktadresse`() {
        val (behandling, soeknad, _) = lagTestoppsett()
        soeknad.bosted = Bosted()
        val doksysBrevbestilling = DoksysBrevbestilling.Builder().medBehandling(behandling).build()
        val personopplysninger = lagPersonopplysningerUtenBostedsadresse()
        val brevdataGrunnlag = lagBrevDataGrunnlag(doksysBrevbestilling, personopplysninger)

        val brevDataA001 = brevDataByggerA001.lag(brevdataGrunnlag, SAKSBEHANDLER_ID) as BrevDataA001
        brevDataA001.bostedsadresse shouldBe personopplysninger.finnKontaktadresse().get().hentEllerLagStrukturertAdresse()
    }

    @Test
    fun `lag brevdata har ikke bostedsadresse eller kontaktadresse kaster feilmelding`() {
        val (behandling, soeknad, _) = lagTestoppsett()
        soeknad.bosted = Bosted()
        val doksysBrevbestilling = DoksysBrevbestilling.Builder().medBehandling(behandling).build()
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
