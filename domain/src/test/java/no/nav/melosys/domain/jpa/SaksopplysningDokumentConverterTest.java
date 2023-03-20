package no.nav.melosys.domain.jpa;

import java.util.List;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.Loennsinntekt;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.person.PersonMedHistorikk;
import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.domain.person.PersonopplysningerObjectFactory;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.jeasy.random.FieldPredicates.*;

class SaksopplysningDokumentConverterTest {
    private static SaksopplysningDokumentConverter converter;
    private static EasyRandom random;

    @BeforeAll
    static void setUp() {
        converter = new SaksopplysningDokumentConverter();
        random = new EasyRandom(new EasyRandomParameters()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .stringLengthRange(2, 10)
            .scanClasspathForConcreteTypes(true)
            // Enhetsregistret har bare SemistrukturertAdresser
            .randomize(inClass(OrganisasjonsDetaljer.class).and(named("forretningsadresse")),
                () -> List.of(random.nextObject(SemistrukturertAdresse.class)))
            .randomize(inClass(OrganisasjonsDetaljer.class).and(named("postadresse")),
                () -> List.of(random.nextObject(SemistrukturertAdresse.class)))
            .randomize(ofType(LovvalgBestemmelse.class),
                () -> new EnumRandomizer<>(Lovvalgbestemmelser_883_2004.class).getRandomValue())
            .randomize(inClass(PersonDokument.class).and(named("midlertidigPostadresse")),
                () -> random.nextObject(MidlertidigPostadresseNorge.class))
            .randomize(inClass(ArbeidsInntektInformasjon.class).and(named("inntektListe")),
                () -> List.of(random.nextObject(Inntekt.class), random.nextObject(Loennsinntekt.class))));
    }

    @Test
    void konverterTilOgFraDatabase_medArbeidsforholdDokument_erUendret() {
        testKonvertering(ArbeidsforholdDokument.class);
    }

    @Test
    void konverterTilOgFraDatabase_medInntektDokument_erUendret() {
        testKonvertering(InntektDokument.class);
    }

    @Test
    void konverterTilOgFraDatabase_medMedlemskapDokument_erUendret() {
        testKonvertering(MedlemskapDokument.class);
    }

    @Test
    void konverterTilOgFraDatabase_medOrganisasjonDokument_erUendret() {
        testKonvertering(OrganisasjonDokument.class);
    }

    @Test
    void konverterTilOgFraDatabase_medPersonDokument_erUendret() {
        testKonvertering(PersonDokument.class);
    }

    @Test
    void konverterTilOgFraDatabase_medPersonhistorikkDokument_erUendret() {
        testKonvertering(PersonhistorikkDokument.class);
    }

    @Test
    void konverterTilOgFraDatabase_medSedDokument_erUendret() {
        testKonvertering(SedDokument.class);
    }

    @Test
    void konverterTilOgFraDatabase_medUtbetalingDokument_erUendret() {
        testKonvertering(UtbetalingDokument.class);
    }

    @Test
    void konverterTilOgFraDatabase_medPersonopplysninger_erUendret() {
        Personopplysninger personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger();
        testKonverteringObject(personopplysninger);
    }

    @Test
    void konverterTilOgFraDatabase_medPersonMedHistorikk_erUendret() {
        PersonMedHistorikk personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk();
        testKonverteringObject(personMedHistorikk);
    }

    private <T extends SaksopplysningDokument> void testKonverteringObject(T saksopplysningDokument) {
        String json = converter.convertToDatabaseColumn(saksopplysningDokument);
        SaksopplysningDokument deserialisertDokument = converter.convertToEntityAttribute(json);
        assertThat(deserialisertDokument).usingRecursiveComparison().isEqualTo(saksopplysningDokument);
    }

    private <T extends SaksopplysningDokument> void testKonvertering(Class<T> clazz) {
        T opprinneligTestDokument = random.nextObject(clazz);
        testKonverteringObject(opprinneligTestDokument);
    }
}
