package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartInnstallasjonsType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;

public class BrevDataByggerInnvilgelse extends AbstraktDokumentDataBygger implements BrevDataBygger {
    private BrevDataByggerA1 a1Bygger;
    private BrevbestillingDto brevbestillingDto;

    public BrevDataByggerInnvilgelse(BrevDataByggerA1 a1bygger,
                                     AvklartefaktaService avklartefaktaService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     BrevbestillingDto brevbestillingDto) {
        super(null, lovvalgsperiodeService, avklartefaktaService);
        this.brevbestillingDto = brevbestillingDto;
        this.a1Bygger = a1bygger;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        this.behandling = behandling;

        BrevDataA1 vedleggA1 = (BrevDataA1) a1Bygger.lag(behandling, saksbehandler);
        BrevDataInnvilgelse brevData = new BrevDataInnvilgelse(saksbehandler, brevbestillingDto);
        brevData.lovvalgsperiode = hentLovvalgsperiode();
        brevData.vedleggA1 = vedleggA1;

        Optional<AvklartInnstallasjonsType> innstallasjonsType = avklartefaktaService.hentInnstallasjonsType(behandling.getId());
        innstallasjonsType.ifPresent(innstallasjon -> brevData.avklartSokkelEllerSkip = innstallasjon);

        return brevData;
    }
}
