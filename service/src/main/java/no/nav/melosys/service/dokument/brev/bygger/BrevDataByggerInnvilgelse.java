package no.nav.melosys.service.dokument.brev.bygger;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.ressurser.Brevressurser;

public class BrevDataByggerInnvilgelse implements BrevDataBygger {
    private final LandvelgerService landVelgerService;
    private final AvklartefaktaService avklartefaktaService;
    private final BrevbestillingDto brevbestillingDto;
    private final BrevDataByggerA1 brevbyggerA1;
    private final Brevressurser brevressurser;
    private final Behandling behandling;

    public BrevDataByggerInnvilgelse(Brevressurser brevressurser,
                                     AvklartefaktaService avklartefaktaService,
                                     BrevbestillingDto brevbestillingDto) {
        this.brevressurser = brevressurser;
        this.behandling = brevressurser.getBehandling();
        this.landVelgerService = brevressurser.getLandvelger();
        this.avklartefaktaService = avklartefaktaService;
        this.brevbestillingDto = brevbestillingDto;
        this.brevbyggerA1 = null;
    }

    public BrevDataByggerInnvilgelse(Brevressurser brevressurser,
                                     AvklartefaktaService avklartefaktaService,
                                     BrevbestillingDto brevbestillingDto,
                                     BrevDataByggerA1 brevbyggerA1) {
        this.landVelgerService = brevressurser.getLandvelger();
        this.brevressurser = brevressurser;
        this.behandling = brevressurser.getBehandling();
        this.avklartefaktaService = avklartefaktaService;
        this.brevbestillingDto = brevbestillingDto;
        this.brevbyggerA1 = brevbyggerA1;
    }

    @Override
    public BrevData lag(String saksbehandler) throws FunksjonellException, TekniskException {
        // Bruker skal ha A1 som vedlegg - Arbeidsgiver skal ikke
        BrevDataInnvilgelse brevdata;
        if (brevbyggerA1 != null) {
            brevdata = lagInnvilgelseBrevdataMedA1(saksbehandler);
        }
        else {
            brevdata = new BrevDataInnvilgelse(brevbestillingDto, saksbehandler);
        }

        brevdata.lovvalgsperiode = brevressurser.getLovvalgsperioder().hentLovvalgsperiode();
        brevdata.arbeidsland = landVelgerService.hentArbeidsland(behandling).getBeskrivelse();

        brevdata.trygdemyndighetsland = landVelgerService.hentUtenlandskTrygdemyndighetsland(behandling).stream()
            .findFirst()
            .map(Landkoder::getBeskrivelse)
            .orElse(null);

        List<AvklartVirksomhet> norskeVirksomheter = brevressurser.getAvklarteVirksomheter().hentAlleNorskeVirksomheterMedAdresse();
        brevdata.hovedvirksomhet = norskeVirksomheter.get(0);

        Optional<Maritimtyper> maritimType = avklartefaktaService.hentMaritimType(behandling.getId());
        maritimType.ifPresent(mt -> brevdata.avklartMaritimType = mt);

        return brevdata;
    }

    private BrevDataInnvilgelse lagInnvilgelseBrevdataMedA1(String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataInnvilgelse brevdata = new BrevDataInnvilgelse(brevbestillingDto, saksbehandler);
        brevdata.vedleggA1 = (BrevDataA1) brevbyggerA1.lag(saksbehandler);
        return brevdata;
    }
}