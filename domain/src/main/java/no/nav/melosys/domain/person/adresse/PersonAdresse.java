package no.nav.melosys.domain.person.adresse;

import no.nav.melosys.domain.adresse.StrukturertAdresse;

import java.time.LocalDate;

public interface PersonAdresse {
    String coAdressenavn();
    StrukturertAdresse strukturertAdresse();
    LocalDate gyldigFraOgMed();
    LocalDate gyldigTilOgMed();
    String master();
    String kilde();
    boolean erHistorisk();

}
