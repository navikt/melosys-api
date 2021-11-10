package no.nav.melosys.service.vedtak;

import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.stereotype.Service;

@Service
public class EosVedtakSystemService extends EosVedtakService {

    public EosVedtakSystemService(BehandlingService behandlingService,
                                  BehandlingsresultatService behandlingsresultatService,
                                  @Qualifier("system") OppgaveService oppgaveService,
                                  ProsessinstansService prosessinstansService,
                                  @Qualifier("system") EessiService eessiService,
                                  LandvelgerService landvelgerService,
                                  @Qualifier("system") PersondataFasade persondataFasade,
                                  RegisteropplysningerService registeropplysningerService,
                                  @Qualifier("system") VedtakKontrollService vedtakKontrollService,
                                  AvklartefaktaService avklartefaktaService,
                                  ApplicationEventMulticaster melosysEventMulticaster) {
        super(behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService,
            eessiService, landvelgerService, persondataFasade, registeropplysningerService, vedtakKontrollService, avklartefaktaService, melosysEventMulticaster);
    }
}
