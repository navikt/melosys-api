package no.nav.melosys.service.vedtak.publisering.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;

public record Vedtak(LocalDate vedtaksDato, LocalDate klagefristDato, String vedtakstype, String saksbehandler,
                     String kontrollertAvSaksbehandler) {
}
