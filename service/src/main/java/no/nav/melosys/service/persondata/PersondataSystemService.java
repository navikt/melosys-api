package no.nav.melosys.service.persondata;

import no.finn.unleash.Unleash;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.tps.TpsSystemService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class PersondataSystemService extends PersondataService {
    public PersondataSystemService(BehandlingService behandlingService,
                                   BehandlingsresultatService behandlingsresultatService,
                                   KodeverkService kodeverkService,
                                   @Qualifier("system") PDLConsumer pdlConsumer,
                                   SaksopplysningerService saksopplysningerService,
                                   TpsSystemService tpsSystemService,
                                   Unleash unleash) {
        super(behandlingService, kodeverkService, pdlConsumer, saksopplysningerService, tpsSystemService, unleash);
    }
}
