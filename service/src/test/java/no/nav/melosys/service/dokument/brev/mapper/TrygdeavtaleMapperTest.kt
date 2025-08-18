package no.nav.melosys.service.dokument.brev.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie
import no.nav.melosys.domain.person.familie.OmfattetFamilie
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.UtledMottaksdato
import no.nav.melosys.service.dokument.DokgenTestData.*
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrygdeavtaleMapperTest {

    @MockK
    private lateinit var mockAvklarteMedfolgendeFamilieService: AvklarteMedfolgendeFamilieService

    @MockK
    private lateinit var mockLovvalgsperiodeService: LovvalgsperiodeService

    @MockK
    private lateinit var mockAvklarteVirksomheterService: AvklarteVirksomheterService

    @MockK
    private lateinit var utledMottaksdato: UtledMottaksdato

    private lateinit var trygdeavtaleMapper: TrygdeavtaleMapper

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        trygdeavtaleMapper = TrygdeavtaleMapper(
            mockAvklarteMedfolgendeFamilieService,
            mockAvklarteVirksomheterService,
            mockLovvalgsperiodeService,
            utledMottaksdato
        )
    }

    @Test
    fun `map altOkInnvilgelseOgAttestSendes populererFelter`() {
        mockMedfølgendeFamilieDefaultCase()
        mockAvklartFamilieDefaultCase()
        mockHappyCase()
        val brevbestilling = lagStorbritanniaBrevbestilling(medPeriode(lagTrygdeavtaleBehandling()))


        val innvilgelseOgAttestTrygdeavtale = trygdeavtaleMapper.map(brevbestilling, Land_iso2.GB)
        val jsonInnvilgelse = ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(innvilgelseOgAttestTrygdeavtale.innvilgelse)
        val resultatInnvilgelse = jsonInnvilgelse.replace(Regex("(\"dagensDato\" :)(.*)"), "$1 \"Fjernet for test\",")
        val jsonAttest = ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(innvilgelseOgAttestTrygdeavtale.attest)
        val resultatAttest = jsonAttest.replace(Regex("(\"dagensDato\" :)(.*)"), "$1 \"Fjernet for test\",")


        innvilgelseOgAttestTrygdeavtale.run {
            isSkalHaAttest shouldBe true
            isSkalHaInnvilgelse shouldBe true
            isSkalHaInfoOmRettigheter shouldBe true
            nyVurderingBakgrunn shouldBe brevbestilling.nyVurderingBakgrunn
        }
        resultatInnvilgelse shouldBe FORVENTEDE_FELTER_FOR_INNVILGELSE_STORBRITANNIA_MAPPING
        resultatAttest shouldBe FORVENTEDE_FELTER_FOR_ATTEST_STORBRITANNIA_MAPPING
    }

    @Test
    fun `map utenlandske virksomheter populererFelter`() {
        mockHappyCase()

        every { mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(any()) } returns emptyList()
        every { mockAvklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns lagAvklarteVirksomheter()

        val brevbestilling = lagStorbritanniaBrevbestilling(medPeriode(lagTrygdeavtaleBehandling()))

        val result = trygdeavtaleMapper.map(brevbestilling, Land_iso2.GB)

        result shouldNotBe null
    }

    @ParameterizedTest(name = "{6}")
    @MethodSource("sjekkAdresser")
    fun `map brukNorskBostedsAddresse`(
        landkodeBosted: Land_iso2?,
        landkodeOpphold: Land_iso2?,
        landkodeKontakt: Land_iso2?,
        norskAddresse: List<String>?,
        ukAddresse: List<String>?,
        soknadsland: Land_iso2?,
        grunn: String?
    ) {
        mockHappyCase()
        val persondata = TrygdeavtaleAdresseSjekkerTest().lagPersonopplysninger(
            landkodeBosted,
            landkodeOpphold,
            landkodeKontakt,
            java.util.Optional.empty(),
            java.util.Optional.empty(),
            java.util.Optional.empty()
        )
        val brevbestilling = lagStorbritanniaBrevbestillingDefaultBuilder(medPeriode(lagTrygdeavtaleBehandling()))
            .medPersonDokument(persondata)
            .build()

        val innvilgelseOgAttestTrygdeavtale = trygdeavtaleMapper.map(brevbestilling, soknadsland!!)
        innvilgelseOgAttestTrygdeavtale.isSkalHaAttest shouldBe true

        val attest = innvilgelseOgAttestTrygdeavtale.attest
        val bostedsadresse = attest.arbeidstaker.bostedsadresse()
        val oppholdsadresse = attest.utsendelse.oppholdsadresse()

        bostedsadresse shouldBe norskAddresse
        oppholdsadresse shouldBe ukAddresse
    }

    @Test
    fun `map bostedadresseUtenforLandIso2 blirMappetKorrekt`() {
        mockHappyCase()

        val persondata = TrygdeavtaleAdresseSjekkerTest().lagPersonopplysninger(
            Land_iso2.NO,
            Land_iso2.NO,
            Land_iso2.NO,
            java.util.Optional.of("SG"),
            java.util.Optional.of("SG"),
            java.util.Optional.of("SG")
        )
        val brevbestilling = lagStorbritanniaBrevbestillingDefaultBuilder(medPeriode(lagTrygdeavtaleBehandling()))
            .medPersonDokument(persondata)
            .build()
        val innvilgelseOgAttestTrygdeavtale = trygdeavtaleMapper.map(brevbestilling, Land_iso2.US)
        innvilgelseOgAttestTrygdeavtale.isSkalHaAttest shouldBe true

        val attest = innvilgelseOgAttestTrygdeavtale.attest
        val bostedsadresse = attest.arbeidstaker.bostedsadresse()

        bostedsadresse shouldBe listOf(TrygdeavtaleAdresseSjekker.BOSTED_UTENFOR_NORGE)
    }

    @Test
    fun `map ingenRepresentantIUtlandet kastFunksjonellException`() {
        mockHappyCase()

        shouldThrow<FunksjonellException> {
            trygdeavtaleMapper.map(lagStorbritanniaBrevbestilling(medPeriode(lagTrygdeavtaleBehandling(null))), Land_iso2.GB)
        }.message should {
            it?.contains(Kontroll_begrunnelser.ATTEST_MANGLER_ARBEIDSSTED.beskrivelse) shouldBe true
        }
    }

    @Test
    fun `map ingenVirksomheter kastFunksjonellException`() {
        mockLovvalgsperiode()

        every { mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(any()) } returns emptyList()
        every { mockAvklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns emptyList()

        shouldThrow<FunksjonellException> {
            trygdeavtaleMapper.map(lagStorbritanniaBrevbestilling(medPeriode(lagTrygdeavtaleBehandling())), Land_iso2.GB)
        }.message should {
            it?.contains("Fant 0 avklarte virksomheter for behandling:") shouldBe true
            it?.contains("Må være 1 for trygdeavtale") shouldBe true
        }
    }

    @Test
    fun `map ettOmfattetBarn minstEttOmfattetFamiliemedlemErTrue`() {
        mockLovvalgsperiode()
        mockMedfølgendeFamilieDefaultCase()
        mockHappyCase()
        every { mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(any()) } returns lagIkkeOmfattetMedfølgendeEktefelle()
        every { mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(any()) } returns lagOmfattetMedfølgendeBarn()

        val brevbestilling = lagStorbritanniaBrevbestilling(medPeriode(lagTrygdeavtaleBehandling()))
        val map = trygdeavtaleMapper.map(brevbestilling, Land_iso2.GB).innvilgelse
        map.familie.minstEttOmfattetFamiliemedlem() shouldBe true
    }

    @Test
    fun `map barnUtenFnr parseOppgittFnrTilDato`() {
        mockMedfølgendeFamilieDefaultCase()
        mockHappyCase()
        every { mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(any()) } returns tomFamilie()
        every { mockAvklarteMedfolgendeFamilieService.hentMedfølgendeBarn(any()) } returns lagMedølgendeBarnUtenFnr()
        every { mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(any()) } returns lagBarnUtenFnr()
        val brevbestilling = lagStorbritanniaBrevbestilling(medPeriode(lagTrygdeavtaleBehandling()))


        val map = trygdeavtaleMapper.map(brevbestilling, Land_iso2.GB).innvilgelse


        map.familie.barn().run {
            size shouldBe 1
            this[0].fnr shouldBe null
            this[0].foedselsdato shouldBe LocalDate.of(2021, 2, 1)
        }
    }

    @Test
    fun `map ingenOmfattet minstEttOmfattetFamiliemedlemErFalse`() {
        mockMedfølgendeFamilieDefaultCase()
        mockLovvalgsperiode()
        every { mockLovvalgsperiodeService.hentLovvalgsperioder(any()) } returns listOf(lagLovvalgsperiode())
        every { mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(any()) } returns lagAvklarteVirksomheter()
        every { mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(any()) } returns lagIkkeOmfattetMedfølgendeEktefelle()
        every { mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(any()) } returns lagIkkeOmfattetMedfølgendeBarn()

        val brevbestilling = lagStorbritanniaBrevbestilling(medPeriode(lagTrygdeavtaleBehandling()))
        val map = trygdeavtaleMapper.map(brevbestilling, Land_iso2.GB).innvilgelse
        map.familie.minstEttOmfattetFamiliemedlem() shouldBe false
    }

    @Test
    fun `map medToLovvalgperioder kastFunksjonellException`() {
        mockHappyCase()
        every { mockLovvalgsperiodeService.hentLovvalgsperioder(any()) } returns listOf(lagLovvalgsperiode(), lagLovvalgsperiode())

        shouldThrow<FunksjonellException> {
            trygdeavtaleMapper.map(lagStorbritanniaBrevbestilling(medPeriode(lagTrygdeavtaleBehandling())), Land_iso2.GB)
        }.message should {
            it?.contains("Det kan bare være en lovvalgsperiode for trygdeavtale. Fant 2") shouldBe true
        }
    }

    @Test
    fun `map medIngenLovvalgperioder kastFunksjonellException`() {
        mockHappyCase()
        every { mockLovvalgsperiodeService.hentLovvalgsperioder(any()) } returns emptyList()

        shouldThrow<FunksjonellException> {
            trygdeavtaleMapper.map(
                InnvilgelseBrevbestilling.Builder()
                    .medBehandling(medPeriode(lagTrygdeavtaleBehandling()))
                    .medPersonDokument(lagPersondata())
                    .medVedtaksdato(VEDTAKS_DATO_INSTANT)
                    .build(),
                Land_iso2.GB
            )
        }.message should {
            it?.contains("Det kan bare være en lovvalgsperiode for trygdeavtale. Fant 0") shouldBe true
        }
    }

    private fun mockMedfølgendeFamilieDefaultCase() {
        every { mockAvklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(any()) } returns lagMedfølgendeEktefelle()
        every { mockAvklarteMedfolgendeFamilieService.hentMedfølgendeBarn(any()) } returns lagMedfølgendeBarn()
    }

    private fun mockAvklartFamilieDefaultCase() {
        every { mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(any()) } returns lagOmfattetMedfølgendeEktefelle()
        every { mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(any()) } returns lagAvklartMedfølgendeBarn()
    }

    private fun mockLovvalgsperiode() {
        every { mockLovvalgsperiodeService.hentLovvalgsperiode(any()) } returns lagLovvalgsperiode()
        every { mockLovvalgsperiodeService.harSelvstendigNæringsdrivendeLovvalgsbestemmelse(any()) } returns false
    }

    private fun medPeriode(behandling: Behandling): Behandling {
        every { utledMottaksdato.getMottaksdato(behandling) } returns SOKNADSDATO
        return behandling
    }

    private fun lagStorbritanniaBrevbestillingDefaultBuilder(behandling: Behandling): InnvilgelseBrevbestilling.Builder =
        InnvilgelseBrevbestilling.Builder()
            .medProduserbartdokument(no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.TRYGDEAVTALE_GB)
            .medPersonDokument(lagPersondata())
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("innledningFritekst")
            .medBegrunnelseFritekst("begrunnelse")
            .medBarnFritekst("barnFritekst")
            .medEktefelleFritekst("ektefelleFritekst")
            .medVedtaksdato(VEDTAKS_DATO_INSTANT)
            .medVirksomhetArbeidsgiverSkalHaKopi(false)

    private fun lagStorbritanniaBrevbestilling(behandling: Behandling): InnvilgelseBrevbestilling =
        lagStorbritanniaBrevbestillingDefaultBuilder(behandling).build()

    private fun lagOmfattetMedfølgendeEktefelle() =
        AvklarteMedfolgendeFamilie(setOf(OmfattetFamilie(UUID_EKTEFELLE)), setOf())

    private fun lagIkkeOmfattetMedfølgendeEktefelle() =
        AvklarteMedfolgendeFamilie(
            setOf(), setOf(
                IkkeOmfattetFamilie(
                    UUID_EKTEFELLE,
                    Medfolgende_ektefelle_samboer_begrunnelser_ftrl.MANGLER_OPPLYSNINGER.kode,
                    ""
                )
            )
        )

    private fun lagOmfattetMedfølgendeBarn() = AvklarteMedfolgendeFamilie(
        setOf(OmfattetFamilie(UUID_BARN_1).apply {
            this.sammensattNavn = BARN_NAVN_1
            this.ident = BARN1_FNR
        }), setOf()
    )

    private fun lagIkkeOmfattetMedfølgendeBarn() = AvklarteMedfolgendeFamilie(
        setOf(),
        setOf(
            IkkeOmfattetFamilie(
                UUID_BARN_1,
                Medfolgende_barn_begrunnelser.MANGLER_OPPLYSNINGER.kode,
                ""
            )
        )
    )

    private fun lagMedølgendeBarnUtenFnr(): Map<String, MedfolgendeFamilie> = mapOf(
        UUID_BARN_3 to MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_BARN_3,
            BARN3_UTEN_FNR,
            BARN_NAVN_3,
            MedfolgendeFamilie.Relasjonsrolle.BARN
        )
    )

    private fun lagBarnUtenFnr() = AvklarteMedfolgendeFamilie(
        setOf(
            OmfattetFamilie(UUID_BARN_3).apply {
                this.sammensattNavn = BARN_NAVN_3
                this.ident = BARN3_UTEN_FNR
            }
        ), setOf()
    )

    private fun tomFamilie(): AvklarteMedfolgendeFamilie =
        AvklarteMedfolgendeFamilie(setOf(), setOf())

    private fun mockHappyCase() {
        every { mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(any()) } returns lagAvklartMedfølgendeEktefelle()
        every { mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(any()) } returns lagAvklartMedfølgendeBarn()
        every { mockAvklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(any()) } returns lagMedfølgendeEktefelle()
        every { mockAvklarteMedfolgendeFamilieService.hentMedfølgendeBarn(any()) } returns lagMedfølgendeBarn()
        every { mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(any()) } returns lagAvklarteVirksomheter()
        every { mockAvklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns emptyList()
        every { mockLovvalgsperiodeService.hentLovvalgsperioder(any()) } returns listOf(lagLovvalgsperiode())
        every { mockLovvalgsperiodeService.hentLovvalgsperiode(any()) } returns lagLovvalgsperiode()
        every { mockLovvalgsperiodeService.harSelvstendigNæringsdrivendeLovvalgsbestemmelse(any()) } returns false
    }

    private fun lagAvklarteVirksomheter() = listOf(
        AvklartVirksomhet(
            ARBEIDSGIVER_NAVN,
            ORG_NR,
            BrevDataTestUtils.lagStrukturertAdresse(),
            Yrkesaktivitetstyper.LOENNET_ARBEID
        )
    )

    private fun lagAvklartMedfølgendeEktefelle() =
        AvklarteMedfolgendeFamilie(
            setOf(
                OmfattetFamilie(UUID_EKTEFELLE)
            ),
            setOf()
        )

    private fun lagAvklartMedfølgendeBarn() = AvklarteMedfolgendeFamilie(
        setOf(OmfattetFamilie(UUID_BARN_1).apply {
            this.ident = BARN1_FNR
        }), setOf(
            IkkeOmfattetFamilie(
                UUID_BARN_2,
                Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.kode,
                null
            ).apply {
                this.ident = BARN2_FNR
            })
    )

    private fun lagMedfølgendeEktefelle(): Map<String, MedfolgendeFamilie> = mapOf(
        UUID_EKTEFELLE to MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_EKTEFELLE,
            EKTEFELLE_FNR,
            EKTEFELLE_NAVN,
            MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER
        )
    )

    private fun lagMedfølgendeBarn(): Map<String, MedfolgendeFamilie> = mapOf(
        UUID_BARN_1 to MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_BARN_1,
            BARN1_FNR,
            BARN_NAVN_1,
            MedfolgendeFamilie.Relasjonsrolle.BARN
        ),
        UUID_BARN_2 to MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_BARN_2,
            BARN2_FNR,
            BARN_NAVN_2,
            MedfolgendeFamilie.Relasjonsrolle.BARN
        )
    )


    fun lagLovvalgsperiode(): Lovvalgsperiode {
        return Lovvalgsperiode().apply {
            fom = LOVVALGSPERIODE_FOM
            tom = LOVVALGSPERIODE_TOM
            dekning = Trygdedekninger.FULL_DEKNING_FTRL
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1
        }
    }

    fun sjekkAdresser(): List<Arguments> {
        return TrygdeavtaleAdresseSjekkerTest().sjekkAdresser()
    }

    companion object {
        private const val UUID_EKTEFELLE = "uuidEktefelle"
        private const val UUID_BARN_1 = "uuidBarn1"
        private const val UUID_BARN_2 = "uuidBarn2"
        private const val UUID_BARN_3 = "uuidBarn3"
        private const val EKTEFELLE_FNR = "01108049800"
        private const val BARN1_FNR = "01100099728"
        private const val BARN2_FNR = "02109049878"
        private const val BARN_NAVN_1 = "Doffen Duck"
        private const val BARN_NAVN_2 = "Dole Duck"
        private const val BARN_NAVN_3 = "Utenid Duck"
        private const val BARN3_UTEN_FNR = "01.02.2021"
        private const val ARBEIDSGIVER_NAVN = "Bang Hansen"
        private const val EKTEFELLE_NAVN = "Dolly Duck"
        private const val ORG_NR = "987654321"
        private val VEDTAKS_DATO = LocalDate.of(2022, 1, 1)
        private val VEDTAKS_DATO_INSTANT = VEDTAKS_DATO.atStartOfDay(ZoneId.systemDefault()).toInstant()
        private val SOKNADSDATO = LocalDate.of(2000, 1, 1)
        private val FORVENTEDE_FELTER_FOR_INNVILGELSE_STORBRITANNIA_MAPPING = String.format(
            """
            {
              "innvilgelse" : {
                "innledningFritekst" : "innledningFritekst",
                "begrunnelseFritekst" : "begrunnelse",
                "ektefelleFritekst" : "ektefelleFritekst",
                "barnFritekst" : "barnFritekst"
              },
              "artikkel" : "UK_ART6_1",
              "soknad" : {
                "soknadsdato" : "%s",
                "periodeFom" : "%s",
                "periodeTom" : "%s",
                "virksomhetsnavn" : "Bang Hansen",
                "soknadsland" : "Storbritannia"
              },
              "familie" : {
                "minstEttOmfattetFamiliemedlem" : true,
                "ektefelle" : {
                  "navn" : "Dolly Duck",
                  "omfattet" : true,
                  "begrunnelse" : null,
                  "fnr" : "%s",
                  "dnr" : null,
                  "foedselsdato" : "%s"
                },
                "barn" : [ {
                  "navn" : "Doffen Duck",
                  "omfattet" : true,
                  "begrunnelse" : null,
                  "fnr" : "%s",
                  "dnr" : null,
                  "foedselsdato" : "%s"
                }, {
                  "navn" : "Dole Duck",
                  "omfattet" : false,
                  "begrunnelse" : "OVER_18_AR",
                  "fnr" : "%s",
                  "dnr" : null,
                  "foedselsdato" : "%s"
                } ]
              },
              "virksomhetArbeidsgiverSkalHaKopi" : false
            }""".trimIndent(),
            SOKNADSDATO,
            LOVVALGSPERIODE_FOM,
            LOVVALGSPERIODE_TOM,
            EKTEFELLE_FNR,
            LocalDate.of(1980, 10, 1),
            BARN1_FNR,
            LocalDate.of(2000, 10, 1),
            BARN2_FNR,
            LocalDate.of(1990, 10, 2)
        )

        private val FORVENTEDE_FELTER_FOR_ATTEST_STORBRITANNIA_MAPPING = String.format(
            """
            {
              "arbeidstaker" : {
                "navn" : "Donald Duck",
                "foedselsdato" : null,
                "fnr" : "05058892382",
                "bostedsadresse" : [ "Andebygata 1 42 C", "9999", "Norge" ]
              },
              "medfolgendeFamiliemedlemmer" : {
                "ektefelle" : {
                  "navn" : "Dolly Duck",
                  "foedselsdato" : "%s",
                  "fnr" : "%s",
                  "dnr" : null
                },
                "barn" : [ {
                  "navn" : "Doffen Duck",
                  "foedselsdato" : "%s",
                  "fnr" : "%s",
                  "dnr" : null
                } ]
              },
              "arbeidsgiverNorge" : {
                "virksomhetsnavn" : "Bang Hansen",
                "fullstendigAdresse" : [ "Strukturert Gate 12B", "4321", "Poststed", "Bulgaria" ]
              },
              "utsendelse" : {
                "artikkel" : "UK_ART6_1",
                "oppholdsadresse" : [ "Unknown" ],
                "startdato" : "%s",
                "sluttdato" : "%s"
              },
              "representant" : {
                "navn" : "Foretaksnavn",
                "adresse" : [ "Uk address" ]
              },
              "vedtaksdato" : "%s"
            }""".trimIndent(),
            LocalDate.of(1980, 10, 1),
            EKTEFELLE_FNR,
            LocalDate.of(2000, 10, 1),
            BARN1_FNR,
            LOVVALGSPERIODE_FOM,
            LOVVALGSPERIODE_TOM,
            VEDTAKS_DATO
        )
    }
}
