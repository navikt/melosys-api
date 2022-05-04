package no.nav.melosys.service.persondata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.integrasjon.pdl.dto.Endring;
import no.nav.melosys.integrasjon.pdl.dto.Folkeregistermetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;
import no.nav.melosys.integrasjon.pdl.dto.person.*;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Bostedsadresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Matrikkeladresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.UtenlandskAdresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Vegadresse;

import static no.nav.melosys.integrasjon.pdl.dto.Endringstype.OPPRETT;

public class PdlObjectFactory {
    private static final Metadata METADATA = lagMetadata();
    private static final Folkeregistermetadata FOLKEREGISTERMETADATA = lagFolkeregistermetadata();

    public static Metadata metadata() {
        return METADATA;
    }

    public static Folkeregistermetadata folkeregistermetadata() {
        return FOLKEREGISTERMETADATA;
    }

    public static Person lagPerson() {
        return new Person(
            Set.of(new Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, metadata())),
            Set.of(lagUtenlandskBostedsadresse("adresse utland", LocalDateTime.MIN), lagNorskBostedsadresse("gata",
                LocalDateTime.MAX)),
            Set.of(new no.nav.melosys.integrasjon.pdl.dto.person.Doedsfall(LocalDate.MAX, metadata())),
            Set.of(new no.nav.melosys.integrasjon.pdl.dto.person.Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested",
                metadata())),
            Set.of(new no.nav.melosys.integrasjon.pdl.dto.person.Folkeregisteridentifikator("IdNr", metadata())),
            Set.of(new Folkeregisterpersonstatus("ikkeBosatt", metadata(), folkeregistermetadata())),
            Set.of(new ForelderBarnRelasjon("barnIdent", Familierelasjonsrolle.BARN, Familierelasjonsrolle.MOR, metadata()),
                new ForelderBarnRelasjon("forelderIdent", Familierelasjonsrolle.MOR, Familierelasjonsrolle.BARN, metadata())),
            Set.of(new Foreldreansvar("felles", metadata())),
            Set.of(new Kjoenn(KjoennType.UKJENT, lagMetadata(LocalDateTime.MIN)), new Kjoenn(KjoennType.UKJENT, lagMetadata(LocalDateTime.MAX))),
            Collections.emptyList(),
            Set.of(new no.nav.melosys.integrasjon.pdl.dto.person.Navn("fornavn", "mellomnavn", "etternavn", metadata())),
            Collections.emptyList(),
            Set.of(lagSivilstand()),
            Set.of(new no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap("AIA", null,
                    LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"), lagMetadata(LocalDateTime.MIN)),
                new no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null,
                    null, lagMetadata(LocalDateTime.MAX))),
            null);
    }

    public static Sivilstand lagSivilstand() {
        return lagSivilstand("relatertVedSivilstandID");
    }

    public static Sivilstand lagSivilstand(String relatertVedSivilstandId) {
        return new Sivilstand(Sivilstandstype.GIFT, relatertVedSivilstandId, LocalDate.MIN, LocalDate.EPOCH,
            lagMetadata());
    }

    public static Bostedsadresse lagBostedsadresseMedMatrikkelAdresse() {
        return new Bostedsadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            null,
            null,
            new Matrikkeladresse("tilleggsnavn", "4321"),
            null,
            null,
            metadata()
        );
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
            List.of(new Endring(OPPRETT, registrertDato, "Dolly")));
    }

    static Metadata lagMetadata() {
        return new Metadata("PDL", false,
            List.of(new Endring(OPPRETT, LocalDateTime.parse("2021-05-07T10:04:52"), "Dolly")));
    }

    static Folkeregistermetadata lagFolkeregistermetadata() {
        return new Folkeregistermetadata(
            LocalDateTime.parse("2021-04-07T10:04:52"),
            LocalDateTime.parse("2021-05-07T10:04:52"),
            LocalDateTime.parse("2021-06-07T10:04:52"),
            null,
            null
        );
    }
}
