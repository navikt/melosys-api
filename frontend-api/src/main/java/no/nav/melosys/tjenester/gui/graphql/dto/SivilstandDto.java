package no.nav.melosys.tjenester.gui.graphql.dto;

import java.time.LocalDate;

public record SivilstandDto(String type,
                            String relatertVedSivilstand,
                            LocalDate gyldigFraOgMed,
                            LocalDate bekreftelsesdato,
                            String master,
                            String kilde,
                            boolean erHistorisk) {
}
