package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.sak.ArkivsakService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.saksflytapi.domain.ProsessSteg.OPPRETT_ARKIVSAK;

@Component
public class OpprettArkivsak implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettArkivsak.class);

    private final FagsakService fagsakService;
    private final ArkivsakService arkivsakService;
    private final OppgaveFactory oppgaveFactory;

    public OpprettArkivsak(FagsakService fagsakService, ArkivsakService arkivsakService, OppgaveFactory oppgaveFactory) {
        this.fagsakService = fagsakService;
        this.arkivsakService = arkivsakService;
        this.oppgaveFactory = oppgaveFactory;
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

        String aktørId = fagsak.finnBrukersAktørID();
        String virksomhetOrgnr = fagsak.finnVirksomhetsOrgnr();

        var tema = oppgaveFactory.utledTema(fagsak.getType(), fagsak.getTema(), behandling.getTema());

        Long arkivsakID;
        if (aktørId != null) {
            arkivsakID = arkivsakService.opprettSakForBruker(saksnummer, tema, aktørId);
        } else if (virksomhetOrgnr != null) {
            arkivsakID = arkivsakService.opprettSakForVirksomhet(saksnummer, tema, virksomhetOrgnr);
        } else {
            throw new FunksjonellException("Finner verken bruker eller virksomhet tilknyttet fagsak " + saksnummer);
        }
        fagsak.setGsakSaksnummer(arkivsakID);
        fagsakService.lagre(fagsak);

        log.info("Opprettet arkivsak {} for fagsak {}", arkivsakID, saksnummer);
    }
}
