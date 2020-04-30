package no.nav.melosys.service.vilkaar;

import no.nav.melosys.domain.kodeverk.begrunnelser.Inngangsvilkaar;

public final class InngangsvilkaarVurdering {
    private boolean oppfylt;
    private Inngangsvilkaar begrunnelseKode;

    public InngangsvilkaarVurdering(boolean oppfylt, Inngangsvilkaar begrunnelseKode) {
        this.oppfylt = oppfylt;
        this.begrunnelseKode = begrunnelseKode;
    }

    public boolean isOppfylt() {
        return oppfylt;
    }

    public Inngangsvilkaar getBegrunnelseKode() {
        return begrunnelseKode;
    }
}
