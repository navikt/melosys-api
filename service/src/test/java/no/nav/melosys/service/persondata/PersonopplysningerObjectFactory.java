package no.nav.melosys.service.persondata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.person.*;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class PersonopplysningerObjectFactory {
    public static Personopplysninger lagPersonopplysninger() {
        return lagPersonopplysninger(false);
    }

    public static Personopplysninger lagPersonopplysningerStatløs() {
        return lagPersonopplysninger(true);
    }

    private static Personopplysninger lagPersonopplysninger(boolean erStatløs) {
        return new Personopplysninger(emptyList(), lagBostedsadresse(), null, emptySet(), lagFødesel(), null,
            lagKjønn(), lagKontaktadresser(), lagNavn(), lagOppholdsadresser(), lagStatsborgerskap(erStatløs));

    }

    private static Foedsel lagFødesel() {
        return new Foedsel(LocalDate.EPOCH, null, null, null);
    }

    private static KjoennType lagKjønn() {
        return KjoennType.UKJENT;
    }

    private static Bostedsadresse lagBostedsadresse() {
        return new Bostedsadresse(new StrukturertAdresse("gatenavnFraBostedsadresse", null, null, null, null, "NO"),
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
        return new StrukturertAdresse(gatenavn, null, "0123", null, null, "NO");
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
}
