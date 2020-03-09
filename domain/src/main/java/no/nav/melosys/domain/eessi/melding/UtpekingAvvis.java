package no.nav.melosys.domain.eessi.melding;

public class UtpekingAvvis {

    private String begrunnelse;
    private boolean etterspørInformasjon;
    private String nyttLovvalgsland;
    private String fritekst;

    public UtpekingAvvis(String begrunnelse, boolean etterspørInformasjon, String nyttLovvalgsland, String fritekst) {
        this.begrunnelse = begrunnelse;
        this.etterspørInformasjon = etterspørInformasjon;
        this.nyttLovvalgsland = nyttLovvalgsland;
        this.fritekst = fritekst;
    }
}
