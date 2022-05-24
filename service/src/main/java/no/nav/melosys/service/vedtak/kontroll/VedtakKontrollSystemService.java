package no.nav.melosys.service.vedtak.kontroll;

import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class VedtakKontrollSystemService extends VedtakKontrollService {
    public VedtakKontrollSystemService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                                       LovvalgsperiodeService lovvalgsperiodeService,
                                       @Qualifier("system") PersondataFasade persondataFasade, RegisteropplysningerService registeropplysningerService) {
        super(behandlingService, behandlingsresultatService, lovvalgsperiodeService, persondataFasade, registeropplysningerService);
    }
}
