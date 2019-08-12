package no.nav.melosys.service.vedtak;

import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class VedtakSystemService extends VedtakService {


    public VedtakSystemService(BehandlingRepository behandlingRepository,
                               @Qualifier("system") OppgaveService oppgaveService,
                               ProsessinstansService prosessinstansService) {
        super(behandlingRepository, oppgaveService, prosessinstansService);
    }
}
