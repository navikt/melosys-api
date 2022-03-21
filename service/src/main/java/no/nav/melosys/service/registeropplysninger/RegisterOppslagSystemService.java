package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RegisterOppslagSystemService extends RegisterOppslagService {

    public RegisterOppslagSystemService(@Qualifier("system") EregFasade eregFasade,
                                        @Qualifier("system") PersondataFasade persondataFasade) {
        super(eregFasade, persondataFasade);
    }
}
