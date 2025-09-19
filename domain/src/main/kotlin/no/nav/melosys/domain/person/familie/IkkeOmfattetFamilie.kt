package no.nav.melosys.domain.person.familie;

public class IkkeOmfattetFamilie {
    private final String uuid;
    private final String begrunnelse;
    private final String begrunnelseFritekst;
    private String sammensattNavn;
    private String ident;

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

    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public String getIdent() {
        return ident;
    }

    public void setSammensattNavn(String sammensattNavn) {
        this.sammensattNavn = sammensattNavn;
    }

    public void setIdent(String ident) {
        this.ident = ident;
    }

    @Override
    public String toString() {
        return "IkkeOmfattetFamilie{" +
            "uuid='" + uuid + '\'' +
            ", begrunnelse='" + begrunnelse + '\'' +
            ", begrunnelseFritekst='" + begrunnelseFritekst + '\'' +
            ", sammensattNavn='" + sammensattNavn + '\'' +
            ", ident='" + ident + '\'' +
            '}';
    }
}
