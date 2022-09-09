package no.nav.melosys.service.lovligeKombinasjoner;

import java.util.Set;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public record BehandlingsKombinasjon(
    Set<Behandlingstema> behandlingsTemaer,
    Set<Behandlingstyper> behandlingsTyper
) {
}
