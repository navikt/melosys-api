package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.ArbeidsstedGrunnlag;
import no.nav.melosys.service.dokument.BostedGrunnlag;
import no.nav.melosys.service.dokument.DataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class BrevDataGrunnlag implements DataGrunnlag {
    private final ArbeidsstedGrunnlag arbeidsstedGrunnlag;
    private final AvklarteVirksomheterGrunnlag avklarteVirksomheterGrunnlag;
    private final MottatteOpplysningerData mottatteOpplysningerData;
    private final BostedGrunnlag bostedGrunnlag;
    private final DoksysBrevbestilling brevbestilling;
    private final Persondata person;

    public BrevDataGrunnlag(DoksysBrevbestilling brevbestilling,
                            KodeverkService kodeverkService,
                            AvklarteVirksomheterService avklarteVirksomheterService,
                            AvklartefaktaService avklartefaktaService,
                            Persondata persondata) {
        this.brevbestilling = brevbestilling;
        final Behandling behandling = brevbestilling.getBehandling();
        this.mottatteOpplysningerData = Optional.ofNullable(behandling.getMottatteOpplysninger())
            .map(MottatteOpplysninger::getMottatteOpplysningerData)
            .orElse(null);
        this.person = persondata;
        this.avklarteVirksomheterGrunnlag = new AvklarteVirksomheterGrunnlag(behandling, avklarteVirksomheterService);
        this.bostedGrunnlag = new BostedGrunnlag(mottatteOpplysningerData, person.finnBostedsadresse().orElse(null),
            kodeverkService);
        this.arbeidsstedGrunnlag = new ArbeidsstedGrunnlag(
            avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(behandling.getId()),
            getAvklarteVirksomheterGrunnlag(),
            mottatteOpplysningerData
        );
    }

    @Override
    public Behandling getBehandling() {
        return brevbestilling.getBehandling();
    }

    public DoksysBrevbestilling getBrevbestilling() {
        return brevbestilling;
    }

    public MottatteOpplysningerData getMottatteOpplysningerData() {
        return mottatteOpplysningerData;
    }

    public Persondata getPerson() {
        return person;
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
