package no.nav.melosys.tjenester.gui.dto.statistikk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nav.melosys.repository.BehandlingStatistikk;

public class StatistikkDto {
    private Map<String, Long> behandlinger;

    private StatistikkDto() {
        behandlinger = new HashMap<>();
    }

    public static StatistikkDto av(List<BehandlingStatistikk> behandlingStatistikk) {
        StatistikkDto statistikkDto = new StatistikkDto();
        behandlingStatistikk.stream()
            .forEach(s -> statistikkDto.getBehandlinger().put(s.getBehandlingstema().getKode(), s.getAntall()));
        return statistikkDto;
    }

    public Map<String, Long> getBehandlinger() {
        return behandlinger;
    }
}
