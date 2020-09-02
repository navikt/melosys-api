package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ManuellSedBehandlingInitialiserer {

    private static final Logger log = LoggerFactory.getLogger(ManuellSedBehandlingInitialiserer.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;

    @Autowired
    public ManuellSedBehandlingInitialiserer(FagsakService fagsakService,
                                             BehandlingService behandlingService,
                                             @Qualifier("system") OppgaveService oppgaveService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
    }

    /**
     * Hvis SED'en er tilknyttet en sak går den til ferdigstilling av journalpost
     * Ellers opprettes det en journalføringsoppgave
     */
    public void bestemManuellBehandling(Prosessinstans prosessinstans, MelosysEessiMelding melosysEessiMelding) throws TekniskException, FunksjonellException {

        Optional<Long> gsakSaksnummer = Optional.ofNullable(prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID, Long.class));

        SedType sedType = SedType.valueOf(melosysEessiMelding.getSedType());
        if (gsakSaksnummer.isEmpty()) {
            prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_OPPRETT_JFR_OPPG);

        } else {
            Fagsak fagsak = fagsakService.hentFagsakFraGsakSaksnummer(gsakSaksnummer.get());
            Behandling behandling = fagsak.hentSistAktiveBehandling();

            if (behandling.erAktiv()) {
                behandlingService.oppdaterStatus(behandling.getId(), Behandlingsstatus.VURDER_DOKUMENT);
            }

            if (skalOppdatereOppgaveForSedType(sedType)) {
                oppdaterOppgave(behandling, prosessinstans, sedType);
            }

            prosessinstans.setBehandling(behandling);
            prosessinstans.setType(ProsessType.MOTTAK_SED_JOURNALFØRING);
            prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
        }
    }

    private boolean skalOppdatereOppgaveForSedType(SedType sedType) {
        return sedType != SedType.A012 && sedType != SedType.X001 && sedType != SedType.X007;
    }

    private void oppdaterOppgave(Behandling behandling, Prosessinstans prosessinstans, SedType sedType) throws FunksjonellException, TekniskException {
        Optional<Oppgave> oppgave = oppgaveService.finnOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());

        String oppgaveID;
        if (oppgave.isEmpty()) {
            oppgaveID = opprettBehandlingsoppgave(behandling, prosessinstans.getData(ProsessDataKey.AKTØR_ID), sedType);
        } else {
            oppgaveID = oppgave.get().getOppgaveId();
        }

        if (sedType.erPurring()) {
            oppdaterOppgavePrioritet(oppgaveID);
        }
    }

    private void oppdaterOppgavePrioritet(String oppgaveID) throws FunksjonellException, TekniskException {
        log.info("Setter prioritet til HØY for oppgave {}", oppgaveID);
        oppgaveService.oppdaterOppgave(oppgaveID,
            OppgaveOppdatering.builder()
                .prioritet(PrioritetType.HOY.name())
                .beskrivelse("PURRING SEDX009")
                .build()
        );
    }

    private String opprettBehandlingsoppgave(Behandling behandling, String aktørID, SedType sedType) throws FunksjonellException, TekniskException {
        Oppgave oppgave = OppgaveFactory.lagBehandlingsOppgaveForType(behandling.getTema(), behandling.getType())
            .setAktørId(aktørID)
            .setSaksnummer(behandling.getFagsak().getSaksnummer())
            .setBeskrivelse("Mottatt SED " + sedType)
            .build();
        return oppgaveService.opprettOppgave(oppgave);
    }
}
