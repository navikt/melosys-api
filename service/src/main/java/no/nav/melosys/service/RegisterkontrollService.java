package no.nav.melosys.service;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Registerkontroll;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.RegisterkontrollRepository;
import no.nav.melosys.service.kontroll.ufm.UfmKontrollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterkontrollService {
    private static final Logger log = LoggerFactory.getLogger(RegisterkontrollService.class);

    private final RegisterkontrollRepository registerkontrollRepository;
    private final BehandlingsresultatService behandlingsresultatService;
    private final UfmKontrollService ufmKontrollService;
    private final BehandlingService behandlingService;

    @Autowired
    public RegisterkontrollService(RegisterkontrollRepository registerkontrollRepository,
                                   BehandlingsresultatService behandlingsresultatService,
                                   UfmKontrollService ufmKontrollService,
                                   BehandlingService behandlingService) {
        this.registerkontrollRepository = registerkontrollRepository;
        this.behandlingsresultatService = behandlingsresultatService;
        this.ufmKontrollService = ufmKontrollService;
        this.behandlingService = behandlingService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void utførKontrollerOgRegistrerFeil(long behandlingId) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        List<Kontroll_begrunnelser> registrerteTreff = ufmKontrollService.utførKontroller(behandling);

        log.info("Treff ved validering av periode for behandling {}. Treffbegrunnelse: {}", behandlingId, registrerteTreff);
        lagreRegisterkontroller(behandlingId, registrerteTreff);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void lagreRegisterkontroller(Long behandlingID, List<Kontroll_begrunnelser> kontrollBegrunnelser) throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        registerkontrollRepository.deleteByBehandlingsresultat(behandlingsresultat);
        registerkontrollRepository.flush();

        List<Registerkontroll> registerkontroller = kontrollBegrunnelser.stream()
            .map(kontrollBegrunnelse -> lagRegisterkontroll(behandlingsresultat, kontrollBegrunnelse))
            .collect(Collectors.toList());

        registerkontrollRepository.saveAll(registerkontroller);
    }

    private Registerkontroll lagRegisterkontroll(Behandlingsresultat behandlingsresultat, Kontroll_begrunnelser kontrollBegrunnelse) {
        Registerkontroll registerkontroll = new Registerkontroll();
        registerkontroll.setBegrunnelse(kontrollBegrunnelse);
        registerkontroll.setBehandlingsresultat(behandlingsresultat);

        return registerkontroll;
    }
}
