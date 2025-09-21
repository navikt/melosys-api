package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelseFlereLand;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import org.apache.commons.collections4.ListUtils;

public class BrevDataByggerInnvilgelseFlereLand implements BrevDataBygger {
    private final AvklartefaktaService avklartefaktaService;
    private final BrevbestillingDto brevbestillingDto;
    private final BrevDataByggerA1 brevbyggerA1;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final LandvelgerService landvelgerService;
    private final SaksopplysningerService saksopplysningerService;

    public BrevDataByggerInnvilgelseFlereLand(AvklartefaktaService avklartefaktaService,
                                              LandvelgerService landvelgerService,
                                              LovvalgsperiodeService lovvalgsperiodeService,
                                              SaksopplysningerService saksopplysningerService,
                                              BrevbestillingDto brevbestillingDto,
                                              BrevDataByggerA1 brevbyggerA1) {

        this.avklartefaktaService = avklartefaktaService;
        this.brevbestillingDto = brevbestillingDto;
        this.brevbyggerA1 = brevbyggerA1;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.landvelgerService = landvelgerService;
        this.saksopplysningerService = saksopplysningerService;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        long behandlingID = dataGrunnlag.getBehandling().getId();
        MottatteOpplysningerData grunnlagData = dataGrunnlag.getMottatteOpplysningerData();

        BrevDataInnvilgelseFlereLand brevdata = lagInnvilgelseBrevdataMedA1(dataGrunnlag, saksbehandler);

        brevdata.setArbeidsgivere(ListUtils.union(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeArbeidsgivere(),
            dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentUtenlandskeArbeidsgivere()));

        brevdata.setLovvalgsperiode(lovvalgsperiodeService.hentLovvalgsperiode(behandlingID));
        brevdata.setUkjenteEllerAlleEosLand(landvelgerService.isFlereLandUkjentHvilke(behandlingID));
        brevdata.setAlleArbeidsland(landvelgerService.hentAlleArbeidsland(behandlingID).stream()
            .map(Land_iso2::getBeskrivelse)
            .collect(Collectors.toList()));

        brevdata.setBostedsland(landvelgerService.hentBostedsland(behandlingID, grunnlagData).getLandkodeobjekt().getBeskrivelse());

        Set<Maritimtyper> maritimType = avklartefaktaService.hentMaritimTyper(behandlingID);
        brevdata.setAvklartMaritimTypeSokkel(maritimType.stream().anyMatch(mt -> mt == Maritimtyper.SOKKEL));
        brevdata.setAvklartMaritimTypeSkip(maritimType.stream().anyMatch(mt -> mt == Maritimtyper.SKIP));

        brevdata.setMarginaltArbeid(avklartefaktaService.harMarginaltArbeid(behandlingID));
        brevdata.setBegrensetPeriode(grunnlagData.periode != null && !PeriodeRegler.periodeErLik(
            grunnlagData.periode.getFom(), grunnlagData.periode.getTom(), brevdata.getLovvalgsperiode().getFom(), brevdata.getLovvalgsperiode().getTom()
        ));

        if (dataGrunnlag.getBehandling().erNorgeUtpekt()) {
            brevdata.setTrydemyndighetsland(saksopplysningerService.hentSedOpplysninger(behandlingID).getAvsenderLandkode());
        }

        return brevdata;
    }

    private BrevDataInnvilgelseFlereLand lagInnvilgelseBrevdataMedA1(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        BrevDataInnvilgelseFlereLand brevdata = new BrevDataInnvilgelseFlereLand(brevbestillingDto, saksbehandler);
        brevdata.setVedleggA1((BrevDataA1) brevbyggerA1.lag(dataGrunnlag, saksbehandler));
        return brevdata;
    }
}
