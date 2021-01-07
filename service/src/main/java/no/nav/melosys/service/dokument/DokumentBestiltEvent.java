package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import org.springframework.context.ApplicationEvent;

public class DokumentBestiltEvent extends ApplicationEvent {
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
