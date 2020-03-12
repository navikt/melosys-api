package no.nav.melosys.service.oppgave;


import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class OppgaveplukkerSystem extends Oppgaveplukker {
    public OppgaveplukkerSystem(@Qualifier("system") OppgaveFasade oppgaveFasade,
                                OppgaveTilbakeleggingRepository oppgaveTilbakeleggingRepo,
                                FagsakService fagsakService,
                                BehandlingService behandlingService,
                                @Qualifier("system") OppgaveService oppgaveService) {
        super(oppgaveFasade, oppgaveTilbakeleggingRepo, fagsakService, behandlingService, oppgaveService);
    }
}
