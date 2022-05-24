package no.nav.melosys.service.behandling;

import no.nav.melosys.repository.KontrollresultatRepository;
import no.nav.melosys.service.ufm.kontroll.UfmKontrollSystemService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class BehandlingskontrollresultatSystemService extends BehandlingskontrollresultatService {
    public BehandlingskontrollresultatSystemService(KontrollresultatRepository kontrollresultatRepository,
                                                    BehandlingsresultatService behandlingsresultatService,
                                                    UfmKontrollSystemService ufmKontrollSystemService, BehandlingService behandlingService) {
        super(kontrollresultatRepository, behandlingsresultatService, ufmKontrollSystemService, behandlingService);
    }
}
