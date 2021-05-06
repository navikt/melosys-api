package no.nav.melosys.saksflyt.steg.hendelse;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.vedtak.VedtakServiceFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PubliserVedtak implements StegBehandler {

    private final VedtakServiceFasade vedtakServiceFasade;

    @Autowired
    public PubliserVedtak(VedtakServiceFasade vedtakServiceFasade) {
        this.vedtakServiceFasade = vedtakServiceFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.PUBLISER_VEDTAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        vedtakServiceFasade.publiserFattetVedtak(prosessinstans.getBehandling().getId());
    }
}
