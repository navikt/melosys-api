package no.nav.melosys.service.dokument.brev.datagrunnlag;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.ArbeidsstedGrunnlag;
import no.nav.melosys.service.dokument.BostedGrunnlag;
import no.nav.melosys.service.dokument.DataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;

public class BrevDataGrunnlag implements DataGrunnlag {
    private final DoksysBrevbestilling brevbestilling;
    private final BehandlingsgrunnlagData behandlingsgrunnlagData;
    private final AvklarteVirksomheterGrunnlag avklarteVirksomheterGrunnlag;
    private final Persondata person;
    private final BostedGrunnlag bostedGrunnlag;
    private final ArbeidsstedGrunnlag arbeidsstedGrunnlag;
    private final Unleash unleash;

    public BrevDataGrunnlag(DoksysBrevbestilling brevbestilling,
                            KodeverkService kodeverkService,
                            AvklarteVirksomheterService avklarteVirksomheterService,
                            AvklartefaktaService avklartefaktaService,
                            PersondataFasade persondataFasade,
                            Unleash unleash) {
        this.unleash = unleash;
        this.brevbestilling = brevbestilling;
        final Behandling behandling = brevbestilling.getBehandling();
        this.behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        this.person = hentPersondata(behandling, persondataFasade);
        this.avklarteVirksomheterGrunnlag = new AvklarteVirksomheterGrunnlag(behandling, avklarteVirksomheterService);
        this.bostedGrunnlag = new BostedGrunnlag(behandlingsgrunnlagData, person.finnBostedsadresse().orElse(null),
            kodeverkService);
        this.arbeidsstedGrunnlag = new ArbeidsstedGrunnlag(
            avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(behandling.getId()),
            getAvklarteVirksomheterGrunnlag(),
            behandlingsgrunnlagData
        );
    }

    @Override
    public Behandling getBehandling() {
        return brevbestilling.getBehandling();
    }

    public DoksysBrevbestilling getBrevbestilling() {
        return brevbestilling;
    }

    public BehandlingsgrunnlagData getBehandlingsgrunnlagData() {
        return behandlingsgrunnlagData;
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

    private Persondata hentPersondata(Behandling behandling, PersondataFasade persondataFasade) {
        if (unleash.isEnabled("<melosys.pdl.brevdata.hentPerson>")) {
            return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID());
        }
        return behandling.hentPersonDokument();
    }
}
