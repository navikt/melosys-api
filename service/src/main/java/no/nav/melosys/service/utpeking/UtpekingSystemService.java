package no.nav.melosys.service.utpeking;

import no.nav.melosys.repository.UtpekingsperiodeRepository;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class UtpekingSystemService extends UtpekingService {
    public UtpekingSystemService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                                 @Qualifier("system") EessiService eessiService, LandvelgerService landvelgerService,
                                 LovvalgsperiodeService lovvalgsperiodeService, @Qualifier("system") OppgaveService oppgaveService,
                                 ProsessinstansService prosessinstansService, UtpekingsperiodeRepository utpekingsperiodeRepository,
                                 @Qualifier("system") VedtakKontrollService vedtakKontrollService,
                                 ApplicationEventMulticaster melosysEventMulticaster) {
        super(behandlingService, behandlingsresultatService, eessiService, landvelgerService, lovvalgsperiodeService, oppgaveService,
              prosessinstansService, utpekingsperiodeRepository, vedtakKontrollService, melosysEventMulticaster);
    }
}
