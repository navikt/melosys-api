package no.nav.melosys.domain;

public class VedtakMetadataLagretEvent extends BehandlingEvent {
    public VedtakMetadataLagretEvent(Long behandlingID) {
        super(behandlingID);
    }
}
