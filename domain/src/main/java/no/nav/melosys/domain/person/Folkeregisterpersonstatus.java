package no.nav.melosys.domain.person;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.Personstatuser;

public record Folkeregisterpersonstatus(
    Personstatuser personstatus,
    String tekstHvisStatusErUdefinert,
    LocalDate gyldigFraOgMed,
    LocalDate gyldigTilOgMed,
    String master,
    String kilde,
    boolean erHistorisk
) {
    public String hentGjeldendeTekst() {
        return personstatus == Personstatuser.UDEFINERT ?
            tekstHvisStatusErUdefinert : personstatus.getBeskrivelse();
    }
}
