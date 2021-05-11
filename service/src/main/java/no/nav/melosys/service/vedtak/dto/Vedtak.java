package no.nav.melosys.service.vedtak.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;

public record Vedtak(LocalDate vedtaksDato, LocalDate klagefristDato, Vedtakstyper vedtakstype, String saksbehandler,
                     String kontrollertAvSaksbehandler) {
}
