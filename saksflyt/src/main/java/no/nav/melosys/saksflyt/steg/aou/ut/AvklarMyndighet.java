package no.nav.melosys.saksflyt.steg.aou.ut;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.saksflyt.steg.AbstraktAvklarMyndighet;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.AOU_AVKLAR_MYNDIGHET;

//TODO: skal slettes
@Component("AnmodningOmUnntakAvklarMyndighet")
public class AvklarMyndighet extends AbstraktAvklarMyndighet {

    @Autowired
    public AvklarMyndighet(BehandlingService behandlingService,
                           BehandlingsresultatService behandlingsresultatService,
                           UtenlandskMyndighetService utenlandskMyndighetService) {
        super(behandlingService, behandlingsresultatService,
            utenlandskMyndighetService);
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AOU_AVKLAR_MYNDIGHET;
    }
}