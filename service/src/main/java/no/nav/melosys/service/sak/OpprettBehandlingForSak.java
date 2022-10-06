package no.nav.melosys.service.sak;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpprettBehandlingForSak {
    private final FagsakService fagsakService;
    private final ProsessinstansService prosessinstansService;

    public OpprettBehandlingForSak(FagsakService fagsakService, ProsessinstansService prosessinstansService) {
        this.fagsakService = fagsakService;
        this.prosessinstansService = prosessinstansService;
    }

    @Transactional
    public void opprettBehandling(String saksnummer, OpprettSakDto opprettSakDto) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);

        if (fagsak.hentAktivBehandling() != null) {
            throw new FunksjonellException("Det finnes allerede en aktiv behandling på fagsak " + saksnummer);
        }
        prosessinstansService.opprettNyBehandlingForSak(saksnummer, opprettSakDto);
    }
}
