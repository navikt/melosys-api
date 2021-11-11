package no.nav.melosys.service.kontroll.ufm;

import no.finn.unleash.Unleash;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class UfmKontrollSystemService extends UfmKontrollService {
    public UfmKontrollSystemService(KontrollFactory kontrollFactory, @Qualifier("system") PersondataFasade persondataFasade,
                                    Unleash unleash) {
        super(kontrollFactory, persondataFasade, unleash);
    }
}
