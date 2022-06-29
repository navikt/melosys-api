package no.nav.melosys.service.dokument.sed;

import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class EessiSystemService extends EessiService {
    public EessiSystemService(SedDataBygger sedDataBygger,
                              SedDataGrunnlagFactory dataGrunnlagFactory,
                              EessiConsumer eessiConsumer,
                              JoarkFasade joarkFasade,
                              BehandlingService behandlingService,
                              BehandlingsresultatService behandlingsresultatService) {
        super(behandlingService, behandlingsresultatService, eessiConsumer, joarkFasade, sedDataBygger,
            dataGrunnlagFactory);
    }
}
