package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataVideresend;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;

public class BrevDataByggerVideresend implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final BrevbestillingDto brevbestillingDto;

    public BrevDataByggerVideresend(LandvelgerService landvelgerService,
                                    BrevbestillingDto brevbestillingDto) {
        this.landvelgerService = landvelgerService;
        this.brevbestillingDto = brevbestillingDto;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException, TekniskException {
        long behandlingID = dataGrunnlag.getBehandling().getId();

        Landkoder bostedsland = landvelgerService.hentBostedsland(behandlingID, dataGrunnlag.getSøknad());
        if (bostedsland == Landkoder.NO) {
            throw new FunksjonellException("Bostedslandet kan ikke være Norge ved videresending av søknad");
        }

        BrevDataVideresend brevdata = new BrevDataVideresend(brevbestillingDto, saksbehandler);
        brevdata.bostedsland = bostedsland.getBeskrivelse();
        brevdata.trygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID).stream()
            .findFirst()
            .map(Landkoder::getBeskrivelse)
            .orElseThrow(() -> new FunksjonellException("Kan ikke avgjøre trygdemyndighetsland i videresending av søknad"));

        return brevdata;
    }
}