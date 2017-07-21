package no.nav.melosys.saksflyt.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.api.Binge;

@Component
public class InitBinge implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(InitBinge.class);

    private Binge binge;

    private BehandlingRepository behandlingRepo;

    @Autowired
    public InitBinge(Binge binge, BehandlingRepository behandlingRepo) {
        this.binge = binge;
        this.behandlingRepo = behandlingRepo;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        List<Behandling> uavsluttet = behandlingRepo.findByStatusNot(BehandlingStatus.AVSLUTTET);

        int teller = 0;

        for (Behandling behandling : uavsluttet) {
            if (binge.leggTil(behandling)) {
                teller++;
                log.debug("Behandling med behandlingsid {} er lagt i bingen", behandling.getBehandlingsId());
            }
        }

        log.info("{} behandling(er) er lagt i bingen", teller);
    }
}