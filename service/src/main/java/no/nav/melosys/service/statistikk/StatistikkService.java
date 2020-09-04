package no.nav.melosys.service.statistikk;

import java.util.List;
import java.util.Set;

import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingStatistikk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.AVSLUTTET;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING;

@Service
public class StatistikkService {
    private final BehandlingRepository behandlingRepository;

    @Autowired
    public StatistikkService(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    public List<BehandlingStatistikk> hentBehandlingstatistikk() {
        return behandlingRepository.antallÅpneBehandlingerPerBehandlingstema(
            Set.of(AVSLUTTET, MIDLERTIDIG_LOVVALGSBESLUTNING)
        );
    }
}

