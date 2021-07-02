package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

public class IkkeOmfattetEktefelle {
    private FamilieInfo info;
    private String begrunnelse;

    public IkkeOmfattetEktefelle(FamilieInfo info, String begrunnelse) {
        this.info = info;
        this.begrunnelse = begrunnelse;
    }

    public FamilieInfo getInfo() {
        return info;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
