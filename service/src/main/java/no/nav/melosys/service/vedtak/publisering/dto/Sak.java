package no.nav.melosys.service.vedtak.publisering.dto;

import java.time.LocalDate;

public record Sak(String fnr, long behandlingId, String saksnummer, String sakstype, LocalDate registreringsdato) {
}
