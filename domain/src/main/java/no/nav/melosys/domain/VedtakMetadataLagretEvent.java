package no.nav.melosys.domain;

import no.nav.melosys.domain.behandling.BehandlingEvent;

public class VedtakMetadataLagretEvent extends BehandlingEvent {
    public VedtakMetadataLagretEvent(Long behandlingID) {
        super(behandlingID);
    }
}
