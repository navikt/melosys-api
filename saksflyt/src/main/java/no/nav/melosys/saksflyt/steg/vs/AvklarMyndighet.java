package no.nav.melosys.saksflyt.steg.vs;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktAvklarMyndighet;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("VideresendSoknadAvklarMyndighet")
public class AvklarMyndighet extends AbstraktAvklarMyndighet {

    @Autowired
    public AvklarMyndighet(BehandlingService behandlingService,
                           BehandlingsresultatService behandlingsresultatService,
                           UtenlandskMyndighetService utenlandskMyndighetService) {
        super(behandlingService, behandlingsresultatService, utenlandskMyndighetService);
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.VS_AVKLAR_MYNDIGHET;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        super.utfør(prosessinstans);
        prosessinstans.setSteg(ProsessSteg.VS_SEND_ORIENTERINGSBREV);
    }
}
