package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

public class FamilieInfo {
    private String navn;
    private String ident;
    private IdentType identType;

    public FamilieInfo(String navn, String ident, IdentType identType) {
        this.navn = navn;
        this.ident = ident;
        this.identType = identType;
    }

    public String getNavn() {
        return navn;
    }

    public String getIdent() {
        return ident;
    }

    public IdentType getIdentType() {
        return identType;
    }
}
