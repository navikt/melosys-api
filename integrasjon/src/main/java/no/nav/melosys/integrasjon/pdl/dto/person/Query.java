package no.nav.melosys.integrasjon.pdl.dto.person;

public final class Query {
    private Query() {
        throw new UnsupportedOperationException();
    }

    public static final String HENT_PERSON_QUERY = """
query($ident: ID!) {
  hentPerson(ident: $ident) {
    adressebeskyttelse {
      gradering
      metadata {
        master
      }
    }
    bostedsadresse {
      gyldigFraOgMed
      gyldigTilOgMed
      coAdressenavn
      matrikkeladresse {
        bruksenhetsnummer
        kommunenummer
        tilleggsnavn
        postnummer
      }
      ukjentBosted {
        bostedskommune
      }
      vegadresse {
        adressenavn
        husnummer
        husbokstav
        tilleggsnavn
        postnummer
      }
      utenlandskAdresse {
        adressenavnNummer
        bygningEtasjeLeilighet
        postboksNummerNavn
        postkode
        bySted
        regionDistriktOmraade
        landkode
      }
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
    doedsfall {
      doedsdato
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    foedsel {
      foedselsdato
      foedselsaar
      foedeland
      foedested
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    folkeregisteridentifikator {
      identifikasjonsnummer
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    forelderBarnRelasjon {
      relatertPersonsIdent
      relatertPersonsRolle
      minRolleForPerson
    }
    kjoenn {
      kjoenn
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    kontaktadresse {
      gyldigFraOgMed
      gyldigTilOgMed
      coAdressenavn
      postboksadresse {
        postboks
        postbokseier
        postnummer
      }
      postadresseIFrittFormat {
        adresselinje1
        adresselinje2
        adresselinje3
        postnummer
      }
      utenlandskAdresse {
        adressenavnNummer
        bygningEtasjeLeilighet
        postboksNummerNavn
        postkode
        bySted
        regionDistriktOmraade
        landkode
      }
      utenlandskAdresseIFrittFormat {
        adresselinje1
        adresselinje2
        adresselinje3
        byEllerStedsnavn
        landkode
        postkode
      }
      vegadresse {
        adressenavn
        husnummer
        husbokstav
        tilleggsnavn
        postnummer
      }
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
    navn {
      fornavn
      mellomnavn
      etternavn
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    oppholdsadresse {
      gyldigFraOgMed
      gyldigTilOgMed
      coAdressenavn
      matrikkeladresse {
        bruksenhetsnummer
        kommunenummer
        tilleggsnavn
        postnummer
      }
      utenlandskAdresse {
        adressenavnNummer
        bygningEtasjeLeilighet
        postboksNummerNavn
        postkode
        bySted
        regionDistriktOmraade
        landkode
      }
      vegadresse {
        adressenavn
        husnummer
        husbokstav
        tilleggsnavn
        postnummer
      }
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
    sivilstand {
      type
      relatertVedSivilstand
      gyldigFraOgMed
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
    statsborgerskap {
      land
      bekreftelsesdato
      gyldigFraOgMed
      gyldigTilOgMed
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
  }
}
        """;

    public static final String HENT_PERSON_HISTORIKK_QUERY = """
query($ident: ID!, $historikk: Boolean!) {
  hentPerson(ident: $ident) {
    bostedsadresse(historikk: $historikk) {
      gyldigFraOgMed
      gyldigTilOgMed
      coAdressenavn
      matrikkeladresse {
        bruksenhetsnummer
        kommunenummer
        tilleggsnavn
        postnummer
      }
      ukjentBosted {
        bostedskommune
      }
      vegadresse {
        adressenavn
        husnummer
        husbokstav
        tilleggsnavn
        postnummer
      }
      utenlandskAdresse {
        adressenavnNummer
        bygningEtasjeLeilighet
        postboksNummerNavn
        postkode
        bySted
        regionDistriktOmraade
        landkode
      }
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
    doedsfall {
      doedsdato
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    foedsel {
      foedselsdato
      foedselsaar
      foedeland
      foedested
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    folkeregisteridentifikator {
      identifikasjonsnummer
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    folkeregisterpersonstatus {
      status
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    forelderBarnRelasjon {
      relatertPersonsIdent
      relatertPersonsRolle
      minRolleForPerson
    }
    kjoenn {
      kjoenn
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    kontaktadresse(historikk: $historikk) {
      gyldigFraOgMed
      gyldigTilOgMed
      coAdressenavn
      postboksadresse {
        postboks
        postbokseier
        postnummer
      }
      postadresseIFrittFormat {
        adresselinje1
        adresselinje2
        adresselinje3
        postnummer
      }
      utenlandskAdresse {
        adressenavnNummer
        bygningEtasjeLeilighet
        postboksNummerNavn
        postkode
        bySted
        regionDistriktOmraade
        landkode
      }
      utenlandskAdresseIFrittFormat {
        adresselinje1
        adresselinje2
        adresselinje3
        byEllerStedsnavn
        landkode
        postkode
      }
      vegadresse {
        adressenavn
        husnummer
        husbokstav
        tilleggsnavn
        postnummer
      }
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
    navn {
      fornavn
      mellomnavn
      etternavn
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    oppholdsadresse(historikk: $historikk) {
      gyldigFraOgMed
      gyldigTilOgMed
      coAdressenavn
      matrikkeladresse {
        bruksenhetsnummer
        kommunenummer
        tilleggsnavn
        postnummer
      }
      utenlandskAdresse {
        adressenavnNummer
        bygningEtasjeLeilighet
        postboksNummerNavn
        postkode
        bySted
        regionDistriktOmraade
        landkode
      }
      vegadresse {
        adressenavn
        husnummer
        husbokstav
        tilleggsnavn
        postnummer
      }
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
    statsborgerskap(historikk: $historikk) {
      land
      bekreftelsesdato
      gyldigFraOgMed
      gyldigTilOgMed
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
    sivilstand(historikk: $historikk) {
      type
      relatertVedSivilstand
      gyldigFraOgMed
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
  }
}
 """;

