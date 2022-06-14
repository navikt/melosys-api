package no.nav.melosys.service.kontroll.feature.godkjennunntak.system;

import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.feature.godkjennunntak.GodkjennUnntakKontrollService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class GodkjennUnntakKontrollSystemService extends GodkjennUnntakKontrollService {
    public GodkjennUnntakKontrollSystemService(BehandlingService behandlingService) {
        super(behandlingService);
    }
}
