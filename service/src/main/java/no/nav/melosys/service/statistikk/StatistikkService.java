package no.nav.melosys.service.statistikk;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
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

    public Map<Behandlingstema, Long> hentBehandlingstatistikk() {
        return behandlingRepository
            .antallBehandlingerPerTemaUtenStatuser(Set.of(AVSLUTTET, MIDLERTIDIG_LOVVALGSBESLUTNING)).stream()
            .collect(Collectors.toMap(BehandlingStatistikk::behandlingstema, BehandlingStatistikk::antall));
    }
}

