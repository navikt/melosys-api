package no.nav.melosys.tjenester.gui.dto.statistikk;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.repository.BehandlingStatistikk;

public class StatistikkDto {
    private final Map<Behandlingstema, Long> aapneBehandlinger;

    private StatistikkDto(Map<Behandlingstema, Long> antallÅpneBehandlingerPerTema) {
        aapneBehandlinger = antallÅpneBehandlingerPerTema;
    }

    public static StatistikkDto av(List<BehandlingStatistikk> behandlingStatistikk) {
        return new StatistikkDto(behandlingStatistikk.stream()
            .collect(Collectors.toMap(BehandlingStatistikk::getBehandlingstema, BehandlingStatistikk::getAntall)));
    }

    public Map<Behandlingstema, Long> getAapneBehandlinger() {
        return aapneBehandlinger;
    }
}
