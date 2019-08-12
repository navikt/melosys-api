package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.*;

public class BrevDataByggerInnvilgelseFlereLand extends AbstraktDokumentDataBygger implements BrevDataBygger {
    private final LandvelgerService landVelgerService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final BrevbestillingDto brevbestillingDto;
    private final BrevDataByggerA1 brevbyggerA1;

    public BrevDataByggerInnvilgelseFlereLand(AvklartefaktaService avklartefaktaService,
                                              AvklarteVirksomheterService avklarteVirksomheterService,
                                              LandvelgerService landVelgerService,
                                              LovvalgsperiodeService lovvalgsperiodeService,
                                              BrevbestillingDto brevbestillingDto,
                                              BrevDataByggerA1 brevbyggerA1) {
        super(null, lovvalgsperiodeService, avklartefaktaService);
        this.landVelgerService = landVelgerService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.brevbestillingDto = brevbestillingDto;
        this.brevbyggerA1 = brevbyggerA1;
    }

    private static final Function<OrganisasjonDokument, Adresse> UTEN_ADRESSE = org -> null;

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);

        BrevDataInnvilgelseFlereLand brevdata = lagInnvilgelseBrevdataMedA1(behandling, saksbehandler);

        brevdata.norskeArbeidsgivere = avklarteVirksomheterService.hentArbeidsgivere(behandling, UTEN_ADRESSE);
        brevdata.norskeSelvstendigVirksomheter = avklarteVirksomheterService.hentSelvstendigeForetak(behandling, UTEN_ADRESSE);

        brevdata.lovvalgsperiode = hentLovvalgsperiode();
        brevdata.alleArbeidsland = landVelgerService.hentAlleArbeidsland(behandling).stream()
            .map(Landkoder::getBeskrivelse)
            .collect(Collectors.toList());

        brevdata.trygdemyndighetsland = landVelgerService.hentTrygdemyndighetsland(behandling).getBeskrivelse();
        brevdata.bostedsland = landVelgerService.hentBostedsland(behandling, søknad).getBeskrivelse();

        Optional<Maritimtyper> maritimType = avklartefaktaService.hentMaritimType(behandling.getId());
        maritimType.ifPresent(mt -> brevdata.avklartMaritimType = mt);

        brevdata.erMarginaltArbeid = avklartefaktaService.harMarginaltArbeid(behandling.getId());
        brevdata.erBegrensetPeriode = true;

        return brevdata;
    }

    private BrevDataInnvilgelseFlereLand lagInnvilgelseBrevdataMedA1(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataInnvilgelseFlereLand brevdata = new BrevDataInnvilgelseFlereLand(brevbestillingDto, saksbehandler);
        brevdata.vedleggA1 = (BrevDataA1) brevbyggerA1.lag(behandling, saksbehandler);
        return brevdata;
    }
}