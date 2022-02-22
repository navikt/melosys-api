package no.nav.melosys.service.persondata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Personstatuser;
import no.nav.melosys.domain.person.*;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.domain.person.familie.Familierelasjon;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class PersonopplysningerObjectFactory {
    public static Personopplysninger lagPersonopplysninger() {
        return lagPersonopplysninger(false, false, false, false, false);
    }

    public static Personopplysninger lagPersonopplysningerStatløs() {
        return lagPersonopplysninger(true, false, false, false, false);
    }

    public static Personopplysninger lagPersonopplysningerUtenBostedsadresse() {
        return lagPersonopplysninger(false, true, false, false, false);
    }

    public static Personopplysninger lagPersoopplysningerUtenOppholdsadresse() {
        return lagPersonopplysninger(false, false, true, false, false);

    }

    public static Personopplysninger lagPersoopplysningerUtenKontaktadresse() {
        return lagPersonopplysninger(false, false, false, true, false);
    }

    public static Personopplysninger lagPersonopplysningerMedFamilie() {
        return lagPersonopplysninger(false, false, false, false, true);
    }

    public static Personopplysninger lagPersonopplysningerKontaktadresseSemistrukturert(boolean erUtenOppholdsadresse){
        return new Personopplysninger(emptyList(), lagBostedsadresse(), null, emptySet(),
            lagFødesel(), null, lagKjønn(), lagKontaktadresser(true), lagNavn(), erUtenOppholdsadresse ? emptySet() : lagOppholdsadresser(),
            lagStatsborgerskap(false));
    }

    private static Personopplysninger lagPersonopplysninger(boolean erStatløs, boolean erUtenBostedsadresse, boolean erUtenOppholdsadresse,
                                                            boolean erUtenKontaktadresse, boolean harFamilie) {
        return new Personopplysninger(emptyList(), erUtenBostedsadresse ? null : lagBostedsadresse(), null,
            harFamilie ? Set.of(lagRelatertVedsivilstand(), lagBarn()) : emptySet(),
            lagFødesel(), null, lagKjønn(), erUtenKontaktadresse ? emptySet() : lagKontaktadresser(false),
            lagNavn(), erUtenOppholdsadresse ? emptySet() : lagOppholdsadresser(),
            lagStatsborgerskap(erStatløs));

    }

    public static Personopplysninger lagPersonopplysningerUtenAdresser() {
        return new Personopplysninger(emptyList(), null, null, emptySet(), lagFødesel(), null,
            lagKjønn(), emptyList(), lagNavn(), emptyList(), lagStatsborgerskap(false));
    }

    private static Foedsel lagFødesel() {
        return new Foedsel(LocalDate.EPOCH, null, null, null);
    }

    private static KjoennType lagKjønn() {
        return KjoennType.UKJENT;
    }

    private static Bostedsadresse lagBostedsadresse() {
        return new Bostedsadresse(new StrukturertAdresse("gatenavnFraBostedsadresse", "3", "1234", "Oslo", "Norge", "NO"),
            null, null, null, null, null, false);
    }

    public static Collection<Kontaktadresse> lagKontaktadresser(boolean semistruktuert) {
        if(semistruktuert){
            return Set.of(
                new Kontaktadresse(
                    null,
                    lagSemistrukturertAdresse(),
                    null,
                    null,
                    null,
                    Master.PDL.name(),
                    null,
                    LocalDateTime.MAX.minusYears(43),
                    false
                )
            );
        }
        return Set.of(
            new Kontaktadresse(
                lagStrukturertAdresse("gatenavnKontaktadressePDL"),
                null,
                null,
                null,
                null,
                Master.PDL.name(),
                null,
                LocalDateTime.MAX.minusYears(42),
                false
            ),
            new Kontaktadresse(
                lagStrukturertAdresse("gammelGatenavnKontaktadressePDL"),
                null,
                null,
                null,
                null,
                Master.PDL.name(),
                null,
                LocalDateTime.MIN,
                false
            ),
            new Kontaktadresse(
                lagStrukturertAdresse("gatenavnKontaktadresseFreg"),
                null,
                null,
                null,
                null,
                Master.FREG.name(),
                null,
                LocalDateTime.MAX,
                false
            )
        );
    }

    private static Collection<Oppholdsadresse> lagOppholdsadresser() {
        return Set.of(
            new Oppholdsadresse(
                lagStrukturertAdresse("gammelGatenavnOppholdsadresseFreg"),
                null,
                null,
                null,
                Master.FREG.name(),
                null,
                LocalDateTime.MIN,
                false
            ),
            new Oppholdsadresse(
                lagStrukturertAdresse("gatenavnOppholdsadresseFreg"),
                null,
                null,
                null,
                Master.FREG.name(),
                null,
                LocalDateTime.MAX,
                false
            )
        );
    }

    private static StrukturertAdresse lagStrukturertAdresse(String gatenavn) {
        return new StrukturertAdresse(gatenavn, null, "0123", "Poststed", null, "NO");
    }

    private static SemistrukturertAdresse lagSemistrukturertAdresse(){
        return new SemistrukturertAdresse("Kranstien 3", "0338 Oslo", null,null, "0338", null, Landkoder.NO.name());
    }

    private static Navn lagNavn() {
        return new Navn("Ola", null, "Nordmann");
    }

    private static Collection<Statsborgerskap> lagStatsborgerskap(boolean erStatløs) {
        if (erStatløs) {
            return List.of(new Statsborgerskap(Land.STATSLØS,
                null,
                LocalDate.EPOCH,
                LocalDate.now(),
                "PDL",
                "Dolly",
                false));
        }
        return List.of(
            new Statsborgerskap("NOR", null, LocalDate.parse("2009-11-18"),
                LocalDate.parse("1980-11-18"), "PDL", "Dolly", false),
            new Statsborgerskap("SWE", null, LocalDate.parse("1979-11-18"),
                LocalDate.parse("1980-11-18"), "PDL", "Dolly", false),
            new Statsborgerskap("DNK", null, null,
                LocalDate.parse("1980-11-18"), "PDL",
                "Dolly", false)
        );
    }

    public static PersonMedHistorikk lagPersonMedHistorikk() {
        final var bostedsadresse_1 = new Bostedsadresse(
            new StrukturertAdresse("gate1", "42 C", null, null, null, null),
            null, null, null, "PDL", null, false);
        final var bostedsadresse_2 = new Bostedsadresse(
            new StrukturertAdresse("gate2", null, null, null, null, null),
            null, null, null, null, null, true);

        final var kontaktadresse_1 = new Kontaktadresse(
            new StrukturertAdresse("kontakt 1", null, null, null, null, null), null, null, null, null, "PDL", null, null,
            false);
        final var kontaktadresse_2 = new Kontaktadresse(null,
            new SemistrukturertAdresse("kontakt 2", "linje 2", null, null, "1234", "By", "IT"), null, null, null, null,
            null, null, false);

        final var oppholdsadresse_1 = new Oppholdsadresse(
            new StrukturertAdresse("opphold 1", null, null, null, null, null), null, null, null, "PDL", null, null,
            false);
        final var oppholdsadresse_2 = new Oppholdsadresse(
            new StrukturertAdresse("opphold 2", null, null, null, null, null), null, null, null, null, null, null,
            true);

        final var statsborgerskap_1 = new Statsborgerskap("AAA", null, LocalDate.parse("2009-11-18"),
            LocalDate.parse("1980-11-18"), "PDL", "Dolly", false);
        final var statsborgerskap_2 = new Statsborgerskap("BBB", null, LocalDate.parse("1979-11-18"),
            LocalDate.parse("1980-11-18"), "PDL", "Dolly", false);
        final var statsborgerskap_3 = new Statsborgerskap("CCC", null, null, LocalDate.parse("1980-11-18"), "PDL",
            "Dolly", false);

        return new PersonMedHistorikk(Set.of(bostedsadresse_1, bostedsadresse_2),
            null, null, new Folkeregisteridentifikator("identNr"),
            Set.of(new Folkeregisterpersonstatus(Personstatuser.UDEFINERT, "ny status fra PDL", Master.PDL.name(), Master.PDL.name(), null, false)),
            KjoennType.UKJENT,
            Set.of(kontaktadresse_1, kontaktadresse_2), new Navn("Ola", "Oops", "King"),
            Set.of(oppholdsadresse_1, oppholdsadresse_2), Set.of(
            new Sivilstand(Sivilstandstype.REGISTRERT_PARTNER, null, "relatertVedSivilstandID", LocalDate.MIN,
                LocalDate.EPOCH, "PDL", "kilde", false),
            new Sivilstand(Sivilstandstype.UDEFINERT, "Udefinert type", "relatertVedSivilstandID", LocalDate.MIN,
                LocalDate.EPOCH, "PDL", "kilde", false)),
            Set.of(statsborgerskap_1, statsborgerskap_2, statsborgerskap_3));
    }

    public static Familiemedlem lagBarn() {
        return new Familiemedlem(new Folkeregisteridentifikator("fnrBarn"), new Navn("barn", null, "etternavn"),
            Familierelasjon.BARN, new Foedsel(LocalDate.now().minusYears(42), null, null, null),
            new Folkeregisteridentifikator("fnrAnnenForelder"), "felles", null);
    }

    public static Familiemedlem lagRelatertVedsivilstand() {
        return new Familiemedlem(new Folkeregisteridentifikator("fnr"), new Navn("fornavn", null, "etternavn"),
            Familierelasjon.RELATERT_VED_SIVILSTAND, new Foedsel(LocalDate.MIN, null, null, null), null, "ukjent",
            new Sivilstand(Sivilstandstype.GIFT, null, "relatertVedSivilstandID", LocalDate.MIN, null, "Dolly", "PDL",
                false));
    }
}
