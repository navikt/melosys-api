package no.nav.melosys.service.persondata;

import no.nav.melosys.integrasjon.pdl.dto.person.Navn;

public class NavnOversetter {
    public static final String UKJENT = "UKJENT";

    private NavnOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static String tilSammensattNavn(Navn navn) {
        return navn.etternavn() + getMellomnavn(navn) + " " + navn.fornavn();
    }

    private static String getMellomnavn(Navn navn) {
        return navn.mellomnavn() == null ? "" : " " + navn.mellomnavn();
    }
}
