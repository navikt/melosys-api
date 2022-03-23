package no.nav.melosys.service.oppgave;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import no.nav.melosys.service.oppgave.dto.TilbakeleggingDto;
import no.nav.melosys.service.sak.FagsakService;
import org.apache.commons.lang3.StringUtils;
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

    public Oppgaveplukker(OppgaveFasade oppgaveFasade, OppgaveTilbakeleggingRepository oppgaveTilbakeleggingRepo,
                          FagsakService fagsakService, BehandlingService behandlingService, OppgaveService oppgaveService) {
        this.oppgaveFasade = oppgaveFasade;
        this.oppgaveTilbakkeleggingRepo = oppgaveTilbakeleggingRepo;
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
    }

    @Transactional
    public synchronized Optional<Oppgave> plukkOppgave(String saksbehandlerID, PlukkOppgaveInnDto plukkDto) {
        validerPlukkOppgave(plukkDto);

        Behandlingstema behandlingstema = Behandlingstema.valueOf(plukkDto.getBehandlingstema());
        List<Oppgave> ufordelteOppgaver = oppgaveFasade.finnUtildelteOppgaverEtterFrist(OppgaveFactory.hentOppgaveParametere(behandlingstema).behandlingstype);

        fjernOppgaverSomVenterForDokumentasjonEllerFagligAvklaring(ufordelteOppgaver);

        Optional<Oppgave> valg = velgNeste(saksbehandlerID, ufordelteOppgaver);

        if (valg.isPresent()) {
            oppdaterBehandlingsstatus(valg.get().getSaksnummer());
            oppgaveService.tildelOppgave(valg.get().getOppgaveId(), saksbehandlerID);
        }
        return valg;
    }

    private void oppdaterBehandlingsstatus(String saksnummer) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        Behandling behandling = fagsak.hentAktivBehandling();
        if (behandling != null && (behandling.getStatus() == Behandlingsstatus.SVAR_ANMODNING_MOTTATT || behandling.getStatus() == Behandlingsstatus.OPPRETTET)) {
            behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
            behandlingService.lagre(behandling);
        }
    }

    private void fjernOppgaverSomVenterForDokumentasjonEllerFagligAvklaring(List<Oppgave> oppgaver) {
        Iterator<Oppgave> iter = oppgaver.iterator();
        while (iter.hasNext()) {
            Oppgave oppgave = iter.next();
            String saksnummer = oppgave.getSaksnummer();
            Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
            if (fagsak == null) {
                log.error("Fant ikke fagsak {} for oppgave {}", saksnummer, oppgave.getOppgaveId());
                throw new TekniskException("Fant ikke fagsak " + saksnummer);
            }

            Behandling behandling = fagsak.hentSistAktivBehandling();
            if (behandling.erVenterForDokumentasjon()) {
                if (behandling.getDokumentasjonSvarfristDato() == null) {
                    log.error("Behandling {} tilhørende {} avventer dokumentasjon, men har ingen svarfristdato.", behandling.getId(), saksnummer);
                    iter.remove();
                } else if (behandling.getDokumentasjonSvarfristDato().isAfter(Instant.now())) {
                    iter.remove();
                }
            } else if (behandling.harStatus(Behandlingsstatus.AVVENT_FAGLIG_AVKLARING)) {
                iter.remove();
            }
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

    private Optional<Oppgave> velgNeste(String saksbehandlerID, List<Oppgave> oppgaver) {

        Optional<Oppgave> valg = oppgaver.stream().max(Oppgave.lavestTilHøyestPrioritet);
        if (valg.isPresent()) {
            String oppgaveId = valg.get().getOppgaveId();
            if (erTilbakeLagt(saksbehandlerID, oppgaveId)) {
                oppgaver.remove(valg.get());
                return velgNeste(saksbehandlerID, oppgaver);
            }
        }

        return valg;
    }

    private boolean erTilbakeLagt(String saksbehandlerID, String oppgaveId) {
        List<OppgaveTilbakelegging> tilbakelegging = oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(saksbehandlerID, oppgaveId);
        return !tilbakelegging.isEmpty();
    }

    private static void validerPlukkOppgave(PlukkOppgaveInnDto plukkDto) {
        if (StringUtils.isEmpty(plukkDto.getBehandlingstema())) {
            throw new FunksjonellException("Behandlingstema er påkrevd");
        }
    }
}
