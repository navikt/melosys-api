package no.nav.melosys.domain;

import java.util.Collection;

public class LovvalgsperiodeLagreEvent extends BehandlingEvent {
    private final long behandlingId;
    private final Collection<Lovvalgsperiode> lovvalgsperioder;

    public LovvalgsperiodeLagreEvent(long behandlingId, Collection<Lovvalgsperiode> lovvalgsperioder) {
        super(behandlingId);
        this.behandlingId = behandlingId;
        this.lovvalgsperioder = lovvalgsperioder;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Collection<Lovvalgsperiode> getLovvalgsperioder() {
        return lovvalgsperioder;
    }

    @Override
    public String toString() {
        return "LovvalgsperiodeLagreEvent{" +
            "behandlingId=" + behandlingId +
            ", lovvalgsperioder=" + lovvalgsperioder +
            '}';
    }
}
