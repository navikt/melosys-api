package no.nav.melosys.domain.person.adresse;

import java.time.LocalDate;
import java.time.LocalDateTime;

import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import org.apache.commons.lang3.StringUtils;

public record Kontaktadresse(
    StrukturertAdresse strukturertAdresse,
    SemistrukturertAdresse semistrukturertAdresse,
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
        StrukturertAdresse adresse = hentEllerLagStrukturertAdresse();
        return adresse != null && !adresse.erTom() && !StringUtils.isBlank(adresse.getPostnummer());
    }

    public StrukturertAdresse hentEllerLagStrukturertAdresse() {
        if (strukturertAdresse != null) {
            return strukturertAdresse;
        } else if (semistrukturertAdresse != null) {
            return semistrukturertAdresse.tilStrukturertAdresse();
        }
        return null;
    }
}
