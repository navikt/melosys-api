package no.nav.melosys.service.oppgave;

import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class OppgaveSystemService extends OppgaveService {

    public OppgaveSystemService(@Qualifier("system") GsakFasade gsakFasade,
                                FagsakService fagsakService,
                                BehandlingService behandlingService,
                                @Qualifier("system") TpsFasade tpsFasade,
                                SaksopplysningerService saksopplysningerService) {
        super(gsakFasade, fagsakService, behandlingService, tpsFasade, saksopplysningerService);
    }
}
