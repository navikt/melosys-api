package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsstatus;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Oppdaterer behandlingsstatus i Melosys.
 *
 * Transisjoner:
 * JFR_OPPDATER_BEHANDLINGSSTATUS → null hvis alt ok
 * JFR_OPPDATER_BEHANDLINGSSTATUS → FEILET_MASKINELT hvis feil
 */
@Component
public class OppdaterBehandlingsstatus extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandlingsstatus.class);

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public OppdaterBehandlingsstatus(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
        log.info("OppdaterBehandlingsstatus initialisert");
    }


    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_OPPDATER_BEHANDLINGSSTATUS;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();

        if (behandling.getStatus() == Behandlingsstatus.AVVENT_DOK_PART) {
            behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
            behandlingRepository.save(behandling);
            log.info("Prosessinstans {} har oppdatert behandling {}", prosessinstans.getId(), behandling.getId());
        }

        prosessinstans.setSteg(null);
    }
}
