package no.nav.melosys.saksflyt.steg.medl;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AvsluttTidligereMedlPeriode implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttTidligereMedlPeriode.class);

    private final MedlPeriodeService medlPeriodeService;

    @Autowired
    public AvsluttTidligereMedlPeriode(MedlPeriodeService medlPeriodeService) {
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.AVSLUTT_TIDLIGERE_MEDL_PERIODE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        if (Boolean.TRUE.equals(prosessinstans.getData(ProsessDataKey.ER_OPPDATERT_SED, Boolean.class))) {
            medlPeriodeService.avsluttTidligerMedlPeriode(prosessinstans.getBehandling().getFagsak());
            log.info("Avsluttet tidligere medl-periode for behandling {}", prosessinstans.getBehandling().getId());
        }
    }
}
