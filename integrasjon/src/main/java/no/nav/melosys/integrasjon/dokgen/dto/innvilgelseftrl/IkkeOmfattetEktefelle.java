package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

public class IkkeOmfattetEktefelle {
    private FamiliemedlemInfo info;
    private String begrunnelse;

    public IkkeOmfattetEktefelle(FamiliemedlemInfo info, String begrunnelse) {
        this.info = info;
        this.begrunnelse = begrunnelse;
    }

    public FamiliemedlemInfo getInfo() {
        return info;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
