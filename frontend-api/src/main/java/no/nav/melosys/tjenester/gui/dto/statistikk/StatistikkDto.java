package no.nav.melosys.tjenester.gui.dto.statistikk;

import java.util.Map;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

public class StatistikkDto {
    private final Map<Behandlingstema, Long> aapneBehandlinger;

    public StatistikkDto(Map<Behandlingstema, Long> antallÅpneBehandlingerPerTema) {
        aapneBehandlinger = antallÅpneBehandlingerPerTema;
    }

    public Map<Behandlingstema, Long> getAapneBehandlinger() {
        return aapneBehandlinger;
    }
}
