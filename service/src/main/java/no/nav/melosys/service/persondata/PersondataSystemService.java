package no.nav.melosys.service.persondata;

import no.nav.melosys.integrasjon.tps.TpsSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class PersondataSystemService extends PersondataService {
    @Autowired
    public PersondataSystemService(TpsSystemService tpsSystemService) {
        super(tpsSystemService);
    }
}
