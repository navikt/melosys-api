package no.nav.melosys.repository;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

public record BehandlingStatistikk(Behandlingstema behandlingstema, long antall) {
}
