package no.nav.melosys.domain.eessi.sed;

public class UtpekingAvvisDto {

    private String nyttLovvalgsland;
    private String begrunnelseUtenlandskMyndighet;
    private boolean vilSendeAnmodningOmMerInformasjon;

    public UtpekingAvvisDto(String nyttLovvalgsland, String begrunnelseUtenlandskMyndighet, boolean vilSendeAnmodningOmMerInformasjon) {
        this.nyttLovvalgsland = nyttLovvalgsland;
        this.begrunnelseUtenlandskMyndighet = begrunnelseUtenlandskMyndighet;
        this.vilSendeAnmodningOmMerInformasjon = vilSendeAnmodningOmMerInformasjon;
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
