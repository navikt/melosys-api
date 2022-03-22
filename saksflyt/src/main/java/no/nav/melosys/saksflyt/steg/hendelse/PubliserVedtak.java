package no.nav.melosys.saksflyt.steg.hendelse;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.vedtak.publisering.FattetVedtakService;
import org.springframework.stereotype.Component;

@Component
public class PubliserVedtak implements StegBehandler {

    private final FattetVedtakService fattetVedtakService;

    public PubliserVedtak(FattetVedtakService fattetVedtakService) {
        this.fattetVedtakService = fattetVedtakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.PUBLISER_VEDTAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        fattetVedtakService.publiserFattetVedtak(prosessinstans.getBehandling().getId());
    }
}
