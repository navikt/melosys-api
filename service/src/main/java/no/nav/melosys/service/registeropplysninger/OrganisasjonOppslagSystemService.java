package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class OrganisasjonOppslagSystemService extends OrganisasjonOppslagService {

    public OrganisasjonOppslagSystemService(@Qualifier("system") EregFasade eregFasade) {
        super(eregFasade);
    }
}
