package no.nav.melosys.service.dokument.sed.datagrunnlag;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.ArbeidsstedGrunnlag;
import no.nav.melosys.service.dokument.BostedGrunnlag;
import no.nav.melosys.service.dokument.brev.datagrunnlag.AvklarteVirksomheterGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class SedDataGrunnlagMedSoknad implements SedDataGrunnlag {
    private final ArbeidsstedGrunnlag arbeidsstedGrunnlag;
    private final AvklarteVirksomheterGrunnlag avklarteVirksomheterGrunnlag;
    private final Behandling behandling;
    private final MottatteOpplysningerData mottatteOpplysningerData;
    private final BostedGrunnlag bostedGrunnlag;
    private final Persondata persondata;

    public SedDataGrunnlagMedSoknad(Behandling behandling, KodeverkService kodeverkService,
                                    AvklarteVirksomheterService avklarteVirksomheterService,
                                    AvklartefaktaService avklartefaktaService, Persondata persondata) {
        this.avklarteVirksomheterGrunnlag = new AvklarteVirksomheterGrunnlag(behandling, avklarteVirksomheterService);
        this.behandling = behandling;
        this.mottatteOpplysningerData = behandling.getMottatteOpplysninger().getMottatteOpplysningerData();
        this.persondata = persondata;
        this.bostedGrunnlag = new BostedGrunnlag(
            mottatteOpplysningerData,
            this.persondata.finnBostedsadresse().orElse(null),
            this.persondata.finnKontaktadresse().orElse(null),
            kodeverkService);
        this.arbeidsstedGrunnlag = new ArbeidsstedGrunnlag(
            avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(behandling.getId()),
            getAvklarteVirksomheterGrunnlag(),
            mottatteOpplysningerData
        );
    }

    @Override
    public Behandling getBehandling() {
        return behandling;
    }

    public MottatteOpplysningerData getMottatteOpplysningerData() {
        return mottatteOpplysningerData;
    }

    @Override
    public Persondata getPersondata() {
        return persondata;
    }

    public AvklarteVirksomheterGrunnlag getAvklarteVirksomheterGrunnlag() {
        return avklarteVirksomheterGrunnlag;
    }

    @Override
    public BostedGrunnlag getBostedGrunnlag() {
        return bostedGrunnlag;
    }

    public ArbeidsstedGrunnlag getArbeidsstedGrunnlag() {
        return arbeidsstedGrunnlag;
    }
}
