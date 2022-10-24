package no.nav.melosys.saksflyt.steg.behandling;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpprettNyBehandlingFraSed implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettNyBehandlingFraSed.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;
    private final Unleash unleash;

    public OpprettNyBehandlingFraSed(FagsakService fagsakService,
                                     BehandlingService behandlingService,
                                     OppgaveService oppgaveService,
                                     Unleash unleash) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
        this.unleash = unleash;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_OPPRETT_NY_BEHANDLING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {

        var arkivsakID = prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID, Long.class);
        var behandlingstema = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class);
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, true);
        var eessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        if (arkivsakID == null) {
            throw new TekniskException("ArkivsakID kan ikke være null");
        } else if (behandlingstema == null) {
            throw new TekniskException("Behandlingstema kan ikke være null");
        }

        var fagsak = fagsakService.hentFagsakFraArkivsakID(arkivsakID);

        avsluttTidligereBehandling(fagsak);
        var behandling = behandlingService.nyBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING,
            unleash.isEnabled("melosys.behandle_alle_saker") ? Behandlingstyper.FØRSTEGANG : Behandlingstyper.SED,
            behandlingstema, eessiMelding.getJournalpostId(), eessiMelding.getDokumentId());

        fagsak.getBehandlinger().add(behandling);
        fagsakService.lagre(fagsak);

        ferdigstillOppgave(fagsak.getSaksnummer());
        log.info("Opprettet ny behandling for fagsak {}", arkivsakID);
        prosessinstans.setBehandling(behandling);
    }

    private void avsluttTidligereBehandling(Fagsak fagsak) {
        var aktivBehandling = fagsak.hentAktivBehandling();

        if (aktivBehandling != null) {
            behandlingService.avsluttBehandling(aktivBehandling.getId());
        }
    }

    private void ferdigstillOppgave(String saksnummer) {
        oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(saksnummer)
            .map(Oppgave::getOppgaveId)
            .ifPresent(oppgaveService::ferdigstillOppgave);
    }
}
