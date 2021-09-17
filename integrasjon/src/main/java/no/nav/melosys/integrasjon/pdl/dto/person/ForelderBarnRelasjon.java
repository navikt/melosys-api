package no.nav.melosys.integrasjon.pdl.dto.person;

public record ForelderBarnRelasjon(String relatertPersonsIdent,
                                   Familierelasjonsrolle relatertPersonsRolle,
                                   Familierelasjonsrolle minRolleForPerson) {
    public boolean erBarn() {
        return relatertPersonsRolle == Familierelasjonsrolle.BARN;
    }

    public boolean erForelder() {
        return relatertPersonsRolle == Familierelasjonsrolle.FAR || relatertPersonsRolle == Familierelasjonsrolle.MOR
            || relatertPersonsRolle == Familierelasjonsrolle.MEDMOR;
    }
}
