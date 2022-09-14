package no.nav.melosys.service.lovligekombinasjoner;

import java.util.Set;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public record BehandlingstemaBehandlingstyperKombinasjon(
    Set<Behandlingstema> behandlingsTemaer,
    Set<Behandlingstyper> behandlingsTyper
) {
}
