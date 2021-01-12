package no.nav.melosys.domain.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public abstract class Brevbestilling {
    protected final Produserbaredokumenter produserbartdokument;
    protected final Behandling behandling;
    protected final String avsenderNavn;

    protected Brevbestilling(Produserbaredokumenter produserbartdokument, Behandling behandling, String avsenderNavn) {
        this.produserbartdokument = produserbartdokument;
        this.behandling = behandling;
        this.avsenderNavn = avsenderNavn;
    }

    public Produserbaredokumenter getProduserbartdokument() {
        return produserbartdokument;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public String getAvsenderNavn() {
        return avsenderNavn;
    }
}
