package no.nav.melosys.domain.familie;

public class OmfattetFamilie {

    private final String uuid;
    private String sammensattNavn;

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
}
