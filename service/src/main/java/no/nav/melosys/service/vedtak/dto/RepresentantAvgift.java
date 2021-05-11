package no.nav.melosys.service.vedtak.dto;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public record RepresentantAvgift(String ident, Aktoersroller type, String representantNrAvgiftssystem) {
}
