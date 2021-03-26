package no.nav.melosys.domain.dokument;

import no.nav.melosys.domain.behandling.BehandlingEvent;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public class DokumentBestiltEvent extends BehandlingEvent {
    private final long behandlingID;
    private final Produserbaredokumenter produserbaredokumenter;

    public DokumentBestiltEvent(long behandlingID, Produserbaredokumenter produserbaredokumenter) {
        super(behandlingID);
        this.behandlingID = behandlingID;
        this.produserbaredokumenter = produserbaredokumenter;
    }

    public long getBehandlingID() {
        return behandlingID;
    }

    public Produserbaredokumenter getProduserbaredokumenter() {
        return produserbaredokumenter;
    }
}
