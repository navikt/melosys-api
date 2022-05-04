package no.nav.melosys.service.dokument.brev.mapper;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.person.Foedsel;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import no.nav.melosys.domain.person.adresse.PersonAdresse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static no.nav.melosys.service.dokument.DokgenTestData.*;
import static no.nav.melosys.service.dokument.brev.mapper.StorbritanniaAdresseSjekker.UKJENT;
import static org.assertj.core.api.Assertions.assertThat;

class StorbritaniaAdresseSjekkerTest {

    private final static Lovvalgsperiode LOVVALGSPERIODE = StorbritanniaMapperTest.lagLovvalgsperiode();

    @ParameterizedTest(name = "{5}")
    @MethodSource("sjekkAdresser")
    void map_brukNorskBostedsAddresse(
        Landkoder landkodeBosted,
        Landkoder landkodeOpphold,
        Landkoder landkodeKontakt,
        List<String> norskAddresse,
        List<String> ukAddresse,
        String grunn) {

        var persondata = lagPersonopplysninger(landkodeBosted, landkodeOpphold, landkodeKontakt);
        var storbritaniaAdresseSjekker = new StorbritanniaAdresseSjekker(persondata);
        var gyldigNorskAdresse = storbritaniaAdresseSjekker.finnGyldigNorskAdresse();
        var gyldigUkAdresse = storbritaniaAdresseSjekker.finnGyldigStorbritanniaAdresse(LOVVALGSPERIODE);

        assertThat(gyldigNorskAdresse).withFailMessage(grunn).isEqualTo(norskAddresse);
        assertThat(gyldigUkAdresse).withFailMessage(grunn).isEqualTo(ukAddresse);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("gyldigePerioder")
    void sjekkOmAdresseGyldighetErInnenforLovalgsperiode_for_gyldigePerioder(Lovvalgsperiode lovvalgsperiode, PersonAdresse personAdresse, String grunn) {
        assertThat(StorbritanniaAdresseSjekker.sjekkOmAdresseGyldighetErInnenforLovalgsperiode(personAdresse, lovvalgsperiode))
            .withFailMessage(grunn)
            .isTrue();
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("ugyldigePerioder")
    void sjekkOmAdresseGyldighetErInnenforLovalgsperiode_for_ugyldigePerioder2(Lovvalgsperiode lovvalgsperiode, PersonAdresse personAdresse, String grunn) {
        assertThat(StorbritanniaAdresseSjekker.sjekkOmAdresseGyldighetErInnenforLovalgsperiode(personAdresse, lovvalgsperiode))
            .withFailMessage(grunn)
            .isFalse();
    }

    static Persondata lagPersonopplysninger(
        Landkoder landkodeBosted,
        Landkoder landkodeOpphold,
        Landkoder landkodeKontakt) {

        var gyldigFom = LOVVALGSPERIODE_FOM;
        var gyldigTom = LOVVALGSPERIODE_TOM;

        final var bostedsadresse = new Bostedsadresse(
            new StrukturertAdresse("bosted", "42 C", null, null, null, kodeEllerNull(landkodeBosted)),
            null, gyldigFom, gyldigTom, "PDL", null, false);

        final var kontaktadresse = new Kontaktadresse(
            new StrukturertAdresse("kontakt 1", null, null, null, null, kodeEllerNull(landkodeKontakt)),
            null, null, gyldigFom, gyldigTom, "PDL", null, null,
            false);

        final var oppholdsadresse = new Oppholdsadresse(
            new StrukturertAdresse("tilleggOpphold", "opphold 1", null, null, null,
                null, null, kodeEllerNull(landkodeOpphold)), null,
            LOVVALGSPERIODE_FOM, LOVVALGSPERIODE_TOM,
            "PDL", null, null, false);

        return new Personopplysninger(Collections.emptyList(), bostedsadresse, null, null,
            new Foedsel(LocalDate.EPOCH, null, null, null), null, null,
            List.of(kontaktadresse), new Navn("Ole", "", "Norman"), List.of(oppholdsadresse), Collections.emptyList());
    }

    private static String kodeEllerNull(Landkoder landkoder) {
        return landkoder == null ? null : landkoder.getKode();
    }

    static List<Arguments> sjekkAdresser() {
        return List.of(
            Arguments.of(Landkoder.NO, Landkoder.NO, Landkoder.NO,
                List.of("bosted 42 C", "Norge"),
                List.of(UKJENT),
                "Velg bosted, når alle adresser er norske"),
            Arguments.of(Landkoder.SE, Landkoder.SE, Landkoder.NO,
                List.of("kontakt 1", "Norge"),
                List.of(UKJENT),
                "Kun kontakt med norsk adresse"),
            Arguments.of(Landkoder.SE, Landkoder.NO, Landkoder.SE,
                List.of("tilleggOpphold", "opphold 1", "Norge"),
                List.of(UKJENT),
                "Kun opphold med norsk adresse"),
            Arguments.of(Landkoder.NO, Landkoder.SE, Landkoder.SE,
                List.of("bosted 42 C", "Norge"),
                List.of(UKJENT),
                "Kun bosted med norsk adresse"),
            Arguments.of(Landkoder.GB, Landkoder.SE, Landkoder.SE,
                List.of("No address in Norway"),
                List.of("bosted 42 C", "Storbritannia"),
                "Ingen norske adresser, men adresse i UK"),
            Arguments.of(Landkoder.GB, null, null,
                List.of("No address in Norway"),
                List.of("bosted 42 C", "Storbritannia"),
                "kun adresse i UK"),
            Arguments.of(Landkoder.SE, Landkoder.SE, Landkoder.SE,
                List.of("Resident outside of Norway", "bosted 42 C", "Sverige"),
                List.of(UKJENT),
                "Utenlandsk adresse, men ikke i UK"),
            Arguments.of(null, null, null,
                List.of(UKJENT),
                List.of(UKJENT),
                "ingen adresser")
        );
    }

    private static List<Arguments> gyldigePerioder() {
        return List.of(
            Arguments.of(
                lagLovvalgsperiode(
                    LocalDate.of(2020, 1, 1),
                    LocalDate.of(2021, 1, 1)),
                lagPersonAdresse(
                    LocalDate.of(2020, 1, 1),
                    LocalDate.of(2021, 1, 1)),
                "lovalgsperiode er lik adresseperiode"
            ),
            Arguments.of(
                lagLovvalgsperiode(
                    LocalDate.of(2020, 1, 1),
                    LocalDate.of(2021, 1, 1)),
                lagPersonAdresse(
                    LocalDate.of(2020, 2, 1),
                    LocalDate.of(2020, 3, 1)),
                "lovalgsperiode har start før og slutt etter adresseperiode"
            ),
            Arguments.of(
                lagLovvalgsperiode(
                    LocalDate.of(2020, 2, 1),
                    LocalDate.of(2020, 3, 1)),
                lagPersonAdresse(
                    LocalDate.of(2020, 1, 1),
                    LocalDate.of(2021, 1, 1)),
                "adresseperiode har start før og slutt etter lovalgsperiode"
            ),
            Arguments.of(
                lagLovvalgsperiode(
                    LocalDate.of(2021, 1, 1),
                    LocalDate.of(2022, 1, 1)),
                lagPersonAdresse(
                    LocalDate.of(2020, 1, 1),
                    LocalDate.of(2021, 1, 1)),
                "lovalgsperiode start er lik adresseperiode slutt"
            ),
            Arguments.of(
                lagLovvalgsperiode(
                    LocalDate.of(2020, 1, 1),
                    LocalDate.of(2021, 1, 1)),
                lagPersonAdresse(
                    LocalDate.of(2021, 1, 1),
                    LocalDate.of(2022, 1, 1)),
                "lovalgsperiode slutt er lik adresseperiode start"
            ),
            Arguments.of(
                lagLovvalgsperiode(
                    LocalDate.of(2020, 1, 1),
                    LocalDate.of(2021, 1, 1)),
                lagPersonAdresse(
                    LocalDate.of(2021, 1, 1),
                    null),
                "personadresse gyldigTom er null"
            )
        );
    }

    private static List<Arguments> ugyldigePerioder() {
        return List.of(
            Arguments.of(
                lagLovvalgsperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1)),
                lagPersonAdresse(LocalDate.of(2020, 2, 1), LocalDate.of(2021, 1, 1)),
                "lovalgsperiode er før adresseperiode"
            ),
            Arguments.of(
                lagLovvalgsperiode(LocalDate.of(2021, 1, 2), LocalDate.of(2022, 1, 1)),
                lagPersonAdresse(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1)),
                "lovalgsperiode er etter adresseperiode"
            ),
            Arguments.of(
                lagLovvalgsperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1)),
                lagPersonAdresse(null, null),
                "adresseperiode fom og tom er null"
            )
        );
    }

    private static PersonAdresse lagPersonAdresse(LocalDate gyldigFom, LocalDate gyldigTom) {
        return new Oppholdsadresse(null,
            null,
            gyldigFom,
            gyldigTom,
            null,
            null,
            null,
            false
        );
    }

    private static Lovvalgsperiode lagLovvalgsperiode(LocalDate lovFom, LocalDate lovTom) {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(lovFom);
        lovvalgsperiode.setTom(lovTom);
        return lovvalgsperiode;
    }
}
