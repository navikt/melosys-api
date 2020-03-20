package no.nav.melosys.integrasjon.oppgave.konsument.dto;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OppgaveDto extends OpprettOppgaveDto {
    private String id;
    private ZonedDateTime opprettetTidspunkt;
    private String status;
    private int versjon;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ZonedDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public void setOpprettetTidspunkt(ZonedDateTime opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getVersjon() {
        return versjon;
    }

    public void setVersjon(int versjon) {
        this.versjon = versjon;
    }

    @Override
    public String toString() {
        return "OppgaveDto{" +
            "id='" + id + '\'' +
            ", status='" + status + '\'' +
            ", versjon=" + versjon +
            "} " + super.toString();
    }
}
