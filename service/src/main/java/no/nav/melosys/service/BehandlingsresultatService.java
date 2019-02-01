package no.nav.melosys.service;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehandlingsresultatService {

    private static final Logger log = LoggerFactory.getLogger(BehandlingsresultatService.class);

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    public BehandlingsresultatService(BehandlingsresultatRepository behandlingsresultatRepository) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    @Transactional
    public void tømBehandlingsresultat(long behandlingsid) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandlingsid).orElse(null);
        if (behandlingsresultat != null) {
            log.info("Fjerner avklarte fakta, lovvalgsperioder og vilkårsresultater fra behandlingsresultat med behandlingsid: {} ", behandlingsid);
            behandlingsresultat.getAvklartefakta().clear();
            behandlingsresultat.getLovvalgsperioder().clear();
            behandlingsresultat.getVilkaarsresultater().clear();
            behandlingsresultatRepository.save(behandlingsresultat);
        }
    }

    public Behandlingsresultat hentBehandlingsresultat(long behandlingsid) throws IkkeFunnetException {
        return behandlingsresultatRepository.findById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException("Kan ikke finne behandlingsresultat for behandling: " + behandlingsid));
    }
}
