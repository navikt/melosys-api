package no.nav.melosys.service;

import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RegisterOppslagSystemService extends RegisterOppslagService {

    @Autowired
    public RegisterOppslagSystemService(@Qualifier("system") EregFasade eregFasade, TpsFasade tpsFasade) {
        super(eregFasade, tpsFasade);
    }
}
