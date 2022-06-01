package no.nav.melosys.service.oppgave;

import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
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
                                @Qualifier("system") PersondataFasade persondataFasade,
                                @Qualifier("system") EregFasade eregFasade) {
        super(behandlingService, fagsakService, oppgaveFasade, saksopplysningerService, behandlingsgrunnlagService, persondataFasade, eregFasade);
    }
}
