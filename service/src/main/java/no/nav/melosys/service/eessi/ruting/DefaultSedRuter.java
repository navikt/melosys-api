package no.nav.melosys.service.eessi.ruting;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DefaultSedRuter implements SedRuter {

    private static final Logger log = LoggerFactory.getLogger(DefaultSedRuter.class);

    private final ProsessinstansService prosessinstansService;
    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;

    @Autowired
    public DefaultSedRuter(ProsessinstansService prosessinstansService, FagsakService fagsakService,
                           BehandlingService behandlingService,
                           @Qualifier("system") OppgaveService oppgaveService) {
        this.prosessinstansService = prosessinstansService;
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
    }

    /**
     * Hvis SED'en er tilknyttet en sak går den til ferdigstilling av journalpost
     * Ellers opprettes det en journalføringsoppgave
     */
    public void rutSedTilBehandling(Prosessinstans prosessinstans, Long arkivsakID) {
        final MelosysEessiMelding eessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        SedType sedType = SedType.valueOf(eessiMelding.getSedType());
        Optional<Fagsak> fagsak = arkivsakID != null ? fagsakService.finnFagsakFraArkivsakID(arkivsakID) : Optional.empty();

        if (fagsak.isEmpty()) {
            log.info("Oppretter oppgave sed {} i rinasak {}", eessiMelding.getSedId(), eessiMelding.getRinaSaksnummer());
            oppgaveService.opprettJournalføringsoppgave(eessiMelding.getJournalpostId(), prosessinstans.hentAktørIDFraDataEllerSED());
        } else {
            Behandling behandling = fagsak.get().hentSistAktiveBehandling();

            if (behandling.erAktiv()) {
                behandlingService.oppdaterStatus(behandling.getId(), Behandlingsstatus.VURDER_DOKUMENT);
            }

            if (skalOppdatereOppgaveForSedType(sedType)) {
                oppdaterOppgave(behandling, prosessinstans, sedType);
            }

            prosessinstans.setBehandling(behandling);
            opprettJournalføringProsess(eessiMelding, behandling);
        }
    }

    private void opprettJournalføringProsess(MelosysEessiMelding melosysEessiMelding, Behandling sistAktiveBehandling) {
        prosessinstansService.opprettProsessinstansSedJournalføring(
            sistAktiveBehandling,
            melosysEessiMelding
        );
    }

    private boolean skalOppdatereOppgaveForSedType(SedType sedType) {
        return sedType != SedType.A012 && sedType != SedType.X001 && sedType != SedType.X007;
    }

    private void oppdaterOppgave(Behandling behandling, Prosessinstans prosessinstans, SedType sedType) {
        Optional<Oppgave> oppgave = oppgaveService.finnÅpenOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());

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

    private void oppdaterOppgavePrioritet(String oppgaveID) {
        log.info("Setter prioritet til HØY for oppgave {}", oppgaveID);
        oppgaveService.oppdaterOppgave(oppgaveID,
            OppgaveOppdatering.builder()
                .prioritet(PrioritetType.HOY.name())
                .beskrivelse("PURRING SEDX009")
                .build()
        );
    }

    private String opprettBehandlingsoppgave(Behandling behandling, String aktørID, SedType sedType) {
        Oppgave oppgave = OppgaveFactory.lagBehandlingsOppgaveForType(behandling.getTema(), behandling.getType())
            .setAktørId(aktørID)
            .setSaksnummer(behandling.getFagsak().getSaksnummer())
            .setBeskrivelse("Mottatt SED " + sedType)
            .build();
        return oppgaveService.opprettOppgave(oppgave);
    }
}
