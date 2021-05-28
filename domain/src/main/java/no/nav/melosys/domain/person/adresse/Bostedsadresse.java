package no.nav.melosys.domain.person.adresse;

import java.time.LocalDateTime;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.adresse.UstrukturertAdresse;

public record Bostedsadresse(
    StrukturertAdresse strukturertAdresse,
    UstrukturertAdresse ustrukturertAdresse,
    String coAdressenavn,
    LocalDateTime gyldigFraOgMed,
    LocalDateTime gyldigTilOgMed,
    String master,
    String kilde,
    boolean erHistorisk
) {
}
