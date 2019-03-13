package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Optional;
import java.util.function.Function;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartInnstallasjonsType;
import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;

public class BrevDataByggerInnvilgelse extends AbstraktDokumentDataBygger implements BrevDataBygger {
    private BrevDataByggerA1 a1Bygger;
    private AvklarteVirksomheterService avklarteVirksomheterService;
    private BrevbestillingDto brevbestillingDto;

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     AvklarteVirksomheterService avklarteVirksomheterService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     BrevbestillingDto brevbestillingDto) {
        super(null, lovvalgsperiodeService, avklartefaktaService);
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.brevbestillingDto = brevbestillingDto;
    }

    public void setA1Bygger(BrevDataByggerA1 a1Bygger) {
        this.a1Bygger = a1Bygger;
    }

    Function<OrganisasjonDokument, Adresse> ingenAdresse = org -> null;

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        this.behandling = behandling;

        if (a1Bygger != null) {
            return lagInnvilgelseBrevdataMedA1(behandling, saksbehandler);
        }
        else {
            return lagBrevdataInnvilgelseUtenA1(behandling, saksbehandler);
        }
    }

    private BrevDataInnvilgelse lagInnvilgelseBrevdataMedA1(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataInnvilgelse brevData = new BrevDataInnvilgelse(saksbehandler, brevbestillingDto);
        brevData.lovvalgsperiode = hentLovvalgsperiode();

        BrevDataA1 vedleggA1 = (BrevDataA1) a1Bygger.lag(behandling, saksbehandler);
        brevData.vedleggA1 = vedleggA1;
        brevData.norskeVirksomheter = vedleggA1.norskeVirksomheter;

        Optional<AvklartInnstallasjonsType> innstallasjonsType = avklartefaktaService.hentInnstallasjonsType(behandling.getId());
        innstallasjonsType.ifPresent(innstallasjon -> brevData.avklartSokkelEllerSkip = innstallasjon);

        return brevData;
    }

    private BrevData lagBrevdataInnvilgelseUtenA1(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataInnvilgelse brevData = new BrevDataInnvilgelse(saksbehandler, brevbestillingDto);
        brevData.lovvalgsperiode = hentLovvalgsperiode();
        brevData.norskeVirksomheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, ingenAdresse);

        return brevData;
    }
}
