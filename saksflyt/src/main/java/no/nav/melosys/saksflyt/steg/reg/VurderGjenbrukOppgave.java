package no.nav.melosys.saksflyt.steg.reg;

import java.time.Instant;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;

@Component
public class VurderGjenbrukOppgave implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(VurderGjenbrukOppgave.class);

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public VurderGjenbrukOppgave(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return VURDER_GJENBRUK_OPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        behandling.setSisteOpplysningerHentetDato(Instant.now());
        behandlingRepository.save(behandling);

        if (prosessinstans.getType() == ProsessType.OPPRETT_NY_SAK) {
            prosessinstans.setSteg(GJENBRUK_OPPGAVE);
        } else {
            prosessinstans.setSteg(GSAK_OPPRETT_OPPGAVE);
        }

        log.debug("Vurdert gjenbruk av oppgave for prosessinstans {}, behandling {}", prosessinstans.getId(), behandling.getId());
    }
}
