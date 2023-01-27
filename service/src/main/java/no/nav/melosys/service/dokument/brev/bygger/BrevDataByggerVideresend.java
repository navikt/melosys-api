package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataVideresend;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;

public class BrevDataByggerVideresend implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final BrevbestillingDto brevbestillingDto;

    public BrevDataByggerVideresend(LandvelgerService landvelgerService,
                                    UtenlandskMyndighetService utenlandskMyndighetService,
                                    BrevbestillingDto brevbestillingDto) {
        this.landvelgerService = landvelgerService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.brevbestillingDto = brevbestillingDto;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        long behandlingID = dataGrunnlag.getBehandling().getId();

        Land_iso2 bostedsland = Land_iso2.valueOf(landvelgerService.hentBostedsland(behandlingID, dataGrunnlag.getMottatteOpplysningerData()).landkode());
        if (bostedsland == Land_iso2.NO) {
            throw new FunksjonellException("Bostedslandet kan ikke være Norge ved videresending av søknad");
        }

        BrevDataVideresend brevdata = new BrevDataVideresend(brevbestillingDto, saksbehandler);
        brevdata.bostedsland = bostedsland.getBeskrivelse();
        brevdata.trygdemyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(bostedsland);

        return brevdata;
    }
}
