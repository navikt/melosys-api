package no.nav.melosys.saksflyt.steg.medl;

import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AvsluttTidligereMedlPeriode implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttTidligereMedlPeriode.class);

    private final MedlPeriodeService medlPeriodeService;

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
            medlPeriodeService.avsluttTidligerMedlPeriode(prosessinstans.getBehandling().getFagsak().getSaksnummer());
            log.info("Avsluttet tidligere medl-periode for behandling {}", prosessinstans.getBehandling().getId());
        }
    }
}
