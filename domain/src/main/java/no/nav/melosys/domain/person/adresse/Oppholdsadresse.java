package no.nav.melosys.domain.person.adresse;

import java.time.LocalDate;
import java.time.LocalDateTime;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import org.apache.commons.lang3.StringUtils;

public record Oppholdsadresse(
    StrukturertAdresse strukturertAdresse,
    String coAdressenavn,
    LocalDate gyldigFraOgMed,
    LocalDate gyldigTilOgMed,
    String master,
    String kilde,
    LocalDateTime registrertDato,
    boolean erHistorisk
) implements PersonAdresse {

    @Override
    public boolean harRegistrertAdresse() {
        return strukturertAdresse != null && !strukturertAdresse.erTom() && !StringUtils.isBlank(strukturertAdresse.getPostnummer());
    }
}
