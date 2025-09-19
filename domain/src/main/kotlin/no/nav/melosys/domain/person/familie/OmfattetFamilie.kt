package no.nav.melosys.domain.person.familie;

public class OmfattetFamilie {

    private final String uuid;
    private String sammensattNavn;
    private String ident;

    public OmfattetFamilie(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public void setSammensattNavn(String sammensattNavn) {
        this.sammensattNavn = sammensattNavn;
    }

    public String getIdent() {
        return ident;
    }

    public void setIdent(String ident) {
        this.ident = ident;
    }

    @Override
    public String toString() {
        return "OmfattetFamilie{" +
            "uuid='" + uuid + '\'' +
            ", sammensattNavn='" + sammensattNavn + '\'' +
            ", ident='" + ident + '\'' +
            '}';
    }
}
