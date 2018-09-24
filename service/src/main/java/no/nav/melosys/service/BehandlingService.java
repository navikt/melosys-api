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

    public boolean aktivProsessinstansEksistererFor(long behandlingsid) {
        Optional<Prosessinstans> aktivProsessinstans = prosessinstansRepository.findByStegIsNotNullAndBehandling_Id(behandlingsid);
        if (aktivProsessinstans.isPresent()) {
            return true;
        }
        return false;
    }
}
