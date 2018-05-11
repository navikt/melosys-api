package no.nav.melosys.saksflyt.impl.agent;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.A1_HENT_PERS_OPPL;

/**
 * Steget sørger for å hente personinfo fra TPS
 * 
 * Transisjoner: 
 * A1_HENT_PERS_OPPL → A1_HENT_ARBF_OPPL hvis alt ok
 * A1_HENT_PERS_OPPL → FEILET_MASKINELT hvis personen ikke finnes i TPS
 */
@Component
public class HentPersonopplysninger extends StandardAbstraktAgent {

    private static final Logger log = LoggerFactory.getLogger(HentPersonopplysninger.class);

    public HentPersonopplysninger(Binge binge, ProsessinstansRepository prosessinstansRepo) {
        super(binge, prosessinstansRepo);
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return A1_HENT_PERS_OPPL;
    }

    @Override
    public void utfoerSteg(Prosessinstans prosessinstans) {
        // TODO: MELOSYS-46
    }
}
