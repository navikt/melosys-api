package no.nav.melosys.service;

import java.util.*;

import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingResultatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import no.nav.melosys.domain.*;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;

@Service
public class BehandlingService {

    private static final Logger log = LoggerFactory.getLogger(BehandlingService.class);

    private final ProsessinstansRepository prosessinstansRepository;

    private final BehandlingRepository behandlingRepository;

    private final BehandlingResultatRepository behandlingResultatRepository;

    @Autowired
    public BehandlingService(ProsessinstansRepository prosessinstansRepository,
                             BehandlingRepository behandlingRepository, BehandlingResultatRepository behandlingResultatRepository) {
        this.prosessinstansRepository = prosessinstansRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingResultatRepository = behandlingResultatRepository;
    }

    /***
     * Metoden sjekker om en behandling med ID {@code behandlingID} har en oppfrisking i gang.
     * Oppfrisking betyr å hente saksopplysninger på nytt for en gitt behandling.
     */
    public boolean harAktivOppfrisking(long behandlingID) {
        Optional<Prosessinstans> aktivProsessinstans = prosessinstansRepository.findByStegIsNotNullAndTypeAndBehandling_Id(ProsessType.OPPFRISKNING, behandlingID);
        if (aktivProsessinstans.isPresent()) {
            log.debug("Behandling {} er under oppfrisking.", behandlingID);
            return true;
        }
        log.debug("Behandling {} er ikke under oppfrisking", behandlingID);
        return false;
    }

    public Behandlingsresultat hentBehandlingresultat(long behandlingsid) {
        Optional<Behandlingsresultat> resultOpt = Optional.ofNullable(behandlingResultatRepository.findOne(behandlingsid));
        return resultOpt.orElseGet(() -> {
            Behandlingsresultat resultat = new Behandlingsresultat();
            Behandling behandling = behandlingRepository.findOne(behandlingsid);
            resultat.setBehandling(behandling);
            behandlingResultatRepository.save(resultat);
            return resultat;
        });
    }
}
