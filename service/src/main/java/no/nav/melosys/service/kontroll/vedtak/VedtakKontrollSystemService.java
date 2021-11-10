package no.nav.melosys.service.kontroll.vedtak;

import no.finn.unleash.Unleash;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class VedtakKontrollSystemService extends VedtakKontrollService {
    public VedtakKontrollSystemService(BehandlingService behandlingService, LovvalgsperiodeService lovvalgsperiodeService,
                                       @Qualifier("system") PersondataFasade persondataFasade, Unleash unleash) {
        super(behandlingService, lovvalgsperiodeService, persondataFasade, unleash);
    }
}
