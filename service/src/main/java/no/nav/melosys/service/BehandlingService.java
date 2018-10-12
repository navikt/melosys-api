package no.nav.melosys.service;

import java.util.*;

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

    @Autowired
    public BehandlingService(ProsessinstansRepository prosessinstansRepository,
                             BehandlingRepository behandlingRepository) {
        this.prosessinstansRepository = prosessinstansRepository;
        this.behandlingRepository = behandlingRepository;
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
}
