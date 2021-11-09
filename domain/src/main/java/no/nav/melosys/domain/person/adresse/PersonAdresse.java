package no.nav.melosys.domain.person.adresse;

import no.nav.melosys.domain.adresse.StrukturertAdresse;

import java.time.LocalDateTime;

public interface PersonAdresse {
    String coAdressenavn();
    StrukturertAdresse strukturertAdresse();
    LocalDateTime gyldigFraOgMed();
    LocalDateTime gyldigTilOgMed();
    String master();
    String kilde();
    boolean erHistorisk();

}
