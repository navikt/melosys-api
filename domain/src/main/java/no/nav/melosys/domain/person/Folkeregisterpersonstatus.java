package no.nav.melosys.domain.person;

import no.nav.melosys.domain.kodeverk.Personstatuser;

public record Folkeregisterpersonstatus(Personstatuser personstatus, String pdlTekst) {
    public String hentGjeldendeTekst() {
        return personstatus == Personstatuser.UDEFINERT ?
            pdlTekst: personstatus.getBeskrivelse();
    }
}
