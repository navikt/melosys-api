package no.nav.melosys.service.statistikk;

import java.util.List;

import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingStatistikk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatistikkService {
    private BehandlingRepository behandlingRepository;

    @Autowired
    public StatistikkService(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    public List<BehandlingStatistikk> hentBehandlingstatistikk() {
        return behandlingRepository.antallÅpneBehandlingerPerBehandlingstema();
    }
}

