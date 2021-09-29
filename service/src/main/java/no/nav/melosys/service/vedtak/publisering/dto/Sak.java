package no.nav.melosys.service.vedtak.publisering.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.Sakstyper;

public record Sak(String fnr, long behandlingId, String saksnummer, String sakstype, LocalDate registreringsdato) {
}
