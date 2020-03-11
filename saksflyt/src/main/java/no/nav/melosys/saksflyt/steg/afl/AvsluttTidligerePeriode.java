package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.springframework.stereotype.Component;

@Component("AFLAvsluttTidligerePeriode")
public class AvsluttTidligerePeriode extends AbstraktStegBehandler {

    private final MedlPeriodeService medlPeriodeService;

    public AvsluttTidligerePeriode(MedlPeriodeService medlPeriodeService) {
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_AVSLUTT_TIDLIGERE_PERIODE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {

        if (Boolean.TRUE.equals(prosessinstans.getData(ProsessDataKey.ER_OPPDATERT_SED, Boolean.class))) {
            medlPeriodeService.avsluttTidligerMedlPeriode(prosessinstans.getBehandling().getFagsak());
        }

        prosessinstans.setSteg(ProsessSteg.AFL_HENT_REGISTEROPPLYSNINGER);
    }
}
