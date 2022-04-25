package no.nav.melosys.service.statistikk;

import java.util.Collections;
import java.util.Map;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import org.springframework.stereotype.Service;

@Service
public class StatistikkService {

    public StatistikkService() {
    }

    public Map<Behandlingstema, Long> hentUtildelteOppgaverStatistikk() {
        return Collections.emptyMap();
    }
}

