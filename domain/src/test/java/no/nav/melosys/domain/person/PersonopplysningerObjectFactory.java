package no.nav.melosys.domain.person;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Personstatuser;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class PersonopplysningerObjectFactory {

    public static Personopplysninger lagPersonopplysninger() {
        return new Personopplysninger(emptyList(), lagBostedsadresse(), null, emptySet(),
            lagFødselsdato(), null, lagKjønn(), lagKontaktadresser(), lagNavn(), lagOppholdsadresser(),
            lagStatsborgerskap());
    }

    private static Foedselsdato lagFødselsdato() {
        return new Foedselsdato(LocalDate.EPOCH, null);
    }

    private static KjoennType lagKjønn() {
        return KjoennType.UKJENT;
    }

    private static Bostedsadresse lagBostedsadresse() {
        return new Bostedsadresse(new StrukturertAdresse("gatenavnFraBostedsadresse", "3", "1234", "Oslo", "Norge", "NO"),
            null, null, null, null, null, false);
    }

    public static Collection<Kontaktadresse> lagKontaktadresser() {
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

    private static Navn lagNavn() {
        return new Navn("Ola", null, "Nordmann");
    }

    private static Collection<Statsborgerskap> lagStatsborgerskap() {
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


}
