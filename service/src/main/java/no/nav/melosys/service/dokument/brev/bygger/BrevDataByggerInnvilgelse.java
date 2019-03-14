package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartInnstallasjonsType;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;

public class BrevDataByggerInnvilgelse extends AbstraktDokumentDataBygger implements BrevDataBygger {
    private BrevDataByggerA1 a1Bygger;
    private BrevbestillingDto brevbestillingDto;

    public BrevDataByggerInnvilgelse(AvklartefaktaService avklartefaktaService,
                                     LovvalgsperiodeService lovvalgsperiodeService,
                                     BrevbestillingDto brevbestillingDto) {
        super(null, lovvalgsperiodeService, avklartefaktaService);
        this.brevbestillingDto = brevbestillingDto;
    }

    public void setA1Bygger(BrevDataByggerA1 a1Bygger) {
        this.a1Bygger = a1Bygger;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);

        BrevDataInnvilgelse brevdata;
        if (a1Bygger != null) {
            brevdata = lagInnvilgelseBrevdataMedA1(behandling, saksbehandler);
        }
        else {
            brevdata = new BrevDataInnvilgelse(saksbehandler, brevbestillingDto);
        }

        Lovvalgsperiode lovvalgsperiode = hentLovvalgsperiode();
        brevdata.lovvalgsperiode = lovvalgsperiode;
        brevdata.arbeidsland = hentArbeidsland(lovvalgsperiode);
        brevdata.trygdemyndighetsland = hentTrygdemyndighetsland(lovvalgsperiode);

        Optional<AvklartInnstallasjonsType> innstallasjonsType = avklartefaktaService.hentInnstallasjonsType(behandling.getId());
        innstallasjonsType.ifPresent(innstallasjon -> brevdata.avklartSokkelEllerSkip = innstallasjon);

        return brevdata;
    }

    private BrevDataInnvilgelse lagInnvilgelseBrevdataMedA1(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataInnvilgelse brevdata = new BrevDataInnvilgelse(saksbehandler, brevbestillingDto);

        BrevDataA1 vedleggA1 = (BrevDataA1) a1Bygger.lag(behandling, saksbehandler);
        brevdata.vedleggA1 = vedleggA1;
        brevdata.norskeVirksomheter = vedleggA1.norskeVirksomheter;
        return brevdata;
    }
}
