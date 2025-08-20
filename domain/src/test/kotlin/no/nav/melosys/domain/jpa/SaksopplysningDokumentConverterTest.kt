package no.nav.melosys.domain.jpa

import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
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
import org.jeasy.random.FieldPredicates.*
import org.jeasy.random.randomizers.misc.EnumRandomizer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class SaksopplysningDokumentConverterTest {

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

    companion object {
        private lateinit var converter: SaksopplysningDokumentConverter
        private lateinit var random: EasyRandom

        @JvmStatic
        @BeforeAll
        fun setUp() {
            converter = SaksopplysningDokumentConverter()
            random = EasyRandom(
                EasyRandomParameters()
                    .overrideDefaultInitialization(true)
                    .collectionSizeRange(1, 4)
                    .stringLengthRange(2, 10)
                    .scanClasspathForConcreteTypes(true)
                    // Enhetsregistret har bare SemistrukturertAdresser
                    .randomize(
                        inClass(OrganisasjonsDetaljer::class.java).and(named("forretningsadresse"))
                    ) { listOf(random.nextObject(SemistrukturertAdresse::class.java)) }
                    .randomize(
                        inClass(OrganisasjonsDetaljer::class.java).and(named("postadresse"))
                    ) { listOf(random.nextObject(SemistrukturertAdresse::class.java)) }
                    .randomize(ofType(LovvalgBestemmelse::class.java)) {
                        EnumRandomizer(Lovvalgbestemmelser_883_2004::class.java).randomValue
                    }
                    .randomize(
                        inClass(PersonDokument::class.java).and(named("midlertidigPostadresse"))
                    ) { random.nextObject(MidlertidigPostadresseNorge::class.java) }
                    .randomize(
                        inClass(ArbeidsInntektInformasjon::class.java).and(named("inntektListe"))
                    ) { listOf(random.nextObject(Inntekt::class.java), random.nextObject(Inntekt::class.java)) }
            )
        }
    }
}
