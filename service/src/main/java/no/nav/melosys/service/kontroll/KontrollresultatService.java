package no.nav.melosys.service.kontroll;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Kontrollresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.KontrollresultatRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.ufm.UfmKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KontrollresultatService {
    private static final Logger log = LoggerFactory.getLogger(KontrollresultatService.class);

    private final KontrollresultatRepository kontrollresultatRepository;
    private final BehandlingsresultatService behandlingsresultatService;
    private final UfmKontrollService ufmKontrollService;
    private final BehandlingService behandlingService;

    @Autowired
    public KontrollresultatService(KontrollresultatRepository kontrollresultatRepository,
                                   BehandlingsresultatService behandlingsresultatService,
                                   UfmKontrollService ufmKontrollService,
                                   BehandlingService behandlingService,
                                   OppgaveService oppgaveService) {
        this.kontrollresultatRepository = kontrollresultatRepository;
        this.behandlingsresultatService = behandlingsresultatService;
        this.ufmKontrollService = ufmKontrollService;
        this.behandlingService = behandlingService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void utførKontrollerOgRegistrerFeil(long behandlingId) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        List<Kontroll_begrunnelser> registrerteTreff = ufmKontrollService.utførKontroller(behandling);

        log.info("Treff ved validering av periode for behandling {}: {}", behandlingId, registrerteTreff);
        lagreKontrollresultater(behandlingId, registrerteTreff);
    }

    private void lagreKontrollresultater(Long behandlingID, List<Kontroll_begrunnelser> kontrollBegrunnelser) throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        kontrollresultatRepository.deleteByBehandlingsresultat(behandlingsresultat);
        kontrollresultatRepository.flush();

        List<Kontrollresultat> kontrollresultater = kontrollBegrunnelser.stream()
            .map(kontrollBegrunnelse -> lagKontrollresultat(behandlingsresultat, kontrollBegrunnelse))
            .collect(Collectors.toList());

        kontrollresultatRepository.saveAll(kontrollresultater);
    }

    private Kontrollresultat lagKontrollresultat(Behandlingsresultat behandlingsresultat, Kontroll_begrunnelser kontrollBegrunnelse) {
        Kontrollresultat kontrollresultat = new Kontrollresultat();
        kontrollresultat.setBegrunnelse(kontrollBegrunnelse);
        kontrollresultat.setBehandlingsresultat(behandlingsresultat);

        return kontrollresultat;
    }
}
