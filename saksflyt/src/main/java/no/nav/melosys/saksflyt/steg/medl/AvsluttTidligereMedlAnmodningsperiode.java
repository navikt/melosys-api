package no.nav.melosys.saksflyt.steg.medl;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.medl.MedlAnmodningsperiodeService;
import org.springframework.stereotype.Component;

@Component
public class AvsluttTidligereMedlAnmodningsperiode implements StegBehandler {
    private final MedlAnmodningsperiodeService medlAnmodningsperiodeService;

    public AvsluttTidligereMedlAnmodningsperiode(MedlAnmodningsperiodeService medlAnmodningsperiodeService) {
        this.medlAnmodningsperiodeService = medlAnmodningsperiodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.AVSLUTT_TIDLIGERE_MEDL_ANMODNINGSPERIODE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        Behandling nyBehandling = prosessinstans.getBehandling();
        if (erOppdatertA001(nyBehandling, prosessinstans)) {
            medlAnmodningsperiodeService.avsluttTidligereAnmodningsperiode(nyBehandling);
        }
    }

    private boolean erOppdatertA001(Behandling behandling, Prosessinstans prosessinstans) {
        Boolean erOppdatertSed = prosessinstans.getData(ProsessDataKey.ER_OPPDATERT_SED, Boolean.class);
        return behandling.erAnmodningOmUnntak() && Boolean.TRUE.equals(erOppdatertSed);
    }
}
