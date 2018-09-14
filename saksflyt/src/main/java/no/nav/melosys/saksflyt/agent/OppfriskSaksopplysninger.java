// FIXME: Må flyttes ned til relevant pakke
package no.nav.melosys.saksflyt.agent;

import java.time.LocalDateTime;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.datavarehus.BehandlingLagretEvent;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessSteg.OPPFRISK_SAKSOPPLYSNINGER;
import static no.nav.melosys.domain.ProsessSteg.OPPRETT_OPPGAVE;

/**
 * Oppdatere behandling med siste opplysninger hentet dato
 *
 * Transisjoner:
 * 1) ProsessType.OPPFRISKNING :
 * OPPFRISK_SAKSOPPLYSNINGER -> null siden oppfrisking av saksopplysning er ferdig og trenger ikke å opprette oppgaven
 * 2) Andre prosess typer
 * OPPFRISK_SAKSOPPLYSNINGER -> OPPRETT_OPPGAVE
 * 3) Ved exception ellers teknisk feil
 * OPPFRISK_SAKSOPPLYSNINGER -> FEILET_MASKINELT
 */
@Component
public class OppfriskSaksopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppfriskSaksopplysninger.class);

    private final BehandlingRepository behandlingRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public OppfriskSaksopplysninger(BehandlingRepository behandlingRepository, ApplicationEventPublisher applicationEventPublisher) {
        log.info("OppfriskSaksopplysninger initialisert");
        this.behandlingRepository = behandlingRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPFRISK_SAKSOPPLYSNINGER;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws SikkerhetsbegrensningException, FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        behandling.setSisteOpplysningerHentetDato(LocalDateTime.now());
        behandlingRepository.save(behandling);
        applicationEventPublisher.publishEvent(new BehandlingLagretEvent(behandling));

        if (prosessinstans.getType() == ProsessType.OPPFRISKNING) {
            prosessinstans.setSteg(null);
            log.info("Oppfrisking av saksopplysning er ferdig for prosessinstans {} for behandlingId {}", prosessinstans.getId(), behandling.getId());
            return;
        } else {
            prosessinstans.setSteg(OPPRETT_OPPGAVE);
        }
        log.info("Ferdigstilt Behandling {}", prosessinstans.getId());
    }
}
