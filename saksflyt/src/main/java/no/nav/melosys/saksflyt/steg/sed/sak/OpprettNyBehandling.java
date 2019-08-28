package no.nav.melosys.saksflyt.steg.sed.sak;

import java.util.Collection;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.DOKUMENT_ID;
import static no.nav.melosys.domain.ProsessDataKey.JOURNALPOST_ID;

@Component
public class OpprettNyBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettNyBehandling.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final GsakFasade gsakFasade;

    public OpprettNyBehandling(FagsakService fagsakService, BehandlingService behandlingService, GsakFasade gsakFasade) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.gsakFasade = gsakFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_OPPRETT_NY_BEHANDLING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {

        Long gsakSaksnummer = prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID, Long.class);
        Behandlingstyper behandlingsType = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class);
        prosessinstans.setData(ProsessDataKey.ER_ENDRING, true);

        if (gsakSaksnummer == null) {
            throw new TekniskException("Gsaksaksnummer kan ikke være null");
        } else if (behandlingsType == null) {
            throw new TekniskException("Behandlignstype kan ikke være null");
        }

        Fagsak fagsak = fagsakService.hentFagsakFraGsakSaksnummer(gsakSaksnummer)
            .orElseThrow(() -> new TekniskException("Finnes en kobling til gsakSaksnummer " +
                gsakSaksnummer + ", men finner ingen fagsak!"));

        if (!Saksstatuser.OPPRETTET.equals(fagsak.getStatus())) {
            fagsak.setStatus(Saksstatuser.OPPRETTET);
        }

        avsluttTidligereBehandling(fagsak);
        ferdigstillOppgave(fagsak.getSaksnummer());
        Behandling behandling = behandlingService.nyBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING, behandlingsType,
            prosessinstans.getData(JOURNALPOST_ID), prosessinstans.getData(DOKUMENT_ID));
        log.info("Opprettet ny behandling for fagsak {}", gsakSaksnummer);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
    }

    private void avsluttTidligereBehandling(Fagsak fagsak) throws TekniskException, IkkeFunnetException {
        Behandling aktivBehandling = fagsak.getAktivBehandling();

        if (aktivBehandling != null) {
            behandlingService.avsluttBehandling(aktivBehandling.getId());
        }
    }

    private void ferdigstillOppgave(String saksnummer) throws FunksjonellException, TekniskException {
        Collection<String> oppgaveIDer = gsakFasade.finnAlleOppgaverMedSaksnummer(saksnummer)
            .stream().map(Oppgave::getOppgaveId).collect(Collectors.toList());

        for (String oppgaveID : oppgaveIDer) {
            gsakFasade.ferdigstillOppgave(oppgaveID);
        }
    }
}
