package no.nav.melosys.saksflyt.steg.medl;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AvsluttTidligereAnmodningUnntaksperiode implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttTidligereAnmodningUnntaksperiode.class);

    private final MedlPeriodeService medlPeriodeService;

    public AvsluttTidligereAnmodningUnntaksperiode(MedlPeriodeService medlPeriodeService) {
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.AVSLUTT_TIDLIGERE_ANMODNINGSPERIODE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        log.info("AvsluttTidligereAnmodningUnntaksperiode: start");
        Behandling behandling = prosessinstans.getBehandling();
        if (erOppdatertA001(behandling, prosessinstans)) {
            medlPeriodeService.avsluttTidligereAnmodningsperiode(behandling);
            log.info("Avsluttet tidligere periode for behandlingID {}", behandling.getId());
        }
        log.info("AvsluttTidligereAnmodningUnntaksperiode: slutt");
    }

    private boolean erOppdatertA001(Behandling behandling, Prosessinstans prosessinstans) {
        Boolean erOppdatertSed = prosessinstans.getData(ProsessDataKey.ER_OPPDATERT_SED, Boolean.class);
        return behandling.erAnmodningOmUnntak() && Boolean.TRUE.equals(erOppdatertSed);
    }
}
