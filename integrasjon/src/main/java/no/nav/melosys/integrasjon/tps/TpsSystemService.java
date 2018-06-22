package no.nav.melosys.integrasjon.tps;

import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.integrasjon.tps.aktoer.AktorConsumer;
import no.nav.melosys.integrasjon.tps.person.PersonConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class TpsSystemService extends TpsService implements TpsFasade {

    @Autowired
    public TpsSystemService(AktorConsumer aktorConsumer, @Qualifier("system")PersonConsumer personConsumer, DokumentFactory dokumentFactory) {
        super(aktorConsumer, personConsumer, dokumentFactory);
    }
}
