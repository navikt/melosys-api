package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FtrlVedtakService {
    private static final Logger log = LoggerFactory.getLogger(FtrlVedtakService.class);

    @Autowired
    public FtrlVedtakService() {

    }

    public void fattVedtak(Behandling behandling, Behandlingsresultattyper behandlingsresultatTypeKode,
                           Vedtakstyper vedtakstype, String fritekstInnledning, String fritekstBegrunnelse) {

    }
}
