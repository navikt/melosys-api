package no.nav.melosys.domain.person.adresse;

import java.time.LocalDate;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import org.apache.commons.lang3.StringUtils;

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
        return !erHistorisk && strukturertAdresse != null && !strukturertAdresse.erTom() && !StringUtils.isBlank(strukturertAdresse.getPostnummer());
    }
}
