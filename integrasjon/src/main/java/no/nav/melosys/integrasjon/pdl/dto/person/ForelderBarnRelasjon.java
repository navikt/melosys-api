package no.nav.melosys.integrasjon.pdl.dto.person;

public record ForelderBarnRelasjon(String relatertPersonsIdent,
                                   Familierelasjonsrolle relatertPersonsRolle,
                                   Familierelasjonsrolle minRolleForPerson) {
}
