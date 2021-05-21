package no.nav.melosys.service.persondata;

import no.finn.unleash.Unleash;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.tps.TpsSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class PersondataSystemService extends PersondataService {
    @Autowired
    public PersondataSystemService(@Qualifier("system") PDLConsumer pdlConsumer,
                                   TpsSystemService tpsSystemService,
                                   Unleash unleash) {
        super(pdlConsumer, tpsSystemService, unleash);
    }
}
