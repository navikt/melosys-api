package no.nav.melosys.service.vedtak;

import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class VedtakSystemService extends VedtakService {

    public VedtakSystemService(BehandlingService behandlingService,
                               BehandlingsresultatService behandlingsresultatService,
                               @Qualifier("system") OppgaveService oppgaveService,
                               ProsessinstansService prosessinstansService,
                               EessiService eessiService,
                               LandvelgerService landvelgerService) {
        super(behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService, eessiService, landvelgerService);
    }
}
