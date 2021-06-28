package no.nav.melosys.service.persondata;

import no.nav.melosys.domain.person.Navn;

public class NavnOversetter {
    public static final String UKJENT = "UKJENT";

    private NavnOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static String tilSammensattNavn(no.nav.melosys.integrasjon.pdl.dto.person.Navn navn) {
        return new Navn(navn.fornavn(), navn.mellomnavn(), navn.etternavn()).tilSammensattNavn();
    }
}
