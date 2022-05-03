package no.nav.melosys.service.oppgave.dto;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

public class PlukkOppgaveInnDto {

    private Behandlingstema behandlingstema;

    public Behandlingstema getBehandlingstema() {
        return behandlingstema;
    }

    public void setBehandlingstema(Behandlingstema behandlingstema) {
        this.behandlingstema = behandlingstema;
    }
}
