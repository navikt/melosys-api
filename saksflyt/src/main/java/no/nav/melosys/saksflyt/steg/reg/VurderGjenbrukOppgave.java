package no.nav.melosys.saksflyt.steg.reg;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;

public class VurderGjenbrukOppgave implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(VurderGjenbrukOppgave.class);

    @Override
    public ProsessSteg inngangsSteg() {
        return VURDER_GJENBRUK_OPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        if (prosessinstans.getType() == ProsessType.OPPRETT_NY_SAK) {
            prosessinstans.setSteg(GJENBRUK_OPPGAVE);
        } else {
            prosessinstans.setSteg(GSAK_OPPRETT_OPPGAVE);
        }

        log.debug("Vurdert gjenbruk av oppgave for prosessinstans {}", prosessinstans.getId());
    }
}
