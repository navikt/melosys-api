package no.nav.melosys.integrasjon.oppgave.konsument.dto;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import no.nav.melosys.domain.oppgave.Oppgave;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OppgaveDto extends OpprettOppgaveDto {
    private String id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
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

    public static OppgaveDto av(Oppgave oppgave) {
        var oppgaveDto = new OppgaveDto();
        oppgaveDto.setId(oppgave.getOppgaveId());
        oppgaveDto.setStatus(oppgave.getStatus());
        oppgaveDto.setVersjon(oppgave.getVersjon());
        oppgaveDto.setOpprettetTidspunkt(oppgave.getOpprettetTidspunkt());
        oppgaveDto.setAktivDato(oppgave.getAktivDato());
        oppgaveDto.setAktørId(oppgave.getAktørId());
        oppgaveDto.setOrgnr(oppgave.getOrgnr());
        oppgaveDto.setBehandlesAvApplikasjon(oppgave.getBehandlesAvApplikasjon().getKode());
        oppgaveDto.setBehandlingstema(oppgave.getBehandlingstema());
        oppgaveDto.setBehandlingstype(oppgave.getBehandlingstype());
        oppgaveDto.setBeskrivelse(oppgave.getBeskrivelse());
        oppgaveDto.setFristFerdigstillelse(oppgave.getFristFerdigstillelse());
        oppgaveDto.setJournalpostId(oppgave.getJournalpostId());
        oppgaveDto.setOppgavetype(oppgave.getOppgavetype().getKode());
        oppgaveDto.setPrioritet(oppgave.getPrioritet().toString());
        oppgaveDto.setSaksreferanse(oppgave.getSaksnummer());
        oppgaveDto.setTema(oppgave.getTema().getKode());
        oppgaveDto.setTemagruppe(oppgave.getTemagruppe());
        oppgaveDto.setTildeltEnhetsnr(oppgave.getTildeltEnhetsnr());
        oppgaveDto.setTilordnetRessurs(oppgave.getTilordnetRessurs());
        return oppgaveDto;
    }
}
