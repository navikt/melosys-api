package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSNUMMER;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_NY_BEHANDLING;

@Component
public class OpprettNyBehandling implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettNyBehandling.class);

    private final FagsakService fagsakService;

    public OpprettNyBehandling(FagsakService fagsakService) {
        this.fagsakService = fagsakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_NY_BEHANDLING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        String saksnummer = prosessinstans.getData(SAKSNUMMER, String.class);
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        Behandling behandling = fagsak.hentAktivBehandling(); // TODO: lag ny behandling
        prosessinstans.setBehandling(behandling);
        log.info("Opprettet fagsak {} med behandling {}", fagsak.getSaksnummer(), behandling.getId());
    }
}
