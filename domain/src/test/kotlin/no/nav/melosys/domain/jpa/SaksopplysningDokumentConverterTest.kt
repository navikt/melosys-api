package no.nav.melosys.domain.jpa

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon
import no.nav.melosys.domain.dokument.inntekt.Inntekt
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.person.PersonopplysningerObjectFactory.lagPersonMedHistorikk
import no.nav.melosys.domain.person.PersonopplysningerObjectFactory.lagPersonopplysninger
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.randomizers.misc.EnumRandomizer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaksopplysningDokumentConverterTest {
    private lateinit var converter: SaksopplysningDokumentConverter
    private lateinit var random: EasyRandom

    @BeforeAll
    fun init() {
        converter = SaksopplysningDokumentConverter()
        random = EasyRandom(
            EasyRandomParameters()
                .overrideDefaultInitialization(true)
                .collectionSizeRange(1, 4)
                .stringLengthRange(2, 10)
                .scanClasspathForConcreteTypes(true)
                // Enhetsregistret har bare SemistrukturertAdresser
                .randomize(
                    FieldPredicates.inClass(OrganisasjonsDetaljer::class.java).and(FieldPredicates.named("forretningsadresse"))
                ) { listOf<SemistrukturertAdresse>(random.nextObject(SemistrukturertAdresse::class.java)) }
                .randomize(
                    FieldPredicates.inClass(OrganisasjonsDetaljer::class.java).and(FieldPredicates.named("postadresse"))
                ) { listOf<SemistrukturertAdresse>(random.nextObject(SemistrukturertAdresse::class.java)) }
                .randomize(FieldPredicates.ofType(LovvalgBestemmelse::class.java)) {
                    EnumRandomizer(Lovvalgbestemmelser_883_2004::class.java).randomValue
                }
                .randomize(
                    FieldPredicates.inClass(PersonDokument::class.java).and(FieldPredicates.named("midlertidigPostadresse"))
                ) { random.nextObject(MidlertidigPostadresseNorge::class.java) }
                .randomize(
                    FieldPredicates.inClass(ArbeidsInntektInformasjon::class.java).and(FieldPredicates.named("inntektListe"))
                ) { listOf<Inntekt>(random.nextObject(Inntekt::class.java), random.nextObject(Inntekt::class.java)) }
        )
    }

    @Test
    fun konverterTilOgFraDatabase_medArbeidsforholdDokument_erUendret() {
        testKonvertering(ArbeidsforholdDokument::class.java)
    }

    @Test
    fun konverterTilOgFraDatabase_medInntektDokument_erUendret() {
        testKonvertering(InntektDokument::class.java)
    }

    @Test
    fun konverterTilOgFraDatabase_medMedlemskapDokument_erUendret() {
        testKonvertering(MedlemskapDokument::class.java)
    }

    @Test
    fun konverterTilOgFraDatabase_medOrganisasjonDokument_erUendret() {
        testKonvertering(OrganisasjonDokument::class.java)
    }

    @Test
    fun konverterTilOgFraDatabase_medPersonDokument_erUendret() {
        testKonvertering(PersonDokument::class.java)
    }

    @Test
    fun konverterTilOgFraDatabase_medPersonhistorikkDokument_erUendret() {
        testKonvertering(PersonhistorikkDokument::class.java)
    }

    @Test
    fun konverterTilOgFraDatabase_medSedDokument_erUendret() {
        testKonvertering(SedDokument::class.java)
    }

    @Test
    fun konverterTilOgFraDatabase_medUtbetalingDokument_erUendret() {
        testKonvertering(UtbetalingDokument::class.java)
    }

    @Test
    fun konverterTilOgFraDatabase_medPersonopplysninger_erUendret() {
        val personopplysninger = lagPersonopplysninger()
        testKonverteringObject(personopplysninger)
    }

    @Test
    fun konverterTilOgFraDatabase_medPersonMedHistorikk_erUendret() {
        val personMedHistorikk = lagPersonMedHistorikk()
        testKonverteringObject(personMedHistorikk)
    }

    @Test
    fun `konverterTilDatabase serialiserer Land som JSON-objekt med kode-felt`() {
        val personDokument = PersonDokument(
            fnr = "12345678901",
            statsborgerskap = no.nav.melosys.domain.dokument.felles.Land("NOR")
        )


        val json = converter.convertToDatabaseColumn(personDokument)


        json shouldContain """"statsborgerskap":{"kode":"NOR"}"""
        json shouldNotContain """"statsborgerskap":"NOR""""
    }

    @Test
    fun `konverterFraDatabase kan deserialisere Land fra JSON-objekt`() {
        val json = """{"type":"PersonDokument","fnr":"12345678901","statsborgerskap":{"kode":"DNK"}}"""


        val deserialisert = converter.convertToEntityAttribute(json) as PersonDokument


        deserialisert.statsborgerskap?.kode shouldBe "DNK"
    }

    @Test
    fun `konverterFraDatabase kan deserialisere Land fra string-verdi`() {
        // VIKTIG: Hvis @JsonCreator fjernes fra Land.av() vil denne testen feile med:
        // "Cannot construct instance of Land: no String-argument constructor/factory method"
        val json = """{"type":"PersonDokument","fnr":"12345678901","statsborgerskap":"SWE"}"""


        val deserialisert = converter.convertToEntityAttribute(json) as PersonDokument


        deserialisert.statsborgerskap?.kode shouldBe "SWE"
    }

    @Test
    fun `round-trip serialisering av Land bevarer data`() {
        val original = PersonDokument(
            fnr = "12345678901",
            statsborgerskap = no.nav.melosys.domain.dokument.felles.Land("SWE")
        )


        val json = converter.convertToDatabaseColumn(original)
        val deserialisert = converter.convertToEntityAttribute(json) as PersonDokument


        with(deserialisert) {
            statsborgerskap?.kode shouldBe "SWE"
            statsborgerskap?.hentKode() shouldBe "SWE"
        }
    }

    @Test
    fun `konverterTilDatabase håndterer null Land-verdi`() {
        val personDokument = PersonDokument(
            fnr = "12345678901",
            statsborgerskap = null
        )


        val json = converter.convertToDatabaseColumn(personDokument)
        val deserialisert = converter.convertToEntityAttribute(json) as PersonDokument


        deserialisert.statsborgerskap.shouldBeNull()
    }

    @Test
    fun `konverterFraDatabase kan deserialisere LocalDate fra gammelt Jackson2 array-format`() {
        // Jackson 2 med WRITE_DATES_AS_TIMESTAMPS=true lagret datoer som arrays [år, måned, dag].
        // Jackson 3 skriver ISO-strenger, men må fortsatt kunne lese gammel data fra DB.
        // Vi serialiserer med ISO-format, bytter ut med arrays og verifiserer backward-kompatibilitet.
        val dokument = ArbeidsforholdDokument(
            arbeidsforhold = listOf(Arbeidsforhold().apply {
                arbeidsgiverID = "999888777"
                ansettelsesPeriode = Periode(LocalDate.of(2021, 6, 2), LocalDate.of(2022, 1, 1))
            })
        )
        val isoJson = converter.convertToDatabaseColumn(dokument)!!
        val arrayJson = isoJson
            .replace("\"2021-06-02\"", "[2021,6,2]")
            .replace("\"2022-01-01\"", "[2022,1,1]")

        val deserialisert = converter.convertToEntityAttribute(arrayJson) as ArbeidsforholdDokument

        val periode = deserialisert.arbeidsforhold.first().ansettelsesPeriode!!
        periode.fom shouldBe LocalDate.of(2021, 6, 2)
        periode.tom shouldBe LocalDate.of(2022, 1, 1)
    }

    @Test
    fun `konverterTilDatabase serialiserer LocalDate som ISO-streng i Jackson3`() {
        // Jackson 2 med WRITE_DATES_AS_TIMESTAMPS=true lagret datoer som arrays.
        // Jackson 3 skal nå skrive ISO-strenger for nye data.
        val dokument = ArbeidsforholdDokument(
            arbeidsforhold = listOf(Arbeidsforhold().apply {
                arbeidsgiverID = "999888777"
                ansettelsesPeriode = Periode(LocalDate.of(2021, 6, 2), LocalDate.of(2022, 1, 1))
            })
        )

        val json = converter.convertToDatabaseColumn(dokument)!!

        json shouldContain """"2021-06-02""""
        json shouldContain """"2022-01-01""""
        json shouldNotContain "[2021"
    }

    @Test
    fun `konverterFraDatabase kan deserialisere LocalDateTime fra gammelt Jackson2 array-format`() {
        // Jackson 2 med WRITE_DATES_AS_TIMESTAMPS=true lagret LocalDateTime som arrays [år,måned,dag,time,minutt,sekund].
        // Jackson 3 skriver ISO-strenger, men må fortsatt kunne lese gammel data fra DB.
        val json = """
            {"type":"PersonhistorikkDokument","bostedsadressePeriodeListe":[{"endringstidspunkt":[2023,7,1,12,30,45]}]}
        """.trimIndent()

        val deserialisert = converter.convertToEntityAttribute(json) as no.nav.melosys.domain.dokument.person.PersonhistorikkDokument

        deserialisert.bostedsadressePeriodeListe.first().endringstidspunkt shouldBe LocalDateTime.of(2023, 7, 1, 12, 30, 45)
    }

    @Test
    fun `konverterFraDatabase kan deserialisere YearMonth fra gammelt Jackson2 array-format`() {
        // Jackson 2 med WRITE_DATES_AS_TIMESTAMPS=true lagret YearMonth som arrays [år,måned].
        // Jackson 3 skriver ISO-strenger, men må fortsatt kunne lese gammel data fra DB.
        val json = """
            {"type":"InntektDokument","arbeidsInntektMaanedListe":[{"aarMaaned":[2022,3],"arbeidsInntektInformasjon":{"inntektListe":[]}}]}
        """.trimIndent()

        val deserialisert = converter.convertToEntityAttribute(json) as InntektDokument

        deserialisert.arbeidsInntektMaanedListe.first().aarMaaned shouldBe YearMonth.of(2022, 3)
    }

    private fun <T : SaksopplysningDokument> testKonverteringObject(saksopplysningDokument: T) {
        val json = converter.convertToDatabaseColumn(saksopplysningDokument)
        val deserialisertDokument = converter.convertToEntityAttribute(json)


        // Using AssertJ for recursive comparison
        org.assertj.core.api.Assertions.assertThat(deserialisertDokument)
            .usingRecursiveComparison()
            .isEqualTo(saksopplysningDokument)
    }

    private fun <T : SaksopplysningDokument> testKonvertering(clazz: Class<T>) {
        val opprinneligTestDokument = random.nextObject(clazz)
        testKonverteringObject(opprinneligTestDokument)
    }
}
