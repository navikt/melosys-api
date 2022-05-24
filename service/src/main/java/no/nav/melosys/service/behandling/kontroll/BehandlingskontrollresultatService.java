package no.nav.melosys.service.behandling.kontroll;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Kontrollresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.repository.KontrollresultatRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.ufm.kontroll.UfmKontrollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
public class BehandlingskontrollresultatService {
    private static final Logger log = LoggerFactory.getLogger(BehandlingskontrollresultatService.class);

    private final KontrollresultatRepository kontrollresultatRepository;
    private final BehandlingsresultatService behandlingsresultatService;
    private final UfmKontrollService ufmKontrollService;
    private final BehandlingService behandlingService;

    public BehandlingskontrollresultatService(KontrollresultatRepository kontrollresultatRepository,
                                              BehandlingsresultatService behandlingsresultatService,
                                              UfmKontrollService ufmKontrollService,
                                              BehandlingService behandlingService) {
        this.kontrollresultatRepository = kontrollresultatRepository;
        this.behandlingsresultatService = behandlingsresultatService;
        this.ufmKontrollService = ufmKontrollService;
        this.behandlingService = behandlingService;
    }

    @Transactional
    public void utførKontrollerOgRegistrerFeil(long behandlingId) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        List<Kontroll_begrunnelser> registrerteTreff = ufmKontrollService.utførKontroller(behandling);

        log.info("Treff ved validering av periode for behandling {}: {}", behandlingId, registrerteTreff);
        lagreKontrollresultater(behandlingId, registrerteTreff);
    }

    private void lagreKontrollresultater(Long behandlingID, List<Kontroll_begrunnelser> kontrollBegrunnelser) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        kontrollresultatRepository.deleteByBehandlingsresultat(behandlingsresultat);
        kontrollresultatRepository.flush();

        List<Kontrollresultat> kontrollresultater = kontrollBegrunnelser.stream()
            .map(kontrollBegrunnelse -> lagKontrollresultat(behandlingsresultat, kontrollBegrunnelse))
            .toList();

        kontrollresultatRepository.saveAll(kontrollresultater);
    }

    private Kontrollresultat lagKontrollresultat(Behandlingsresultat behandlingsresultat, Kontroll_begrunnelser kontrollBegrunnelse) {
        Kontrollresultat kontrollresultat = new Kontrollresultat();
        kontrollresultat.setBegrunnelse(kontrollBegrunnelse);
        kontrollresultat.setBehandlingsresultat(behandlingsresultat);

        return kontrollresultat;
    }
}
