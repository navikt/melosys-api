package no.nav.melosys.service.avklartefakta;

import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagSystemService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class AvklarteVirksomheterSystemService extends AvklarteVirksomheterService {

    public AvklarteVirksomheterSystemService(AvklartefaktaService avklartefaktaService,
                                             RegisterOppslagSystemService registerOppslagService,
                                             BehandlingService behandlingService,
                                             KodeverkService kodeverkService) {
        super(avklartefaktaService, registerOppslagService, behandlingService, kodeverkService);
    }
}
