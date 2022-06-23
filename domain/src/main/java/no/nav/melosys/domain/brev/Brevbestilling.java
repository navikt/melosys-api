package no.nav.melosys.domain.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Distribusjonstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public abstract class Brevbestilling {
    protected Produserbaredokumenter produserbartdokument;
    protected Behandling behandling;
    protected String avsenderID;
    protected Distribusjonstyper distribusjonstype;

    protected Brevbestilling() {
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    protected Brevbestilling(Produserbaredokumenter produserbartdokument, Behandling behandling, String avsenderID, Distribusjonstyper distribusjonstype) {
        this.produserbartdokument = produserbartdokument;
        this.behandling = behandling;
        this.avsenderID = avsenderID;
        this.distribusjonstype = distribusjonstype;
    }

    public Produserbaredokumenter getProduserbartdokument() {
        return produserbartdokument;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public String getAvsenderID() {
        return avsenderID;
    }

    public Distribusjonstyper getDistribusjonstype() {
        return distribusjonstype;
    }
}
