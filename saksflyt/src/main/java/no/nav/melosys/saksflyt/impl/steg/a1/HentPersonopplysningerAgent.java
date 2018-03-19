package no.nav.melosys.saksflyt.impl.steg.a1;

import static no.nav.melosys.domain.ProsessSteg.A1_HENT_PERS_OPPL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Agent;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.saksflyt.impl.Utils;

/**
 * Steget sørger for å hente personinfo fra TPS
 * 
 * Transisjoner: 
 * A1_HENT_PERS_OPPL → A1_HENT_ARBF_OPPL hvis alt ok
 * A1_HENT_PERS_OPPL → FEILET_MASKINELT hvis personen ikke finnes i TPS
 */
@Component
public class HentPersonopplysningerAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(HentPersonopplysningerAgent.class);

    private Binge binge;

    private BehandlingRepository behandlingRepo;

    private ProsessinstansRepository prosessinstansRepo;
    
    @Autowired
    public HentPersonopplysningerAgent(Binge binge, BehandlingRepository behandlingRepo, ProsessinstansRepository prosessinstansRepo) {
        this.binge = binge;
        this.behandlingRepo = behandlingRepo;
        this.prosessinstansRepo = prosessinstansRepo;
    }

    @Override
    public void finnProsessinstansOgUtfoerSteg() {
        Prosessinstans pi = binge.fjernFørsteProsessinstans(Utils.medSteg(A1_HENT_PERS_OPPL));
        if (pi == null) {
            // Ingenting å gjøre
            return;
        }
        // TODO: MELOSYS-46
    }

}
