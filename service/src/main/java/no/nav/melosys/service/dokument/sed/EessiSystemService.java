package no.nav.melosys.service.dokument.sed;

import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class EessiSystemService extends EessiService {
    public EessiSystemService(@Value("${MelosysEessi.forsokSendSed:true}") String skalSendeSed,
                              SedDataBygger sedDataBygger,
                              SedDataGrunnlagFactory dataGrunnlagFactory,
                              @Qualifier("system") EessiConsumer eessiConsumer,
                              BehandlingService behandlingService,
                              BehandlingsresultatService behandlingsresultatService) {
        super(skalSendeSed, sedDataBygger, dataGrunnlagFactory, eessiConsumer, behandlingService, behandlingsresultatService);
    }
}
