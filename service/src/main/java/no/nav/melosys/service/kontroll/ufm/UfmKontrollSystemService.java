package no.nav.melosys.service.kontroll.ufm;

import no.nav.melosys.repository.KontrollresultatRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class UfmKontrollSystemService extends UfmKontrollService {
    public UfmKontrollSystemService(KontrollresultatRepository kontrollresultatRepository,
                                    BehandlingsresultatService behandlingsresultatService,
                                    BehandlingService behandlingService,
                                    @Qualifier("system") PersondataFasade persondataFasade) {
        super(kontrollresultatRepository, behandlingsresultatService, behandlingService, persondataFasade);
    }
}
