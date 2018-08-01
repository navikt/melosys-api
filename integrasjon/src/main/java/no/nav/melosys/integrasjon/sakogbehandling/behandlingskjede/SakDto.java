package no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede;

import java.time.LocalDateTime;
import java.util.List;

public class SakDto {

    private String saksId;

    private String sakstema; // http://nav.no/kodeverk/Kodeverk/Sakstemaer

    private LocalDateTime opprettet;

    private LocalDateTime lukket;

    private List<BehandlingskjedeDto> behandlingskjede;

    public String getSaksId() {
        return saksId;
    }

    public void setSaksId(String saksId) {
        this.saksId = saksId;
    }

    public String getSakstema() {
        return sakstema;
    }

    public void setSakstema(String sakstema) {
        this.sakstema = sakstema;
    }

    public LocalDateTime getOpprettet() {
        return opprettet;
    }

    public void setOpprettet(LocalDateTime opprettet) {
        this.opprettet = opprettet;
    }

    public LocalDateTime getLukket() {
        return lukket;
    }

    public void setLukket(LocalDateTime lukket) {
        this.lukket = lukket;
    }

    public List<BehandlingskjedeDto> getBehandlingskjede() {
        return behandlingskjede;
    }

    public void setBehandlingskjede(List<BehandlingskjedeDto> behandlingskjede) {
        this.behandlingskjede = behandlingskjede;
    }
}
