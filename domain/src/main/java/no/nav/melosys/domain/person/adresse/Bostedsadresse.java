package no.nav.melosys.domain.person.adresse;

import no.nav.melosys.domain.adresse.StrukturertAdresse;

import java.time.LocalDate;

public record Bostedsadresse(
    StrukturertAdresse strukturertAdresse,
    String coAdressenavn,
    LocalDate gyldigFraOgMed,
    LocalDate gyldigTilOgMed,
    String master,
    String kilde,
    boolean erHistorisk
) implements PersonAdresse {

    @Override
    public boolean erGyldig() {
        return !erHistorisk && strukturertAdresse != null && strukturertAdresse.erGyldig();
    }
}
