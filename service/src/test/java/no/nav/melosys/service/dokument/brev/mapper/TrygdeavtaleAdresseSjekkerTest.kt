package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.person.Foedsel;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import no.nav.melosys.domain.person.adresse.PersonAdresse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.Collections.emptyList;
import static no.nav.melosys.service.dokument.DokgenTestData.LOVVALGSPERIODE_FOM;
import static no.nav.melosys.service.dokument.DokgenTestData.LOVVALGSPERIODE_TOM;
import static no.nav.melosys.service.dokument.brev.mapper.TrygdeavtaleAdresseSjekker.UKJENT;
import static org.assertj.core.api.Assertions.assertThat;

class TrygdeavtaleAdresseSjekkerTest {

    private final static Lovvalgsperiode LOVVALGSPERIODE = TrygdeavtaleMapperTest.lagLovvalgsperiode();

    @ParameterizedTest(name = "{6}")
    @MethodSource("sjekkAdresser")
    void map_brukNorskBostedsAddresse(
        Land_iso2 landkodeBosted,
        Land_iso2 landkodeOpphold,
        Land_iso2 landkodeKontakt,
        List<String> norskAddresse,
        List<String> trygdeavtaleAddresse,
        Land_iso2 soknadsland,
        String grunn) {

        var persondata = lagPersonopplysninger(landkodeBosted, landkodeOpphold, landkodeKontakt, Optional.empty(), Optional.empty(), Optional.empty());
        var trygdeavtaleAdresseSjekker = new TrygdeavtaleAdresseSjekker(persondata);
        var gyldigNorskAdresse = trygdeavtaleAdresseSjekker.finnGyldigNorskAdresse(soknadsland);
        var gyldigTrygdeavtaleAdresse = trygdeavtaleAdresseSjekker.finnGyldigTrygdeavtaleAdresse(LOVVALGSPERIODE, soknadsland);

        assertThat(gyldigNorskAdresse).withFailMessage(grunn).isEqualTo(norskAddresse);
        assertThat(gyldigTrygdeavtaleAdresse).withFailMessage(grunn).isEqualTo(trygdeavtaleAddresse);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("gyldigePerioder")
    void sjekkOmAdresseGyldighetErInnenforLovalgsperiode_for_gyldigePerioder(Lovvalgsperiode lovvalgsperiode, PersonAdresse personAdresse, String grunn) {
        assertThat(TrygdeavtaleAdresseSjekker.sjekkOmAdresseGyldighetErInnenforLovalgsperiode(personAdresse, lovvalgsperiode))
            .withFailMessage(grunn)
            .isTrue();
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("ugyldigePerioder")
    void sjekkOmAdresseGyldighetErInnenforLovalgsperiode_for_ugyldigePerioder2(Lovvalgsperiode lovvalgsperiode, PersonAdresse personAdresse, String grunn) {
        assertThat(TrygdeavtaleAdresseSjekker.sjekkOmAdresseGyldighetErInnenforLovalgsperiode(personAdresse, lovvalgsperiode))
            .withFailMessage(grunn)
            .isFalse();
    }

    @Test
    void finnGyldigNorskAdresse_brukerHarSemistrukturertAdresse_mappesKorrekt() {
        var semistrukturertAdresse = new SemistrukturertAdresse(
            "adresselinje 1", "adresselinje 2", null, null,
            "postnr", "poststed", Land_iso2.SE.getKode());
        var kontaktadresse = new Kontaktadresse(
            null, semistrukturertAdresse, null, null,
            null, "PDL", null, null, false);
        var persondata = new Personopplysninger(emptyList(), null, null, null,
            new Foedsel(LocalDate.EPOCH, null, null, null), null, null,
            List.of(kontaktadresse), new Navn("Ole", "", "Norman"), emptyList(), emptyList());


        var storbritanniaAdresseSjekker = new TrygdeavtaleAdresseSjekker(persondata);
        var gyldigNorskAdresse = storbritanniaAdresseSjekker.finnGyldigNorskAdresse(Land_iso2.GB);


        assertThat(gyldigNorskAdresse).isEqualTo(
            List.of("Resident outside of Norway", "adresselinje 1 adresselinje 2", "postnr", "poststed", "Sverige")
        );
    }

    static Persondata lagPersonopplysninger(
        Land_iso2 landkodeBosted,
        Land_iso2 landkodeOpphold,
        Land_iso2 landkodeKontakt,
        Optional<String> landkodeBostedValgfritt,
        Optional<String> landkodeOppholdValgfritt,
        Optional<String> landkodeKontaktValgfritt
    ) {

        var gyldigFom = LOVVALGSPERIODE_FOM;
        var gyldigTom = LOVVALGSPERIODE_TOM;

        var kunLandkodeBosted = landkodeBostedValgfritt.orElse(kodeEllerNull(landkodeBosted));
        var kunLandkodeOpphold = landkodeOppholdValgfritt.orElse(kodeEllerNull(landkodeOpphold));
        var kunLandkodeKontakt = landkodeKontaktValgfritt.orElse(kodeEllerNull(landkodeKontakt));

        final var bostedsadresse = new Bostedsadresse(
            new StrukturertAdresse("bosted", "42 C", null, null, null, kunLandkodeBosted),
            null, gyldigFom, gyldigTom, "PDL", null, false);

        final var kontaktadresse = new Kontaktadresse(
            new StrukturertAdresse("kontakt 1", null, null, null, null, kunLandkodeKontakt),
            null, null, gyldigFom, gyldigTom, "PDL", null, null,
            false);

        final var oppholdsadresse = new Oppholdsadresse(
            new StrukturertAdresse("tilleggOpphold", "opphold 1", null, null, null,
                null, null, kunLandkodeOpphold), null,
            LOVVALGSPERIODE_FOM, LOVVALGSPERIODE_TOM,
            "PDL", null, null, false);

        return new Personopplysninger(emptyList(), bostedsadresse, null, null,
            new Foedsel(LocalDate.EPOCH, null, null, null), null, null,
            List.of(kontaktadresse), new Navn("Ole", "", "Norman"), List.of(oppholdsadresse), emptyList());
    }

    private static String kodeEllerNull(Land_iso2 landkoder) {
        return landkoder == null ? null : landkoder.getKode();
    }

    static List<Arguments> sjekkAdresser() {
        return List.of(
            Arguments.of(Land_iso2.NO, Land_iso2.NO, Land_iso2.NO,
                List.of("bosted 42 C", "Norge"),
                List.of(UKJENT), Land_iso2.GB,
                "Velg bosted, når alle adresser er norske"),
            Arguments.of(Land_iso2.SE, Land_iso2.SE, Land_iso2.NO,
                List.of("kontakt 1", "Norge"),
                List.of(UKJENT), Land_iso2.GB,
                "Kun kontakt med norsk adresse"),
            Arguments.of(Land_iso2.SE, Land_iso2.NO, Land_iso2.SE,
                List.of("tilleggOpphold", "opphold 1", "Norge"),
                List.of(UKJENT), Land_iso2.GB,
                "Kun opphold med norsk adresse"),
            Arguments.of(Land_iso2.NO, Land_iso2.SE, Land_iso2.SE,
                List.of("bosted 42 C", "Norge"),
                List.of(UKJENT),
                Land_iso2.US,
                "Kun bosted med norsk adresse"),
            Arguments.of(Land_iso2.GB, Land_iso2.SE, Land_iso2.SE,
                List.of("No address in Norway"),
                List.of("bosted 42 C", "Storbritannia"),
                Land_iso2.GB,
                "Ingen norske adresser, men adresse i UK"),
            Arguments.of(Land_iso2.GB, null, null,
                List.of("No address in Norway"),
                List.of("bosted 42 C", "Storbritannia"),
                Land_iso2.GB,
                "kun adresse i UK"),
            Arguments.of(Land_iso2.US, null, null,
                List.of("No address in Norway"),
                List.of("bosted 42 C", "USA"),
                Land_iso2.US,
                "kun adresse i US"),
            Arguments.of(Land_iso2.SE, Land_iso2.SE, Land_iso2.SE,
                List.of("Resident outside of Norway", "bosted 42 C", "Sverige"),
                List.of(UKJENT), Land_iso2.GB,
                "Utenlandsk adresse, men ikke i UK"),
            Arguments.of(null, null, null,
                List.of(UKJENT),
                List.of(UKJENT),
                Land_iso2.US,
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
