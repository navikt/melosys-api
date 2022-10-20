package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.util.Land_ISO2;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataVideresend;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;

public class BrevDataByggerVideresend implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final BrevbestillingRequest brevbestillingRequest;

    public BrevDataByggerVideresend(LandvelgerService landvelgerService,
                                    UtenlandskMyndighetService utenlandskMyndighetService,
                                    BrevbestillingRequest brevbestillingRequest) {
        this.landvelgerService = landvelgerService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.brevbestillingRequest = brevbestillingRequest;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        long behandlingID = dataGrunnlag.getBehandling().getId();

        Land_ISO2 bostedsland = Land_ISO2.valueOf(landvelgerService.hentBostedsland(behandlingID, dataGrunnlag.getBehandlingsgrunnlagData()).landkode());
        if (bostedsland == Land_ISO2.NO) {
            throw new FunksjonellException("Bostedslandet kan ikke være Norge ved videresending av søknad");
        }

        BrevDataVideresend brevdata = new BrevDataVideresend(brevbestillingRequest, saksbehandler);
        brevdata.bostedsland = bostedsland.getBeskrivelse();
        brevdata.trygdemyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(bostedsland);

        return brevdata;
    }
}
