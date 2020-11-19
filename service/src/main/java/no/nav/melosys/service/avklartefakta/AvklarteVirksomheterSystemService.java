package no.nav.melosys.service.avklartefakta;

import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class AvklarteVirksomheterSystemService extends AvklarteVirksomheterService {

    @Autowired
    public AvklarteVirksomheterSystemService(AvklartefaktaService avklartefaktaService,
                                             RegisterOppslagSystemService registerOppslagService,
                                             BehandlingService behandlingService) {
        super(avklartefaktaService, registerOppslagService, behandlingService);
    }
}
