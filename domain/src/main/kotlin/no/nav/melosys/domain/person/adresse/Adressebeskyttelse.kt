package no.nav.melosys.domain.person.adresse;

import static no.nav.melosys.domain.person.adresse.AdressebeskyttelseGradering.STRENGT_FORTROLIG;
import static no.nav.melosys.domain.person.adresse.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND;

public record Adressebeskyttelse(AdressebeskyttelseGradering gradering, String master) {
    public boolean erStrengtFortrolig() {
        return gradering() == STRENGT_FORTROLIG || gradering() == STRENGT_FORTROLIG_UTLAND;
    }
}
