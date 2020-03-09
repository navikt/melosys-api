package no.nav.melosys.tjenester.gui.dto.utpeking;

import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;

public class UtpekingAvvisDto {

    private String fritekst;
    private String nyttLovvalgsland;
    private String begrunnelseUtenlandskMyndighet;
    private boolean vilSendeAnmodningOmMerInformasjon;

    public UtpekingAvvis tilDomene() {
        return new UtpekingAvvis(
            begrunnelseUtenlandskMyndighet,
            vilSendeAnmodningOmMerInformasjon,
            nyttLovvalgsland,
            fritekst
        );
    }

    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(String fritekst) {
        this.fritekst = fritekst;
    }

    public String getNyttLovvalgsland() {
        return nyttLovvalgsland;
    }

    public void setNyttLovvalgsland(String nyttLovvalgsland) {
        this.nyttLovvalgsland = nyttLovvalgsland;
    }

    public String getBegrunnelseUtenlandskMyndighet() {
        return begrunnelseUtenlandskMyndighet;
    }

    public void setBegrunnelseUtenlandskMyndighet(String begrunnelseUtenlandskMyndighet) {
        this.begrunnelseUtenlandskMyndighet = begrunnelseUtenlandskMyndighet;
    }

    public boolean isVilSendeAnmodningOmMerInformasjon() {
        return vilSendeAnmodningOmMerInformasjon;
    }

    public void setVilSendeAnmodningOmMerInformasjon(boolean vilSendeAnmodningOmMerInformasjon) {
        this.vilSendeAnmodningOmMerInformasjon = vilSendeAnmodningOmMerInformasjon;
    }
}
