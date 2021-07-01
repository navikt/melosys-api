package no.nav.melosys.service.persondata;

import no.finn.unleash.Unleash;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.tps.TpsSystemService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class PersondataSystemService extends PersondataService {
    @Autowired
    public PersondataSystemService(KodeverkService kodeverkService,
                                   @Qualifier("system") PDLConsumer pdlConsumer,
                                   TpsSystemService tpsSystemService,
                                   Unleash unleash) {
        super(kodeverkService, pdlConsumer, tpsSystemService, unleash);
    }
}
