package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelseFlereLand;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.ressurser.Brevressurser;

public class BrevDataByggerInnvilgelseFlereLand implements BrevDataBygger {
    private final Brevressurser brevressurser;
    private final AvklartefaktaService avklartefaktaService;
    private final BrevbestillingDto brevbestillingDto;
    private final BrevDataByggerA1 brevbyggerA1;
    private final Behandling behandling;
    private final SoeknadDokument søknad;

    public BrevDataByggerInnvilgelseFlereLand(Brevressurser brevressurser,
                                              AvklartefaktaService avklartefaktaService,
                                              BrevbestillingDto brevbestillingDto,
                                              BrevDataByggerA1 brevbyggerA1) {
        this.brevressurser = brevressurser;
        this.behandling = brevressurser.getBehandling();
        this.søknad = brevressurser.getSøknad();

        this.avklartefaktaService = avklartefaktaService;
        this.brevbestillingDto = brevbestillingDto;
        this.brevbyggerA1 = brevbyggerA1;
    }

    @Override
    public BrevData lag(String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataInnvilgelseFlereLand brevdata = lagInnvilgelseBrevdataMedA1(saksbehandler);

        brevdata.norskeArbeidsgivere = brevressurser.getAvklarteVirksomheter().hentNorskeArbeidsgivere();
        brevdata.norskeSelvstendigVirksomheter = brevressurser.getAvklarteVirksomheter().hentNorskeSelvstendige();

        brevdata.lovvalgsperiode = brevressurser.getLovvalgsperioder().hentLovvalgsperiode();
        brevdata.alleArbeidsland = brevressurser.getLandvelger().hentAlleArbeidsland(brevressurser.getBehandling()).stream()
            .map(Landkoder::getBeskrivelse)
            .collect(Collectors.toList());

        brevdata.bostedsland = brevressurser.getLandvelger().hentBostedsland(behandling, søknad).getBeskrivelse();

        Optional<Maritimtyper> maritimType = avklartefaktaService.hentMaritimType(behandling.getId());
        maritimType.ifPresent(mt -> brevdata.avklartMaritimType = mt);

        brevdata.erMarginaltArbeid = avklartefaktaService.harMarginaltArbeid(behandling.getId());
        brevdata.erBegrensetPeriode = true;

        return brevdata;
    }

    private BrevDataInnvilgelseFlereLand lagInnvilgelseBrevdataMedA1(String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataInnvilgelseFlereLand brevdata = new BrevDataInnvilgelseFlereLand(brevbestillingDto, saksbehandler);
        brevdata.vedleggA1 = (BrevDataA1) brevbyggerA1.lag(saksbehandler);
        return brevdata;
    }
}