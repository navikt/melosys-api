package no.nav.melosys.service.oppgave;

import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class OppgaveSystemService extends OppgaveService {

    public OppgaveSystemService(BehandlingService behandlingService,
                                FagsakService fagsakService,
                                @Qualifier("system") GsakFasade gsakFasade,
                                SaksopplysningerService saksopplysningerService,
                                SoeknadService soeknadService,
                                @Qualifier("system") TpsFasade tpsFasade) {
        super(behandlingService, fagsakService, gsakFasade, saksopplysningerService, soeknadService, tpsFasade);
    }
}
