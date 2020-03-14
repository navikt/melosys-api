package no.nav.melosys.service.oppgave;

import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class OppgaveSystemService extends OppgaveService {

    public OppgaveSystemService(BehandlingService behandlingService,
                                FagsakService fagsakService,
                                @Qualifier("system") OppgaveFasade oppgaveFasade,
                                SaksopplysningerService saksopplysningerService,
                                BehandlingsgrunnlagService behandlingsgrunnlagService,
                                @Qualifier("system") TpsFasade tpsFasade) {
        super(behandlingService, fagsakService, oppgaveFasade, saksopplysningerService, behandlingsgrunnlagService, tpsFasade);
    }
}
