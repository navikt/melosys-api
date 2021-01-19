package no.nav.melosys.domain.familie;

public class IkkeOmfattetFamilie {
    public final String uuid;
    public final String begrunnelse;
    public final String begrunnelseFritekst;

    public IkkeOmfattetFamilie(String uuid, String begrunnelse, String begrunnelseFritekst) {
        this.uuid = uuid;
        this.begrunnelse = begrunnelse;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }
}
