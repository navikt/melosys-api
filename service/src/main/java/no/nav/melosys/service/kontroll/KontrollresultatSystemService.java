package no.nav.melosys.service.kontroll;

import no.nav.melosys.repository.KontrollresultatRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.ufm.UfmKontrollSystemService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class KontrollresultatSystemService extends KontrollresultatService {
    public KontrollresultatSystemService(KontrollresultatRepository kontrollresultatRepository,
                                         BehandlingsresultatService behandlingsresultatService,
                                         UfmKontrollSystemService ufmKontrollSystemService, BehandlingService behandlingService) {
        super(kontrollresultatRepository, behandlingsresultatService, ufmKontrollSystemService, behandlingService);
    }
}
