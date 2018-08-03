package no.nav.melosys.domain.dokument.sakogbehandling;

public class Behandlingskjede {

    private String behandlingskjedetype; // http://nav.no/kodeverk/Kodeverk/Behandlingskjedetyper

    private String behandlingstema; // http://nav.no/kodeverk/Kodeverk/Behandlingstema

    public String getBehandlingskjedetype() {
        return behandlingskjedetype;
    }

    public void setBehandlingskjedetype(String behandlingskjedetype) {
        this.behandlingskjedetype = behandlingskjedetype;
    }

    public String getBehandlingstema() {
        return behandlingstema;
    }

    public void setBehandlingstema(String behandlingstema) {
        this.behandlingstema = behandlingstema;
    }
}
