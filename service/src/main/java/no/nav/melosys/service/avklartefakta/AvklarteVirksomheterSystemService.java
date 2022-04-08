package no.nav.melosys.service.avklartefakta;

import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagSystemService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class AvklarteVirksomheterSystemService extends AvklarteVirksomheterService {

    public AvklarteVirksomheterSystemService(AvklartefaktaService avklartefaktaService,
                                             OrganisasjonOppslagSystemService registerOppslagService,
                                             BehandlingService behandlingService,
                                             KodeverkService kodeverkService) {
        super(avklartefaktaService, registerOppslagService, behandlingService, kodeverkService);
    }
}
