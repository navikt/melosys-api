package no.nav.melosys.saksflyt.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;

@Component
public class InitBinge implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(InitBinge.class);

    private Binge binge;

    private ProsessinstansRepository prosessinstansRepo;

    @Autowired
    public InitBinge(Binge binge, ProsessinstansRepository prosessinstansRepo) {
        this.binge = binge;
        this.prosessinstansRepo = prosessinstansRepo;
    }

    @Override
    public void afterPropertiesSet() {

        Iterable<Prosessinstans> alleProsessinstanser = prosessinstansRepo.findAll();

        int teller = 0;

        for (Prosessinstans pi : alleProsessinstanser) {
            if (binge.leggTil(pi)) {
                teller++;
                log.debug("Prosessinstans med id {} er lagt i bingen", pi.getId());
            } else {
                assert false;
            }
        }

        log.info("{} prosessinstans(er) er lagt i bingen", teller);
    }
}