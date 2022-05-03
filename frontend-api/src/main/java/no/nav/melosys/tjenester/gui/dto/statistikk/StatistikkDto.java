package no.nav.melosys.tjenester.gui.dto.statistikk;

import java.util.Map;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

public class StatistikkDto {
    private final Map<Behandlingstema, Long> antallUtildelteOppgaverPerBehandlingstema;

    public StatistikkDto(Map<Behandlingstema, Long> antallUtildelteOppgaverPerBehandlingstema) {
        this.antallUtildelteOppgaverPerBehandlingstema = antallUtildelteOppgaverPerBehandlingstema;
    }
}
