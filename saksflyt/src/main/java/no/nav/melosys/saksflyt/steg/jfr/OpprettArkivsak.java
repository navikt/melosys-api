package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
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
        // Digital søknad i NY-flyten kan under DB-låsen ha festet seg på en eksisterende sak (MELOSYS-8151).
        // Den saken har allerede arkivsak, så dette steget skal da hoppes over (ellers ville sjekken under kaste).
        if (Boolean.TRUE.equals(prosessinstans.getData(ProsessDataKey.DIGITAL_SØKNAD_ATTACHED_EKSISTERENDE, Boolean.class, false))) {
            log.info("Hopper over opprettelse av arkivsak — digital søknad ble festet på eksisterende sak");
            return;
        }

        Behandling behandling = prosessinstans.getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        String saksnummer = fagsak.getSaksnummer();

        if (fagsak.getGsakSaksnummer() != null) {
            throw new FunksjonellException("Kan ikke knytte fagsak " + saksnummer + " til ny arkivsak: allerede knyttet til " + fagsak.getGsakSaksnummer());
        }

        String aktørId = fagsak.finnBrukersAktørID();
        String virksomhetOrgnr = fagsak.finnVirksomhetsOrgnr();

        var tema = oppgaveFactory.utledTema(fagsak.getType(), fagsak.getTema(), behandling.getTema(), behandling.getType());

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
