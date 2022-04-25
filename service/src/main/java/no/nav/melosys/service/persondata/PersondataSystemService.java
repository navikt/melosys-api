package no.nav.melosys.service.persondata;

import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.detaljer.FamiliemedlemService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class PersondataSystemService extends PersondataService {
    public PersondataSystemService(BehandlingService behandlingService, KodeverkService kodeverkService, @Qualifier("system") PDLConsumer pdlConsumer,
                                   SaksopplysningerService saksopplysningerService, FamiliemedlemService familiemedlemService) {
        super(behandlingService, kodeverkService, pdlConsumer, saksopplysningerService, familiemedlemService);
    }
}
