package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sak.ArkivsakService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_ARKIVSAK;
import static no.nav.melosys.service.oppgave.OppgaveFactory.utledTema;

@Component
public class OpprettArkivsak implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettArkivsak.class);

    private final FagsakService fagsakService;
    private final ArkivsakService arkivsakService;

    public OpprettArkivsak(FagsakService fagsakService, ArkivsakService arkivsakService) {
        this.fagsakService = fagsakService;
        this.arkivsakService = arkivsakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_ARKIVSAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        Behandling behandling = prosessinstans.getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        String saksnummer = fagsak.getSaksnummer();

        if (fagsak.getGsakSaksnummer() != null) {
            throw new FunksjonellException("Kan ikke knytte fagsak " + saksnummer + " til ny arkivsak: allerede knyttet til " + fagsak.getGsakSaksnummer());
        }

        Optional<String> aktørId = fagsak.finnBrukersAktørID();
        Optional<String> virksomhetOrgnr = fagsak.finnVirksomhetsOrgnr();

        var tema = utledTema(behandling.getFagsak().getTema());

        Long arkivsakID;
        if (aktørId.isPresent()) {
            arkivsakID = arkivsakService.opprettSakForBruker(saksnummer, tema, aktørId.get());
        } else if (virksomhetOrgnr.isPresent()) {
            arkivsakID = arkivsakService.opprettSakForVirksomhet(saksnummer, tema, virksomhetOrgnr.get());
        } else {
            throw new FunksjonellException("Finner verken bruker eller virksomhet tilknyttet fagsak " + saksnummer);
        }
        fagsak.setGsakSaksnummer(arkivsakID);
        fagsakService.lagre(fagsak);

        log.info("Opprettet arkivsak {} for fagsak {}", arkivsakID, saksnummer);
    }
}
