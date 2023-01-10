package no.nav.melosys.saksflyt.steg.medl;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.medl.MedlAnmodningsperiodeService;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessType.ANMODNING_OM_UNNTAK;

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
        if (nyBehandling.erNyVurdering() && ANMODNING_OM_UNNTAK == prosessinstans.getType()) {
            medlAnmodningsperiodeService.avsluttTidligereSendtAnmodningPeriode(nyBehandling);
        }
    }

    private boolean erOppdatertA001(Behandling behandling, Prosessinstans prosessinstans) {
        Boolean erOppdatertSed = prosessinstans.getData(ProsessDataKey.ER_OPPDATERT_SED, Boolean.class);
        return behandling.erAnmodningOmUnntak() && Boolean.TRUE.equals(erOppdatertSed);
    }
}
