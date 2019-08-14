package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;

public class BrevDataByggerInnvilgelse extends AbstraktDokumentDataBygger implements BrevDataBygger {
    private final LandvelgerService landVelgerService;
    private final BrevbestillingDto brevbestillingDto;
    private final BrevDataByggerA1 brevbyggerA1;

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     LandvelgerService landVelgerService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     BrevbestillingDto brevbestillingDto) {
        super(null, lovvalgsperiodeService, avklartefaktaService);
        this.landVelgerService = landVelgerService;
        this.brevbestillingDto = brevbestillingDto;
        this.brevbyggerA1 = null;
    }

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     LandvelgerService landVelgerService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     BrevbestillingDto brevbestillingDto,
                                     BrevDataByggerA1 brevbyggerA1) {
        super(null, lovvalgsperiodeService, avklartefaktaService);
        this.landVelgerService = landVelgerService;
        this.brevbestillingDto = brevbestillingDto;
        this.brevbyggerA1 = brevbyggerA1;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        this.behandling = behandling;

        // Bruker skal ha A1 som vedlegg - Arbeidsgiver skal ikke
        BrevDataInnvilgelse brevdata;
        if (brevbyggerA1 != null) {
            brevdata = lagInnvilgelseBrevdataMedA1(behandling, saksbehandler);
        }
        else {
            brevdata = new BrevDataInnvilgelse(brevbestillingDto, saksbehandler);
        }

        brevdata.lovvalgsperiode = hentLovvalgsperiode();
        brevdata.arbeidsland = landVelgerService.hentArbeidsland(behandling).getBeskrivelse();

        brevdata.trygdemyndighetsland = landVelgerService.hentUtenlandskTrygdemyndighetsland(behandling).stream()
            .findFirst()
            .map(Landkoder::getBeskrivelse)
            .orElse(null);

        Optional<Maritimtyper> maritimType = avklartefaktaService.hentMaritimType(behandling.getId());
        maritimType.ifPresent(mt -> brevdata.avklartMaritimType = mt);

        return brevdata;
    }

    private BrevDataInnvilgelse lagInnvilgelseBrevdataMedA1(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataInnvilgelse brevdata = new BrevDataInnvilgelse(brevbestillingDto, saksbehandler);

        BrevDataA1 vedleggA1 = (BrevDataA1) brevbyggerA1.lag(behandling, saksbehandler);
        brevdata.vedleggA1 = vedleggA1;
        brevdata.norskeVirksomheter = vedleggA1.norskeVirksomheter;
        return brevdata;
    }
}