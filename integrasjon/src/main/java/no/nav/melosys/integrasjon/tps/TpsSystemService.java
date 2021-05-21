package no.nav.melosys.integrasjon.tps;

import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.integrasjon.tps.person.PersonConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class TpsSystemService extends TpsService {

    @Autowired
    public TpsSystemService(@Qualifier("system") PersonConsumer personConsumer,
                            DokumentFactory dokumentFactory) {
        super(personConsumer, dokumentFactory);
    }
}
