package no.nav.melosys.service.dokument.sed;

import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import no.nav.melosys.service.eessi.SedGrunnlagMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class EessiSystemService extends EessiService {
    public EessiSystemService(SedDataBygger sedDataBygger,
                              SedDataGrunnlagFactory dataGrunnlagFactory,
                              @Qualifier("system") EessiConsumer eessiConsumer,
                              BehandlingService behandlingService,
                              BehandlingsresultatService behandlingsresultatService,
                              SedGrunnlagMapper sedGrunnlagMapper) {
        super(sedDataBygger, dataGrunnlagFactory, eessiConsumer, behandlingService, behandlingsresultatService, sedGrunnlagMapper);
    }
}
