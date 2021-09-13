package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelseFlereLand;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import org.apache.commons.collections4.ListUtils;

public class BrevDataByggerInnvilgelseFlereLand implements BrevDataBygger {
    private final AvklartefaktaService avklartefaktaService;
    private final BrevbestillingRequest brevbestillingRequest;
    private final BrevDataByggerA1 brevbyggerA1;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final LandvelgerService landvelgerService;
    private final SaksopplysningerService saksopplysningerService;

    public BrevDataByggerInnvilgelseFlereLand(AvklartefaktaService avklartefaktaService,
                                              LandvelgerService landvelgerService,
                                              LovvalgsperiodeService lovvalgsperiodeService,
                                              SaksopplysningerService saksopplysningerService,
                                              BrevbestillingRequest brevbestillingRequest,
                                              BrevDataByggerA1 brevbyggerA1) {

        this.avklartefaktaService = avklartefaktaService;
        this.brevbestillingRequest = brevbestillingRequest;
        this.brevbyggerA1 = brevbyggerA1;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.landvelgerService = landvelgerService;
        this.saksopplysningerService = saksopplysningerService;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        long behandlingID = dataGrunnlag.getBehandling().getId();
        BehandlingsgrunnlagData grunnlagData = dataGrunnlag.getBehandlingsgrunnlagData();

        BrevDataInnvilgelseFlereLand brevdata = lagInnvilgelseBrevdataMedA1(dataGrunnlag, saksbehandler);

        brevdata.arbeidsgivere =
            ListUtils.union(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeArbeidsgivere(),
                            dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentUtenlandskeArbeidsgivere());

        brevdata.lovvalgsperiode = lovvalgsperiodeService.hentValidertLovvalgsperiode(behandlingID);
        brevdata.alleArbeidsland = landvelgerService.hentAlleArbeidsland(behandlingID).stream()
            .map(Landkoder::getBeskrivelse)
            .collect(Collectors.toList());

        brevdata.bostedsland = Landkoder.valueOf(landvelgerService.hentBostedsland(behandlingID, grunnlagData)).getBeskrivelse();

        Set<Maritimtyper> maritimType = avklartefaktaService.hentMaritimTyper(behandlingID);
        brevdata.harAvklartMaritimTypeSokkel = maritimType.stream().anyMatch(mt -> mt == Maritimtyper.SOKKEL);
        brevdata.harAvklartMaritimTypeSkip = maritimType.stream().anyMatch(mt -> mt == Maritimtyper.SKIP);

        brevdata.erMarginaltArbeid = avklartefaktaService.harMarginaltArbeid(behandlingID);
        brevdata.erBegrensetPeriode = !PeriodeKontroller.periodeErLik(
            grunnlagData.periode.getFom(), grunnlagData.periode.getTom(), brevdata.lovvalgsperiode.getFom(), brevdata.lovvalgsperiode.getTom()
        );

        if (dataGrunnlag.getBehandling().erNorgeUtpekt()) {
            brevdata.trydemyndighetsland = saksopplysningerService.hentSedOpplysninger(behandlingID).getAvsenderLandkode();
        }

        return brevdata;
    }

    private BrevDataInnvilgelseFlereLand lagInnvilgelseBrevdataMedA1(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        BrevDataInnvilgelseFlereLand brevdata = new BrevDataInnvilgelseFlereLand(brevbestillingRequest, saksbehandler);
        brevdata.vedleggA1 = (BrevDataA1) brevbyggerA1.lag(dataGrunnlag, saksbehandler);
        return brevdata;
    }
}
