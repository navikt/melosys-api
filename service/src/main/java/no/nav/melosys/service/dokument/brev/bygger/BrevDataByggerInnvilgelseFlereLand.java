package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelseFlereLand;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlag;

public class BrevDataByggerInnvilgelseFlereLand implements BrevDataBygger {
    private final AvklartefaktaService avklartefaktaService;
    private final BrevbestillingDto brevbestillingDto;
    private final BrevDataByggerA1 brevbyggerA1;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final LandvelgerService landvelgerService;

    public BrevDataByggerInnvilgelseFlereLand(AvklartefaktaService avklartefaktaService,
                                              LandvelgerService landvelgerService,
                                              LovvalgsperiodeService lovvalgsperiodeService,
                                              BrevbestillingDto brevbestillingDto,
                                              BrevDataByggerA1 brevbyggerA1) {

        this.avklartefaktaService = avklartefaktaService;
        this.brevbestillingDto = brevbestillingDto;
        this.brevbyggerA1 = brevbyggerA1;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.landvelgerService = landvelgerService;
    }

    @Override
    public BrevData lag(DokumentdataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException, TekniskException {
        long behandlingID = dataGrunnlag.getBehandling().getId();
        SoeknadDokument søknad = dataGrunnlag.getSøknad();

        BrevDataInnvilgelseFlereLand brevdata = lagInnvilgelseBrevdataMedA1(dataGrunnlag, saksbehandler);

        brevdata.norskeArbeidsgivere = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeArbeidsgivere();
        brevdata.norskeSelvstendigVirksomheter = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeSelvstendige();

        brevdata.lovvalgsperiode = lovvalgsperiodeService.hentLovvalgsperiode(behandlingID);
        brevdata.alleArbeidsland = landvelgerService.hentAlleArbeidsland(behandlingID).stream()
            .map(Landkoder::getBeskrivelse)
            .collect(Collectors.toList());

        brevdata.bostedsland = landvelgerService.hentBostedsland(behandlingID, søknad).getBeskrivelse();

        Optional<Maritimtyper> maritimType = avklartefaktaService.hentMaritimType(behandlingID);
        maritimType.ifPresent(mt -> brevdata.avklartMaritimType = mt);

        brevdata.erMarginaltArbeid = avklartefaktaService.harMarginaltArbeid(behandlingID);
        brevdata.erBegrensetPeriode = true;

        return brevdata;
    }

    private BrevDataInnvilgelseFlereLand lagInnvilgelseBrevdataMedA1(DokumentdataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataInnvilgelseFlereLand brevdata = new BrevDataInnvilgelseFlereLand(brevbestillingDto, saksbehandler);
        brevdata.vedleggA1 = (BrevDataA1) brevbyggerA1.lag(dataGrunnlag, saksbehandler);
        return brevdata;
    }
}