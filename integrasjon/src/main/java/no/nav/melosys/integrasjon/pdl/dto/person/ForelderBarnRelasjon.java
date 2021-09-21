package no.nav.melosys.integrasjon.pdl.dto.person;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record ForelderBarnRelasjon(String relatertPersonsIdent,
                                   Familierelasjonsrolle relatertPersonsRolle,
                                   Familierelasjonsrolle minRolleForPerson,
                                   Metadata metadata) implements HarMetadata {
    public boolean erBarn() {
        return relatertPersonsRolle == Familierelasjonsrolle.BARN;
    }

    public boolean erForelder() {
        return relatertPersonsRolle == Familierelasjonsrolle.FAR || relatertPersonsRolle == Familierelasjonsrolle.MOR
            || relatertPersonsRolle == Familierelasjonsrolle.MEDMOR;
    }
}
