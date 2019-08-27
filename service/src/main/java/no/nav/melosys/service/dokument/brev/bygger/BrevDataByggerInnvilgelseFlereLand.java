package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
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
import no.nav.melosys.service.dokument.brev.ressurser.Dokumentressurser;

public class BrevDataByggerInnvilgelseFlereLand implements BrevDataBygger {
    private final AvklartefaktaService avklartefaktaService;
    private final BrevbestillingDto brevbestillingDto;
    private final BrevDataByggerA1 brevbyggerA1;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final LandvelgerService landvelgerService;
    private Behandling behandling;
    private SoeknadDokument søknad;

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
    public BrevData lag(Dokumentressurser dokumentressurser, String saksbehandler) throws FunksjonellException, TekniskException {
        this.behandling = dokumentressurser.getBehandling();
        this.søknad = dokumentressurser.getSøknad();

        BrevDataInnvilgelseFlereLand brevdata = lagInnvilgelseBrevdataMedA1(dokumentressurser, saksbehandler);

        brevdata.norskeArbeidsgivere = dokumentressurser.getAvklarteVirksomheter().hentNorskeArbeidsgivere();
        brevdata.norskeSelvstendigVirksomheter = dokumentressurser.getAvklarteVirksomheter().hentNorskeSelvstendige();

        brevdata.lovvalgsperiode = lovvalgsperiodeService.hentLovvalgsperiode(behandling.getId());
        brevdata.alleArbeidsland = landvelgerService.hentAlleArbeidsland(behandling).stream()
            .map(Landkoder::getBeskrivelse)
            .collect(Collectors.toList());

        brevdata.bostedsland = landvelgerService.hentBostedsland(behandling, søknad).getBeskrivelse();

        Optional<Maritimtyper> maritimType = avklartefaktaService.hentMaritimType(behandling.getId());
        maritimType.ifPresent(mt -> brevdata.avklartMaritimType = mt);

        brevdata.erMarginaltArbeid = avklartefaktaService.harMarginaltArbeid(behandling.getId());
        brevdata.erBegrensetPeriode = true;

        return brevdata;
    }

    private BrevDataInnvilgelseFlereLand lagInnvilgelseBrevdataMedA1(Dokumentressurser dokumentressurser, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataInnvilgelseFlereLand brevdata = new BrevDataInnvilgelseFlereLand(brevbestillingDto, saksbehandler);
        brevdata.vedleggA1 = (BrevDataA1) brevbyggerA1.lag(dokumentressurser, saksbehandler);
        return brevdata;
    }
}