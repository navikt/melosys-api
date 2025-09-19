package no.nav.melosys.domain.person;

import java.time.LocalDate;

public record Statsborgerskap(String landkode,
                              LocalDate bekreftelsesdato,
                              LocalDate gyldigFraOgMed,
                              LocalDate gyldigTilOgMed,
                              String master,
                              String kilde,
                              boolean erHistorisk) {

    public boolean erBekreftetPåDato(LocalDate dato) {
        return bekreftelsesdato != null && !bekreftelsesdato.isAfter(dato);
    }

    public boolean erGyldigPåDato(LocalDate dato) {
        return erGyldigFraOgMedDato(dato) && erGyldigTilOgMedDato(dato);
    }

    private boolean erGyldigFraOgMedDato(LocalDate dato) {
        return (gyldigFraOgMed == null && !erHistorisk) || (gyldigFraOgMed != null && !gyldigFraOgMed.isAfter(dato));
    }

    private boolean erGyldigTilOgMedDato(LocalDate dato) {
        return gyldigTilOgMed == null || gyldigTilOgMed.isAfter(dato);
    }
}
