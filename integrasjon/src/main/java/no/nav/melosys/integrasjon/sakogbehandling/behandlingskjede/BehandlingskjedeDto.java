package no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede;

public class BehandlingskjedeDto {

    private String behandlingstema; // http://nav.no/kodeverk/Kodeverk/Behandlingstemaer

    public String getBehandlingstema() {
        return behandlingstema;
    }

    public void setBehandlingstema(String behandlingstema) {
        this.behandlingstema = behandlingstema;
    }
}
