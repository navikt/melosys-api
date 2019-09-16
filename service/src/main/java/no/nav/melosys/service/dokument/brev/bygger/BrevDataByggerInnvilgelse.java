package no.nav.melosys.service.dokument.brev.bygger;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlag;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;

public class BrevDataByggerInnvilgelse implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final AvklartefaktaService avklartefaktaService;
    private final BrevbestillingDto brevbestillingDto;
    private final BrevDataByggerA1 brevbyggerA1;
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    private DokumentdataGrunnlag dataGrunnlag;

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     LandvelgerService landvelgerService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     AnmodningsperiodeService anmodningsperiodeService,
                                     BrevbestillingDto brevbestillingDto) {
        this.landvelgerService = landvelgerService;
        this.avklartefaktaService = avklartefaktaService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.brevbestillingDto = brevbestillingDto;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.brevbyggerA1 = null;
    }

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     LandvelgerService landvelgerService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     AnmodningsperiodeService anmodningsperiodeService,
                                     BrevbestillingDto brevbestillingDto,
                                     BrevDataByggerA1 brevbyggerA1) {
        this.landvelgerService = landvelgerService;
        this.avklartefaktaService = avklartefaktaService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.brevbestillingDto = brevbestillingDto;
        this.brevbyggerA1 = brevbyggerA1;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    @Override
    public BrevData lag(DokumentdataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException, TekniskException {
        this.dataGrunnlag = dataGrunnlag;
        long behandlingID = dataGrunnlag.getBehandling().getId();

        // Bruker skal ha A1 som vedlegg - Arbeidsgiver skal ikke
        BrevDataInnvilgelse brevdata;
        if (brevbyggerA1 != null) {
            brevdata = lagInnvilgelseBrevdataMedA1(saksbehandler);
        }
        else {
            brevdata = new BrevDataInnvilgelse(brevbestillingDto, saksbehandler);
        }

        brevdata.lovvalgsperiode = lovvalgsperiodeService.hentLovvalgsperiode(behandlingID);
        brevdata.arbeidsland = landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse();

        brevdata.trygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID).stream()
            .findFirst()
            .map(Landkoder::getBeskrivelse)
            .orElse(null);

        List<AvklartVirksomhet> norskeVirksomheter = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentAlleNorskeVirksomheterMedAdresse();
        brevdata.hovedvirksomhet = norskeVirksomheter.get(0);

        Optional<Maritimtyper> maritimType = avklartefaktaService.hentMaritimType(behandlingID);
        maritimType.ifPresent(mt -> brevdata.avklartMaritimType = mt);

        brevdata.anmodningsperiodesvar = anmodningsperiodeService.hentAnmodningsperioder(behandlingID)
            .stream()
            .filter(Anmodningsperiode::erSendtUtland)
            .findFirst()
            .map(Anmodningsperiode::getAnmodningsperiodeSvar);

        return brevdata;
    }

    private BrevDataInnvilgelse lagInnvilgelseBrevdataMedA1(String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataInnvilgelse brevdata = new BrevDataInnvilgelse(brevbestillingDto, saksbehandler);
        brevdata.vedleggA1 = (BrevDataA1) brevbyggerA1.lag(dataGrunnlag, saksbehandler);
        return brevdata;
    }
}