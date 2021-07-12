package no.nav.melosys.service.persondata;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.melosys.integrasjon.pdl.dto.Endring;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;
import no.nav.melosys.integrasjon.pdl.dto.person.Adressebeskyttelse;
import no.nav.melosys.integrasjon.pdl.dto.person.AdressebeskyttelseGradering;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Bostedsadresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.UtenlandskAdresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Vegadresse;

public class PdlObjectFactory {
    private static final Metadata METADATA = lagMetadata();

    public static Metadata metadata() {
        return METADATA;
    }

    public static Adressebeskyttelse lagAdressebeskyttelse(AdressebeskyttelseGradering adressebeskyttelseGradering) {
        return new Adressebeskyttelse(adressebeskyttelseGradering, metadata());
    }

    public static Bostedsadresse lagNorskBostedsadresse() {
        return new Bostedsadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            "Kari Hansen",
            new Vegadresse(
                "Kirkegata",
                "12",
                "B",
                "Storgården",
                "1234"
            ),
            null,
            null,
            null,
            metadata()
        );
    }

    public static Bostedsadresse lagNorskBostedsadresse(String gatenavn, LocalDateTime registrertDato) {
        return new Bostedsadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            "Kari Hansen",
            new Vegadresse(
                gatenavn,
                "12",
                "B",
                "Storgården",
                "1234"
            ),
            null,
            null,
            null,
            lagMetadata(registrertDato)
        );
    }

    public static Bostedsadresse lagUtenlandskBostedsadresse() {
        return new Bostedsadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            "Kari Hansen",
            null,
            null,
            new UtenlandskAdresse(
                "adressenavnNummer",
                "bygningEtasjeLeilighet",
                "P.O.Box 1234 Place",
                "SE-12345",
                "Haworth",
                "Yorkshire",
                "SWE"
            ),
            null,
            metadata());
    }

    public static Bostedsadresse lagUtenlandskBostedsadresse(String gatenavn, LocalDateTime registrertDato) {
        return new Bostedsadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            "Kari Hansen",
            null,
            null,
            new UtenlandskAdresse(
                gatenavn,
                "bygningEtasjeLeilighet",
                "P.O.Box 1234 Place",
                "SE-12345",
                "Haworth",
                "Yorkshire",
                "SWE"
            ),
            null,
            lagMetadata(registrertDato));
    }

    public static Metadata lagMetadata(LocalDateTime registrertDato) {
        return new Metadata("PDL", false,
            List.of(new Endring("OPPRETT", registrertDato, "Dolly")));
    }

    static Metadata lagMetadata() {
        return new Metadata("PDL", false,
            List.of(new Endring("OPPRETT", LocalDateTime.parse("2021-05-07T10:04:52"), "Dolly")));
    }
}