    public static final String HENT_ADRESSEBESKYTTELSE_QUERY = """
query($ident: ID!, $historikk: Boolean!) {
  hentPerson(ident: $ident) {
    adressebeskyttelse(historikk: $historikk) {
      gradering
    }
  }
}
        """;

    public static final String HENT_FAMILIERELASJONER_QUERY = """
query($ident: ID!, $historikk: Boolean!) {
  hentPerson(ident: $ident) {
    foedsel {
      foedselsdato
      foedselsaar
      foedeland
      foedested
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    folkeregisteridentifikator {
      identifikasjonsnummer
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    forelderBarnRelasjon {
      relatertPersonsIdent
      relatertPersonsRolle
      minRolleForPerson
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
    sivilstand(historikk: $historikk) {
      type
      relatertVedSivilstand
      gyldigFraOgMed
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
  }
}
 """;

    public static final String HENT_RELATERT_VED_SIVILSTAND_QUERY = """
query($ident: ID!) {
  hentPerson(ident: $ident) {
    bostedsadresse {
      gyldigFraOgMed
      gyldigTilOgMed
      coAdressenavn
      matrikkeladresse {
        bruksenhetsnummer
        kommunenummer
        tilleggsnavn
        postnummer
      }
      ukjentBosted {
        bostedskommune
      }
      vegadresse {
        adressenavn
        husnummer
        husbokstav
        tilleggsnavn
        postnummer
      }
      utenlandskAdresse {
        adressenavnNummer
        bygningEtasjeLeilighet
        postboksNummerNavn
        postkode
        bySted
        regionDistriktOmraade
        landkode
      }
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
    foedsel {
      foedselsdato
      foedselsaar
      foedeland
      foedested
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    folkeregisteridentifikator {
      identifikasjonsnummer
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    navn {
      fornavn
      mellomnavn
      etternavn
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    sivilstand {
      type
      relatertVedSivilstand
      gyldigFraOgMed
      metadata {
        master
        historisk
        endringer {
          type
          registrert
          kilde
        }
      }
    }
  }
}
        """;

    public static final String HENT_BARN_QUERY = """
query($ident: ID!) {
  hentPerson(ident: $ident) {
    foedsel {
      foedselsdato
      foedselsaar
      foedeland
      foedested
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    folkeregisteridentifikator {
      identifikasjonsnummer
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    forelderBarnRelasjon {
      relatertPersonsIdent
      relatertPersonsRolle
      minRolleForPerson
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    foreldreansvar {
      ansvar
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    navn {
      fornavn
      mellomnavn
      etternavn
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
  }
}
        """;

    public static final String HENT_FORELDER_QUERY = """
query($ident: ID!) {
  hentPerson(ident: $ident) {
    foedsel {
      foedselsdato
      foedselsaar
      foedeland
      foedested
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    folkeregisteridentifikator {
      identifikasjonsnummer
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
    navn {
      fornavn
      mellomnavn
      etternavn
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
  }
}
        """;

    public static final String HENT_SAMMENSATT_NAVN_QUERY = """
query($ident: ID!, $historikk: Boolean!) {
  hentPerson(ident: $ident) {
    navn(historikk: $historikk) {
      fornavn
      mellomnavn
      etternavn
      metadata {
        master
        endringer {
          registrert
          type
        }
      }
    }
  }
}
        """;

    public static final String HENT_STATSBORGERSKAP_QUERY = """
        query($ident: ID!, $historikk: Boolean!) {
            hentPerson(ident: $ident) {
                statsborgerskap(historikk: $historikk) {
                    land
                    bekreftelsesdato
                    gyldigFraOgMed
                    gyldigTilOgMed
                    metadata {
                        master
                        historisk
                        endringer {
                            type
                            registrert
                            kilde
                        }
                    }
                }
            }
        }
        """;
}
