package no.nav.melosys.service.persondata.mapping;


import no.nav.melosys.domain.person.adresse.Adressebeskyttelse;
import no.nav.melosys.domain.person.adresse.AdressebeskyttelseGradering;

public final class AdressebeskyttelseOversetter {
    private AdressebeskyttelseOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static Adressebeskyttelse oversett(
        no.nav.melosys.integrasjon.pdl.dto.person.Adressebeskyttelse adressebeskyttelse) {
        return new Adressebeskyttelse(AdressebeskyttelseGradering.valueOf(adressebeskyttelse.gradering().name()),
            adressebeskyttelse.metadata().master());
    }
}
