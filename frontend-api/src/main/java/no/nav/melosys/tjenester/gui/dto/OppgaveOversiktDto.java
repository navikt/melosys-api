package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

import no.nav.melosys.service.oppgave.dto.BehandlingsoppgaveDto;
import no.nav.melosys.service.oppgave.dto.JournalfoeringsoppgaveDto;

public class OppgaveOversiktDto {
    private List<JournalfoeringsoppgaveDto> journalforing;
    private List<BehandlingsoppgaveDto> saksbehandling;

    public List<JournalfoeringsoppgaveDto> getJournalforing() {
        return journalforing;
    }

    public void setJournalforing(List<JournalfoeringsoppgaveDto> journalforing) {
        this.journalforing = journalforing;
    }

    public List<BehandlingsoppgaveDto> getSaksbehandling() {
        return saksbehandling;
    }

    public void setSaksbehandling(List<BehandlingsoppgaveDto> saksbehandling) {
        this.saksbehandling = saksbehandling;
    }
}
