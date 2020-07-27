package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.springframework.stereotype.Component;

@Component("AFLAvsluttTidligerePeriode")
public class AvsluttTidligerePeriode implements StegBehandler {

    private final MedlPeriodeService medlPeriodeService;

    public AvsluttTidligerePeriode(MedlPeriodeService medlPeriodeService) {
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_AVSLUTT_TIDLIGERE_PERIODE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {

        if (Boolean.TRUE.equals(prosessinstans.getData(ProsessDataKey.ER_OPPDATERT_SED, Boolean.class))) {
            medlPeriodeService.avsluttTidligerMedlPeriode(prosessinstans.getBehandling().getFagsak());
        }

        prosessinstans.setSteg(ProsessSteg.AFL_HENT_REGISTEROPPLYSNINGER);
    }
}
