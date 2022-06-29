package no.nav.melosys.service.kontroll.feature.ferdigbehandling.system;

import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class FerdigbehandlingKontrollSystemService extends FerdigbehandlingKontrollService {
    public FerdigbehandlingKontrollSystemService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                                                 LovvalgsperiodeService lovvalgsperiodeService,
                                                 PersondataFasade persondataFasade, RegisteropplysningerService registeropplysningerService) {
        super(behandlingService, behandlingsresultatService, lovvalgsperiodeService, persondataFasade, registeropplysningerService);
    }
}
