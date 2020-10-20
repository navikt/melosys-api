package no.nav.melosys.service.unntaksperiode;

import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class UnntaksperiodeSystemService extends UnntaksperiodeService {
    public UnntaksperiodeSystemService(BehandlingService behandlingService,
                                       BehandlingsresultatService behandlingsresultatService,
                                       LovvalgsperiodeService lovvalgsperiodeService,
                                       @Qualifier("system") OppgaveService oppgaveService,
                                       ProsessinstansService prosessinstansService) {
        super(behandlingService, behandlingsresultatService, lovvalgsperiodeService, oppgaveService, prosessinstansService);
    }
}
