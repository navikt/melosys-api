package no.nav.melosys.saksflyt.steg.reg;

import java.time.Instant;
import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.GSAK_OPPRETT_OPPGAVE;
import static no.nav.melosys.domain.ProsessSteg.OPPFRISK_SAKSOPPLYSNINGER;

/**
 * Oppdatere behandling med siste opplysninger hentet dato
 *
 * Transisjoner:
 * 1) ProsessType.OPPFRISKNING :
 * OPPFRISK_SAKSOPPLYSNINGER -> null siden oppfrisking av saksopplysning er ferdig og trenger ikke å opprette oppgaven
 * 2) Andre prosess typer
 * OPPFRISK_SAKSOPPLYSNINGER -> GSAK_OPPRETT_OPPGAVE
 * 3) Ved exception ellers teknisk feil
 * OPPFRISK_SAKSOPPLYSNINGER -> FEILET_MASKINELT
 */
@Component
public class OppfriskSaksopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppfriskSaksopplysninger.class);

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public OppfriskSaksopplysninger(BehandlingRepository behandlingRepository) {
        log.info("OppfriskSaksopplysninger initialisert");
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPFRISK_SAKSOPPLYSNINGER;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        behandling.setSisteOpplysningerHentetDato(Instant.now());
        behandlingRepository.save(behandling);

        if (prosessinstans.getType() == ProsessType.OPPFRISKNING) {
            prosessinstans.setSteg(ProsessSteg.FERDIG);
            log.info("Oppfrisking av saksopplysninger er ferdig for prosessinstans {} og behandlingID {}.", prosessinstans.getId(), behandling.getId());
            return;
        } else {
            prosessinstans.setSteg(GSAK_OPPRETT_OPPGAVE);
        }

        log.debug("Prosessinstans {} oppdatert behandling {} med sisteOpplysningerHentetDato.", prosessinstans.getId(), behandling.getId());
    }
}
