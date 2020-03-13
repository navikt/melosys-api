package no.nav.melosys.service.oppgave;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging;
import no.nav.melosys.domain.util.KodeverkUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import no.nav.melosys.service.oppgave.dto.TilbakeleggingDto;
import no.nav.melosys.service.sak.FagsakService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Oppgaveplukker {
    private static final Logger log =  LoggerFactory.getLogger(Oppgaveplukker.class);

    private final OppgaveFasade oppgaveFasade;
    private final OppgaveTilbakeleggingRepository oppgaveTilbakkeleggingRepo;
    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;

    @Autowired
    public Oppgaveplukker(OppgaveFasade oppgaveFasade, OppgaveTilbakeleggingRepository oppgaveTilbakeleggingRepo,
                          FagsakService fagsakService, BehandlingService behandlingService, OppgaveService oppgaveService) {
        this.oppgaveFasade = oppgaveFasade;
        this.oppgaveTilbakkeleggingRepo = oppgaveTilbakeleggingRepo;
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
    }

    /**
     * 1) Input valideres og behandlingstema og oppgavetyper beregnes.
     * 2) Oppgaveplukker henter i Oppgave en liste over alle aktive, ikke tildelte oppgaver med oppgitt parametre.
     * 3) Neste oppgave velges basert på prioritet (først) og frist.
     * 4) Oppgaven tildeles til saksbehandleren.
     */
    @Transactional(rollbackFor = MelosysException.class)
    public synchronized Optional<Oppgave> plukkOppgave(String saksbehandlerID, PlukkOppgaveInnDto plukkDto) throws FunksjonellException, TekniskException {
        validerPlukkOppgave(plukkDto);

        Behandlingstema behandlingstema = hentBehandlingstema(plukkDto.getSakstype());
        Behandlingstyper behandlingstype = KodeverkUtils.dekod(Behandlingstyper.class, plukkDto.getBehandlingstype());
        Set<Oppgavetyper> oppgavetyper = Collections.singleton(OppgaveFactory.hentOppgavetype(behandlingstype));

        List<Oppgave> ufordelteOppgaver = oppgaveFasade.finnUtildelteOppgaverEtterFrist(oppgavetyper, behandlingstype, behandlingstema);
        ufordelteOppgaver.addAll(hentOppgaverGammeltBehandlingstema(oppgavetyper, behandlingstype));

        fjernOppgaverSomVenterForDokumentasjon(ufordelteOppgaver);

        Optional<Oppgave> valg = velgNeste(saksbehandlerID, ufordelteOppgaver);

        if (valg.isPresent()) {
            // Tildeler oppgaven
            oppdaterBehandlingsstatus(valg.get().getSaksnummer());
            oppgaveService.tildelOppgave(valg.get().getOppgaveId(), saksbehandlerID);
        }
        return valg;
    }

    private void oppdaterBehandlingsstatus(String saksnummer) throws IkkeFunnetException, TekniskException {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        Behandling behandling = fagsak.getAktivBehandling();
        if (behandling != null && (behandling.getStatus() == Behandlingsstatus.SVAR_ANMODNING_MOTTATT || behandling.getStatus() == Behandlingsstatus.OPPRETTET)) {
            behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
            behandlingService.lagre(behandling);
        }
    }

    private Collection<Oppgave> hentOppgaverGammeltBehandlingstema(Set<Oppgavetyper> oppgavetyper, Behandlingstyper behandlingstype) throws FunksjonellException, TekniskException {
        if (oppgavetyper.contains(Oppgavetyper.BEH_SAK_MK) && behandlingstype == Behandlingstyper.SOEKNAD) {
            return hentOppgaverGammeltBehandlingstema(Oppgavetyper.BEH_SAK_MK);
        } else if (oppgavetyper.contains(Oppgavetyper.VUR) && behandlingstype == Behandlingstyper.ENDRET_PERIODE) {
            return hentOppgaverGammeltBehandlingstema(Oppgavetyper.VUR);
        } else {
            return Collections.emptySet();
        }
    }

    private Collection<Oppgave> hentOppgaverGammeltBehandlingstema(Oppgavetyper oppgavetype) throws FunksjonellException, TekniskException {
        //Byttet behandlingstema-kode for EU/EØS 4.10.2019. Må fortsatt kunne plukke med gammelt tema
        return oppgaveFasade.finnUtildelteOppgaverEtterFrist(Collections.singleton(oppgavetype),
            null, Behandlingstema.EU_EOS_GAMMEL_KODE);
    }

    private static Behandlingstema hentBehandlingstema(String sakstype) throws IkkeFunnetException {
        return Behandlingstema.valueOf(KodeverkUtils.dekod(Sakstyper.class, sakstype).name());
    }

    private void fjernOppgaverSomVenterForDokumentasjon(List<Oppgave> oppgaver) throws TekniskException, FunksjonellException {
        Iterator<Oppgave> iter = oppgaver.iterator();
        while (iter.hasNext()) {
            Oppgave oppgave = iter.next();
            String saksnummer = oppgave.getSaksnummer();
            Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
            if (fagsak == null) {
                log.error("Fant ikke fagsak {} for oppgave {}", saksnummer, oppgave.getOppgaveId());
                throw new TekniskException("Fant ikke fagsak " + saksnummer);
            }

            Behandling behandling = fagsak.hentSistAktiveBehandling();
            if (behandling.erVenterForDokumentasjon()) {
                if (behandling.getDokumentasjonSvarfristDato() == null) {
                    log.error("Behandling {} tilhørende {} avventer dokumentasjon, men har ingen svarfristdato.", behandling.getId(), saksnummer);
                    iter.remove();
                } else if (behandling.getDokumentasjonSvarfristDato().isAfter(Instant.now())) {
                    iter.remove();
                }
            }
        }
    }

    @Transactional(rollbackFor = MelosysException.class)
    public synchronized void leggTilbakeOppgave(String saksbehandlerID, TilbakeleggingDto tilbakelegging) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(tilbakelegging.getBehandlingID());

        Fagsak fagsak = behandling.getFagsak();
        Oppgave oppgave = oppgaveService.hentOppgaveMedFagsaksnummer(fagsak.getSaksnummer());

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
        // Vi må ikke tildele en oppgave som var tilbakelagt.
        if (valg.isPresent()) {
            String oppgaveId = valg.get().getOppgaveId();
            if (erTilbakeLagt(saksbehandlerID, oppgaveId)) {
                oppgaver.remove(valg.get());
                return velgNeste(saksbehandlerID, oppgaver);
            }
        }

        return valg;
    }

    // Sjekker tabellen for tilbakelegging.
    private boolean erTilbakeLagt(String saksbehandlerID, String oppgaveId) {
        List<OppgaveTilbakelegging> tilbakelegging = oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(saksbehandlerID, oppgaveId);
        return !tilbakelegging.isEmpty();
    }

    private static void validerPlukkOppgave(PlukkOppgaveInnDto plukkDto) throws FunksjonellException {
        if (StringUtils.isEmpty(plukkDto.getSakstype())) {
            throw new FunksjonellException("Sakstype er påkrevd");
        }
        if (StringUtils.isEmpty(plukkDto.getBehandlingstype())) {
            throw new FunksjonellException("Behandlingstype er påkrevd");
        }
    }
}
