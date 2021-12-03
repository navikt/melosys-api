package no.nav.melosys.domain.person;

import no.nav.melosys.domain.kodeverk.Personstatuser;

public record Folkeregisterpersonstatus(Personstatuser personstatus,
                                        String tekstHvisStatusErUdefinert) {
    public String hentGjeldendeTekst() {
        return personstatus == Personstatuser.UDEFINERT ?
            tekstHvisStatusErUdefinert : personstatus.getBeskrivelse();
    }
}
