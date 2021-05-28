package no.nav.melosys.integrasjon.pdl.dto.person;

public final class Query {
    public static final String HENT_PERSON_QUERY = """
query($ident: ID!) {
  hentPerson(ident: $ident) {
    adressebeskyttelse {
      gradering
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
      type
      status
    }
    folkeregisterpersonstatus {
      status
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
    statsborgerskap {
      land
      metadata {
        master
      }
    }
    sivilstand {
      type
      relatertVedSivilstand
      gyldigFraOgMed
    }
    utenlandskIdentifikasjonsnummer {
      identifikasjonsnummer
      utstederland
      opphoert
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

    private Query() {
        throw new UnsupportedOperationException();
    }
}
