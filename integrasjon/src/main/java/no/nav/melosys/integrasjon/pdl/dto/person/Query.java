package no.nav.melosys.integrasjon.pdl.dto.person;

public final class Query {
    public static final String HENT_PERSON_QUERY = """
query($ident: ID!) {
    hentPerson(ident: $ident) {
        adressebeskyttelse {
            gradering
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
