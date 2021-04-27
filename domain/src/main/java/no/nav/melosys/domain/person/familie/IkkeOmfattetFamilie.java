package no.nav.melosys.domain.person.familie;

public class IkkeOmfattetFamilie {
    private final String uuid;
    private final String begrunnelse;
    private final String begrunnelseFritekst;

    public IkkeOmfattetFamilie(String uuid, String begrunnelse, String begrunnelseFritekst) {
        this.uuid = uuid;
        this.begrunnelse = begrunnelse;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public String getUuid() {
        return uuid;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }
}
