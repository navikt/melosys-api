package no.nav.melosys.integrasjon.pdl.dto.person;

public final class Query {
    public static final String HENT_PERSON_QUERY = """
query($ident: ID!) {
    hentPerson(ident: $ident) {
        adressebeskyttelse {
            gradering
        }
        bostedsadresse {
            angittFlyttedato
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
            forenkletStatus
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
            type
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
    }
}
        """;

    private Query() {
        throw new UnsupportedOperationException();
    }
}
