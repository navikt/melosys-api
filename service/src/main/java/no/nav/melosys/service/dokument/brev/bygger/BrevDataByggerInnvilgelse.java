package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartInnstallasjonsType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.*;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class BrevDataByggerInnvilgelse extends AbstraktDokumentDataBygger implements BrevDataBygger {
    private BrevDataByggerA1 a1Bygger;
    private BrevbestillingDto brevbestillingDto;

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     AvklarteVirksomheterService avklarteVirksomheterService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     KodeverkService kodeverkService,
                                     BrevbestillingDto brevbestillingDto) {
        super(kodeverkService, lovvalgsperiodeService, avklartefaktaService);
        this.brevbestillingDto = brevbestillingDto;

        a1Bygger = new BrevDataByggerA1(avklartefaktaService, avklarteVirksomheterService, kodeverkService);
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
