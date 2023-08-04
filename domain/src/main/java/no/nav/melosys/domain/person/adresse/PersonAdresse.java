package no.nav.melosys.domain.person.adresse;

import java.time.LocalDate;

import no.nav.melosys.domain.adresse.StrukturertAdresse;

public interface PersonAdresse {
    String coAdressenavn();
    StrukturertAdresse strukturertAdresse();
    LocalDate gyldigFraOgMed();
    LocalDate gyldigTilOgMed();
    String master();
    String kilde();
    boolean erHistorisk();

    boolean harRegistrertAdresse();
}
