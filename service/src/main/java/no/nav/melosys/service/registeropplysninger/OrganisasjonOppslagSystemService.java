package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.integrasjon.ereg.EregFasade;
import org.springframework.stereotype.Service;

@Service
public class OrganisasjonOppslagSystemService extends OrganisasjonOppslagService {

    public OrganisasjonOppslagSystemService(EregFasade eregFasade) {
        super(eregFasade);
    }
}
