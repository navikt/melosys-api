package no.nav.melosys.service.oppgave;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging;
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
        log.info("Begynner plukking av oppgave for saksbehandler med følgende kriterier: {}", plukkDto);
        List<Oppgave> utildelteOppgaver = hentUtildelteOppgaver(plukkDto);
        log.info("Funnet {} oppgaver ", utildelteOppgaver.size());

        List<Oppgave> filtrerteOppgaver = filtrerOppgaver(plukkDto, utildelteOppgaver);
        Optional<Oppgave> valg = filtrerteOppgaver.stream().max(Oppgave.LAVEST_TIL_HØYEST_PRIORITET);

        if (valg.isPresent()) {
            oppdaterBehandlingsstatus(valg.get().getSaksnummer());
            oppgaveService.tildelOppgave(valg.get().getOppgaveId(), saksbehandlerID);
            log.info("Oppgave {} ble plukket.", valg.get().getOppgaveId());
        } else {
            log.info("Ingen oppgave kunne plukkes med følgende kriterier {}", plukkDto);
        }
        return valg;
    }

    public List<Oppgave> hentUtildelteOppgaver(PlukkOppgaveInnDto plukkDto){
        List<Oppgave> utildelteOppgaver = new ArrayList<>();
        Set<String> oppgaveBehandlingstemaSet = hentAlleOppgaveBehandlingstemaTilSøk(plukkDto.sakstype(), plukkDto.sakstema(), plukkDto.behandlingstema());
        for (var oppgaveBehandlingstema : oppgaveBehandlingstemaSet) {
            utildelteOppgaver.addAll(oppgaveFasade.finnUtildelteOppgaverEtterFrist(oppgaveBehandlingstema));
        }
        return utildelteOppgaver;
    }

    private List<Oppgave> filtrerOppgaver(PlukkOppgaveInnDto plukkDto, List<Oppgave> utildelteOppgaver) {
        Set<String> saksnumre = utildelteOppgaver.stream().map(Oppgave::getSaksnummer).collect(Collectors.toSet());
        Map<String, Fagsak> sasksnummerFagsakMap = fagsakService.hentFagsaker(saksnumre).stream()
            .collect(Collectors.toMap(Fagsak::getSaksnummer, Function.identity()));

        int antallSakSomIkkeMatcherSøk = 0;
        int antallSakSomVenter = 0;
        List<Oppgave> filtrerteOppgaver = new ArrayList<>();
        for (Oppgave oppgave : utildelteOppgaver) {
            String saksnummer = oppgave.getSaksnummer();
            Fagsak fagsak = sasksnummerFagsakMap.get(saksnummer);
            if (fagsak == null) {
                log.warn("Fant ikke fagsak {} for oppgave {}", saksnummer, oppgave.getOppgaveId());
                continue;
            }
            boolean fagsakMatcherSøk = fagsakMatcherSøk(fagsak, plukkDto);
            boolean venterPåDokEllerAvklaring = venterPåDokumentasjonEllerFagligAvklaring(fagsak);
            if (!fagsakMatcherSøk) {
                antallSakSomIkkeMatcherSøk++;
            }
            if (venterPåDokEllerAvklaring) {
                antallSakSomVenter++;
            }

            if (fagsakMatcherSøk && !venterPåDokEllerAvklaring) {
                filtrerteOppgaver.add(oppgave);
            }
        }

        if (antallSakSomIkkeMatcherSøk > 0) {
            log.info("Antall sak som ikke matcher søk: {} / {}", antallSakSomIkkeMatcherSøk, saksnumre.size());
        }
        log.info("Antall sak som venter på dokumentasjon eller avklaring: {} / {}", antallSakSomVenter, saksnumre.size());
        return filtrerteOppgaver;
    }

    private boolean fagsakMatcherSøk(Fagsak fagsak, PlukkOppgaveInnDto plukkDto) {
        return fagsak != null && fagsak.getType() == plukkDto.sakstype()
            && fagsak.getTema() == plukkDto.sakstema()
            && fagsak.getBehandlinger().stream().anyMatch(behandling -> behandling.getTema() == plukkDto.behandlingstema());
    }

    private boolean venterPåDokumentasjonEllerFagligAvklaring(Fagsak fagsak) {
        if (fagsak == null) {
            return false;
        }
        Behandling behandling = fagsak.hentSistAktivBehandling();
        if (behandling.erVenterForDokumentasjon()) {
            if (behandling.getDokumentasjonSvarfristDato() == null) {
                log.warn("Behandling {} tilhørende {} avventer dokumentasjon, men har ingen svarfristdato.",
                    behandling.getId(), fagsak.getSaksnummer());
                return true;
            }
            return Instant.now().isBefore(behandling.getDokumentasjonSvarfristDato());
        }
        return behandling.harStatus(Behandlingsstatus.AVVENT_FAGLIG_AVKLARING);
    }

    private void oppdaterBehandlingsstatus(String saksnummer) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        Behandling behandling = fagsak.hentAktivBehandling();
        if (behandling != null && (behandling.getStatus() == Behandlingsstatus.SVAR_ANMODNING_MOTTATT || behandling.getStatus() == Behandlingsstatus.OPPRETTET)) {
            behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
            behandlingService.lagre(behandling);
        }
    }

    private Set<String> hentAlleOppgaveBehandlingstemaTilSøk(Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema) {
        return Arrays.stream(Behandlingstyper.values())
            .map(behandlingstype -> OppgaveFactory.utledBehandlingstema(sakstype, sakstema, behandlingstema, behandlingstype).getKode())
            .collect(Collectors.toSet());
    }

    @Transactional
    public synchronized void leggTilbakeOppgave(String saksbehandlerID, TilbakeleggingDto tilbakelegging) {
        Behandling behandling = behandlingService.hentBehandling(tilbakelegging.getBehandlingID());

        Fagsak fagsak = behandling.getFagsak();
        Oppgave oppgave = oppgaveService.hentÅpenBehandlingsoppgaveMedFagsaksnummer(fagsak.getSaksnummer());

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
