package no.nav.melosys.service.eessi.ruting;

import java.util.Optional;
import java.util.Set;

import no.finn.unleash.Unleash;
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
import org.springframework.stereotype.Service;

@Service
public class DefaultSedRuter implements SedRuter {

    private static final Logger log = LoggerFactory.getLogger(DefaultSedRuter.class);

    private final ProsessinstansService prosessinstansService;
    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;
    private final Unleash unleash;

    public DefaultSedRuter(ProsessinstansService prosessinstansService, FagsakService fagsakService,
                           BehandlingService behandlingService,
                           OppgaveService oppgaveService,
                           Unleash unleash) {
        this.prosessinstansService = prosessinstansService;
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
        this.unleash = unleash;
    }

    @Override
    public void rutSedTilBehandling(Prosessinstans prosessinstans, Long arkivsakID) {
        final MelosysEessiMelding eessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        SedType sedType = SedType.valueOf(eessiMelding.getSedType());
        Optional<Fagsak> fagsak = arkivsakID != null ? fagsakService.finnFagsakFraArkivsakID(arkivsakID) : Optional.empty();

        if (fagsak.isEmpty()) {
            log.info("Oppretter oppgave sed {} i rinasak {}", eessiMelding.getSedId(), eessiMelding.getRinaSaksnummer());
            oppgaveService.opprettJournalføringsoppgave(eessiMelding.getJournalpostId(), prosessinstans.hentAktørIDFraDataEllerSED());
        } else {
            Behandling behandling = fagsak.get().hentSistAktivBehandling();

            if (behandling.erAktiv()) {
                behandlingService.endreStatus(behandling.getId(), Behandlingsstatus.VURDER_DOKUMENT);
            } else {

            }

            if (skalOppdatereOppgaveForSedType(sedType)) {
                oppdaterEllerOpprettOppgave(behandling, prosessinstans, sedType);
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

    private static final Set<SedType> SED_TYPE_TIL_MANUEL_BEHANDLING = Set.of(
        SedType.A012, SedType.X001, SedType.X007, SedType.X008);

    private boolean skalOppdatereOppgaveForSedType(SedType sedType) {
        return !SED_TYPE_TIL_MANUEL_BEHANDLING.contains(sedType);
    }

    private void oppdaterEllerOpprettOppgave(Behandling behandling, Prosessinstans prosessinstans, SedType sedType) {
        Optional<Oppgave> oppgave = oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());

        String oppgaveID;
        if (oppgave.isEmpty()) {
            oppgaveID = opprettBehandlingsoppgave(behandling, prosessinstans.getData(ProsessDataKey.AKTØR_ID));
        } else {
            oppgaveID = oppgave.get().getOppgaveId();
        }

        var oppdaterOppgaveBuilder = OppgaveOppdatering.builder();

        if (sedType.erPurring()) {
            log.info("Setter prioritet til HØY for oppgave {}", oppgaveID);
            oppdaterOppgaveBuilder.beskrivelse("PURRING SED X009")
                .prioritet(PrioritetType.HOY.name());
        } else {
            oppdaterOppgaveBuilder.beskrivelse("Mottatt SED " + sedType);
        }

        oppgaveService.oppdaterOppgave(oppgaveID, oppdaterOppgaveBuilder.build());
    }

    private String opprettBehandlingsoppgave(Behandling behandling, String aktørID) {
        var oppgave = (
            unleash.isEnabled("melosys.behandle_alle_saker")
                ? OppgaveFactory.lagBehandlingsoppgave(behandling)
                : OppgaveFactory.lagBehandlingsOppgaveForType(behandling.getTema(), behandling.getType()))
            .setAktørId(aktørID)
            .setSaksnummer(behandling.getFagsak().getSaksnummer())
            .build();
        return oppgaveService.opprettOppgave(oppgave);
    }
}
