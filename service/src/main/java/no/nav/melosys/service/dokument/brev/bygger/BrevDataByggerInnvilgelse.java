package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.arbeidsforhold.Fartsomraade;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
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
    private LandvelgerService landVelgerService;
    private BrevbestillingDto brevbestillingDto;
    private BrevDataByggerA1 brevbyggerA1;

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     LandvelgerService landVelgerService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     BrevbestillingDto brevbestillingDto) {
        super(null, lovvalgsperiodeService, avklartefaktaService);
        this.landVelgerService = landVelgerService;
        this.brevbestillingDto = brevbestillingDto;
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
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);

        // Bruker skal ha A1 som vedlegg - Arbeidsgiver skal ikke
        BrevDataInnvilgelse brevdata;
        if (brevbyggerA1 != null) {
            brevdata = lagInnvilgelseBrevdataMedA1(behandling, saksbehandler);
        }
        else {
            brevdata = new BrevDataInnvilgelse(saksbehandler, brevbestillingDto);
        }

        brevdata.lovvalgsperiode = hentLovvalgsperiode();
        brevdata.alleArbeidsland = landVelgerService.hentAlleArbeidsland(behandling).stream()
            .map(Landkoder::getBeskrivelse)
            .collect(Collectors.toList());

        brevdata.arbeidsland = brevdata.alleArbeidsland.iterator().next();

        brevdata.trygdemyndighetsland = landVelgerService.hentTrygdemyndighetsland(behandling).getBeskrivelse();

        Optional<Maritimtyper> maritimType = avklartefaktaService.hentMaritimType(behandling.getId());
        maritimType.ifPresent(mt -> brevdata.avklartMaritimType = mt);

        brevdata.fartsområdeErInnenriks = søknad.maritimtArbeid.stream()
            .map(ma -> ma.fartsomradeKode)
            .findFirst()
            .map(fo -> fo.equalsIgnoreCase(Fartsomraade.INNENRIKS.getKode()))
            .filter(fo -> !fo);

        return brevdata;
    }

    private BrevDataInnvilgelse lagInnvilgelseBrevdataMedA1(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataInnvilgelse brevdata = new BrevDataInnvilgelse(saksbehandler, brevbestillingDto);

        BrevDataA1 vedleggA1 = (BrevDataA1) brevbyggerA1.lag(behandling, saksbehandler);
        brevdata.vedleggA1 = vedleggA1;
        brevdata.norskeVirksomheter = vedleggA1.norskeVirksomheter;
        return brevdata;
    }
}
