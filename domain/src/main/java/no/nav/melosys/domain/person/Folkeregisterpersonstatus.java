package no.nav.melosys.domain.person;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.Personstatuser;

public record Folkeregisterpersonstatus(
    Personstatuser personstatus,
    String tekstHvisStatusErUdefinert,
    String master,
    String kilde,
    LocalDate registreringsdato,
    boolean erHistorisk
) {
    public String hentGjeldendeTekst() {
        return personstatus == Personstatuser.UDEFINERT ?
            tekstHvisStatusErUdefinert : personstatus.getBeskrivelse();
    }
}
