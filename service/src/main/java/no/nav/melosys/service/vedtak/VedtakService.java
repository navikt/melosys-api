package no.nav.melosys.service.vedtak;

import java.time.LocalDateTime;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VedtakService {

    private static final Logger log = LoggerFactory.getLogger(VedtakService.class);

    private final BehandlingRepository behandlingRepository;

    private final Binge binge;

    private final ProsessinstansRepository prosessinstansRepo;

    private final OppgaveService oppgaveService;


    @Autowired
    public VedtakService(BehandlingRepository behandlingRepository, Binge binge, ProsessinstansRepository prosessinstansRepo, OppgaveService oppgaveService) {
        this.behandlingRepository = behandlingRepository;
        this.binge = binge;
        this.prosessinstansRepo = prosessinstansRepo;
        this.oppgaveService = oppgaveService;
    }

    public void anmodningOmUnntak(long behandlingID) throws IkkeFunnetException {
        log.info("Anmodning om unntak for behandling: " + behandlingID);

        Behandling behandling = behandlingRepository.findOne(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Kan ikke fatte vedtak fordi behandling " + behandlingID + " ikke finnes.");
        }
        lagreProsessinstans(opprettProsessinstansAnmodningUnntak(), behandling);
    }

    private Prosessinstans opprettProsessinstansAnmodningUnntak() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.ANMODNING_UNNTAK);
        prosessinstans.setSteg(ProsessSteg.AU_VALIDERING);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, BehandlingsresultatType.ANMODNING_OM_UNNTAK);
        return prosessinstans;
    }

    private void lagreProsessinstans(Prosessinstans prosessinstans, Behandling behandling) {
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);

        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, SubjectHandler.getInstance().getUserID());

        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);
    }

    @Transactional
    public void fattVedtak(long behandlingID, BehandlingsresultatType behandlingsresultatType) throws FunksjonellException, TekniskException {
        log.info("Fatter vedtak for behandling: " + behandlingID);

        Behandling behandling = behandlingRepository.findOne(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Kan ikke fatte vedtak fordi behandling " + behandlingID + " ikke finnes.");
        }

        Prosessinstans prosessinstans = opprettProsessinstansIverksettVedtak(behandlingsresultatType);
        lagreProsessinstans(prosessinstans, behandling);

        avsluttBehandlingsoppgave(behandling.getFagsak().getSaksnummer());
    }

    private Prosessinstans opprettProsessinstansIverksettVedtak(BehandlingsresultatType behandlingsresultatType) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK);
        prosessinstans.setSteg(ProsessSteg.IV_VALIDERING);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, behandlingsresultatType.getKode());
        return prosessinstans;
    }

    private void avsluttBehandlingsoppgave(String fagSaksnummer) throws FunksjonellException, TekniskException {
        Oppgave oppgave = oppgaveService.hentOppgaveMedFagSaksnummer(fagSaksnummer);

        oppgaveService.ferdigstillOppgave(oppgave.getOppgaveId());
    }
}
