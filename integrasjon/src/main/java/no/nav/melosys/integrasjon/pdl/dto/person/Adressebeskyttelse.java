package no.nav.melosys.integrasjon.pdl.dto.person;

import static no.nav.melosys.integrasjon.pdl.dto.person.AdressebeskyttelseGradering.*;

public record Adressebeskyttelse(AdressebeskyttelseGradering gradering) {
    public boolean erStrengtFortrolig() {
        return gradering() == STRENGT_FORTROLIG || gradering() == STRENGT_FORTROLIG_UTLAND;
    }
}
