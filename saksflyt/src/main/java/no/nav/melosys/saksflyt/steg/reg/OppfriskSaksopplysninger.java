package no.nav.melosys.saksflyt.steg.reg;

import java.time.Instant;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;

/**
 * Oppdaterer behandling med siste opplysninger hentet dato.
 */
@Component
public class OppfriskSaksopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppfriskSaksopplysninger.class);

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public OppfriskSaksopplysninger(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPFRISK_SAKSOPPLYSNINGER;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        behandling.setSisteOpplysningerHentetDato(Instant.now());
        behandlingRepository.save(behandling);

        // FIXME: Fjern sjekk og prosesstype, og endre navn på klasse eller flytt resterende kode
        if (prosessinstans.getType() == ProsessType.OPPFRISKNING) {
            prosessinstans.setSteg(ProsessSteg.FERDIG);
            log.info("Oppfrisking av saksopplysninger er ferdig for prosessinstans {} og behandlingID {}.", prosessinstans.getId(), behandling.getId());
            return;
        } else if (prosessinstans.getType() == ProsessType.OPPRETT_NY_SAK) {
            prosessinstans.setSteg(GJENBRUK_OPPGAVE);
        } else {
            prosessinstans.setSteg(GSAK_OPPRETT_OPPGAVE);
        }

        log.debug("Prosessinstans {} oppdatert behandling {} med sisteOpplysningerHentetDato.", prosessinstans.getId(), behandling.getId());
    }
}
