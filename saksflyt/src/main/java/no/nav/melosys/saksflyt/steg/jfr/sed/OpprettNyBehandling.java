package no.nav.melosys.saksflyt.steg.jfr.sed;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.DOKUMENT_ID;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.JOURNALPOST_ID;

@Component
public class OpprettNyBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettNyBehandling.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;

    public OpprettNyBehandling(FagsakService fagsakService,
                               BehandlingService behandlingService,
                               @Qualifier("system") OppgaveService oppgaveService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_OPPRETT_NY_BEHANDLING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {

        Long gsakSaksnummer = prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID, Long.class);
        Behandlingstema behandlingstema = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class);
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, true);

        if (gsakSaksnummer == null) {
            throw new TekniskException("Gsaksaksnummer kan ikke være null");
        } else if (behandlingstema == null) {
            throw new TekniskException("Behandlingstype kan ikke være null");
        }

        Fagsak fagsak = fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer)
            .orElseThrow(() -> new TekniskException("Finnes en kobling til gsakSaksnummer " +
                gsakSaksnummer + ", men finner ingen fagsak!"));

        avsluttTidligereBehandling(fagsak);
        Behandling behandling = behandlingService.nyBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.SED, behandlingstema,
            prosessinstans.getData(JOURNALPOST_ID), prosessinstans.getData(DOKUMENT_ID));

        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.getBehandlinger().add(behandling);
        fagsakService.lagre(fagsak);

        ferdigstillOppgave(fagsak.getSaksnummer());
        log.info("Opprettet ny behandling for fagsak {}", gsakSaksnummer);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
    }

    private void avsluttTidligereBehandling(Fagsak fagsak) throws TekniskException, FunksjonellException {
        Behandling aktivBehandling = fagsak.getAktivBehandling();

        if (aktivBehandling != null) {
            behandlingService.avsluttBehandling(aktivBehandling.getId());
        }
    }

    private void ferdigstillOppgave(String saksnummer) throws FunksjonellException, TekniskException {
        Optional<String> oppgaveID = oppgaveService.finnOppgaveMedFagsaksnummer(saksnummer)
            .map(Oppgave::getOppgaveId);

        if (oppgaveID.isPresent()) {
            oppgaveService.ferdigstillOppgave(oppgaveID.get());
        }
    }
}
