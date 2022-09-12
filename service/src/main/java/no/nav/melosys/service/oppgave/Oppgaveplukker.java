package no.nav.melosys.service.oppgave;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import no.nav.melosys.service.oppgave.dto.TilbakeleggingDto;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Oppgaveplukker {
    private static final Logger log = LoggerFactory.getLogger(Oppgaveplukker.class);

    private final OppgaveFasade oppgaveFasade;
    private final OppgaveTilbakeleggingRepository oppgaveTilbakkeleggingRepo;
    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;
    private final Unleash unleash;

    public Oppgaveplukker(OppgaveFasade oppgaveFasade, OppgaveTilbakeleggingRepository oppgaveTilbakeleggingRepo,
                          FagsakService fagsakService, BehandlingService behandlingService, OppgaveService oppgaveService, Unleash unleash) {
        this.oppgaveFasade = oppgaveFasade;
        this.oppgaveTilbakkeleggingRepo = oppgaveTilbakeleggingRepo;
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
        this.unleash = unleash;
    }

    @Transactional
    public synchronized Optional<Oppgave> plukkOppgave(String saksbehandlerID, PlukkOppgaveInnDto plukkDto) {
        List<Oppgave> utildelteOppgaverEtterFrist = null;
        if (unleash.isEnabled("melosys.oppgave.oppretting")) {
            utildelteOppgaverEtterFrist = oppgaveFasade.finnUtildelteOppgaverEtterFrist(null, OppgaveFactory.utledBehandlingstema(plukkDto.getSakstema(), plukkDto.getSakstype(), plukkDto.getBehandlingstema(), plukkDto.getBehandlingstype()));
        } else {
            var parametere = OppgaveFactory.hentOppgaveParametere(plukkDto.getBehandlingstema());
            oppgaveFasade.finnUtildelteOppgaverEtterFrist(parametere.behandlingstype, parametere.behandlingstema);
        }

        List<Oppgave> filtrerteOppgaver = utildelteOppgaverEtterFrist.stream()
            .filter(oppgave -> {
                String saksnummer = oppgave.getSaksnummer();
                Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
                if (fagsak == null) {
                    log.error("Fant ikke fagsak {} for oppgave {}", saksnummer, oppgave.getOppgaveId());
                    throw new TekniskException("Fant ikke fagsak " + saksnummer);
                }
                return !venterSakPaaDokumentasjonEllerFagligAvklaring(fagsak);
            }).toList();

        Optional<Oppgave> valg = filtrerteOppgaver.stream()
            .sorted(Oppgave.lavestTilHøyestPrioritet.reversed())
            .filter(oppgave -> !erTilbakeLagt(saksbehandlerID, oppgave.getOppgaveId()))
            .findFirst();

        if (valg.isPresent()) {
            oppdaterBehandlingsstatus(valg.get().getSaksnummer());
            oppgaveService.tildelOppgave(valg.get().getOppgaveId(), saksbehandlerID);
        }
        return valg;
    }

    private boolean venterSakPaaDokumentasjonEllerFagligAvklaring(Fagsak fagsak) {
        Behandling behandling = fagsak.hentSistAktivBehandling();
        if (behandling.erVenterForDokumentasjon()) {
            if (behandling.getDokumentasjonSvarfristDato() == null) {
                log.error("Behandling {} tilhørende {} avventer dokumentasjon, men har ingen svarfristdato.",
                    behandling.getId(), fagsak.getSaksnummer());
                return true;
            }
            return behandling.getDokumentasjonSvarfristDato().isAfter(Instant.now());
        }
        return behandling.harStatus(Behandlingsstatus.AVVENT_FAGLIG_AVKLARING);
    }

    private boolean erTilbakeLagt(String saksbehandlerID, String oppgaveId) {
        List<OppgaveTilbakelegging> tilbakelegging = oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(saksbehandlerID, oppgaveId);
        return !tilbakelegging.isEmpty();
    }

    private void oppdaterBehandlingsstatus(String saksnummer) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        Behandling behandling = fagsak.hentAktivBehandling();
        if (behandling != null && (behandling.getStatus() == Behandlingsstatus.SVAR_ANMODNING_MOTTATT || behandling.getStatus() == Behandlingsstatus.OPPRETTET)) {
            behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
            behandlingService.lagre(behandling);
        }
    }

    @Transactional
    public synchronized void leggTilbakeOppgave(String saksbehandlerID, TilbakeleggingDto tilbakelegging) {
        Behandling behandling = behandlingService.hentBehandling(tilbakelegging.getBehandlingID());

        Fagsak fagsak = behandling.getFagsak();
        Oppgave oppgave = oppgaveService.hentÅpenOppgaveMedFagsaksnummer(fagsak.getSaksnummer());

        String oppgaveId = oppgave.getOppgaveId();
        if (!tilbakelegging.isVenterPåDokumentasjon()) {
            OppgaveTilbakelegging oppgaveTilbakelegging = new OppgaveTilbakelegging();
            oppgaveTilbakelegging.setOppgaveId(oppgaveId);
            oppgaveTilbakelegging.setSaksbehandlerId(saksbehandlerID);
            oppgaveTilbakelegging.setBegrunnelse(tilbakelegging.getBegrunnelse());
            oppgaveTilbakelegging.setRegistrertDato(LocalDateTime.now());
            oppgaveTilbakkeleggingRepo.save(oppgaveTilbakelegging);
        }

        oppgaveFasade.leggTilbakeOppgave(oppgaveId);
        log.info("Oppgave med oppgaveId {} er lagt tilbake. ", oppgaveId);
    }

}
