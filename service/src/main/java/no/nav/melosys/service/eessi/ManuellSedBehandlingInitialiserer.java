package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.service.BehandlingService;
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
    private final GsakFasade gsakFasade;

    @Autowired
    public ManuellSedBehandlingInitialiserer(FagsakService fagsakService, BehandlingService behandlingService, @Qualifier("system") GsakFasade gsakFasade) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.gsakFasade = gsakFasade;
    }

    /**
     * Hvis SED'en er tilknyttet en sak går den til ferdigstilling av journalpost
     * Ellers opprettes det en journalføringsoppgave
     */
    public void bestemManuellBehandling(Prosessinstans prosessinstans, MelosysEessiMelding melosysEessiMelding) throws TekniskException, FunksjonellException {

        Optional<Long> gsakSaksnummer = Optional.ofNullable(prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID, Long.class));

        SedType sedType = SedType.valueOf(melosysEessiMelding.getSedType());
        if (!gsakSaksnummer.isPresent()) {
            prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_OPPRETT_JFR_OPPG);

        } else {
            Fagsak fagsak = fagsakService.hentFagsakFraGsakSaksnummer(gsakSaksnummer.get());
            Behandling behandling = fagsak.getAktivBehandling() != null ? fagsak.getAktivBehandling() : fagsak.getSistOppdaterteBehandling();

            if (behandling.getStatus() == Behandlingsstatus.UNDER_BEHANDLING) {
                behandlingService.oppdaterStatus(behandling.getId(), Behandlingsstatus.VURDER_DOKUMENT);
            }

            if (SedType.X009 == sedType) {
                mottattPurring(fagsak.getSaksnummer());
            }

            prosessinstans.setBehandling(behandling);
            prosessinstans.setType(ProsessType.MOTTAK_SED_JOURNALFØRING);
            prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
        }
    }

    private void mottattPurring(String saksnummer) throws FunksjonellException, TekniskException {
        Optional<Oppgave> oppgave = gsakFasade.finnOppgaverMedSaksnummer(saksnummer).stream().findFirst();
        if (oppgave.isPresent()) {
            log.info("Setter prioritet til HØY for oppgave {}", oppgave.get().getOppgaveId());
            gsakFasade.oppdaterOppgavePrioritet(oppgave.get().getOppgaveId(), PrioritetType.HOY);
        }
    }
}
