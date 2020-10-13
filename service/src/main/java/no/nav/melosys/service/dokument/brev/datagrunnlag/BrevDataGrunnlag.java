package no.nav.melosys.service.dokument.brev.datagrunnlag;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.ArbeidsstedGrunnlag;
import no.nav.melosys.service.dokument.BostedGrunnlag;
import no.nav.melosys.service.dokument.DataGrunnlag;
import no.nav.melosys.service.dokument.MedfolgendeFamilieGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class BrevDataGrunnlag implements DataGrunnlag {
    private final Brevbestilling brevbestilling;
    private final BehandlingsgrunnlagData behandlingsgrunnlagData;
    private final PersonDokument person;
    private final AvklarteVirksomheterGrunnlag avklarteVirksomheterGrunnlag;
    private final BostedGrunnlag bostedGrunnlag;
    private final ArbeidsstedGrunnlag arbeidsstedGrunnlag;
    private final MedfolgendeFamilieGrunnlag medfolgendeFamilieGrunnlag;

    public BrevDataGrunnlag(Brevbestilling brevbestilling,
                            KodeverkService kodeverkService,
                            AvklarteVirksomheterService avklarteVirksomheterService,
                            AvklartefaktaService avklartefaktaService) throws TekniskException {
        this.brevbestilling = brevbestilling;
        final Behandling behandling = brevbestilling.getBehandling();
        this.behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        this.person = behandling.hentPersonDokument();
        this.avklarteVirksomheterGrunnlag = new AvklarteVirksomheterGrunnlag(behandling, avklarteVirksomheterService, kodeverkService);
        this.bostedGrunnlag = new BostedGrunnlag(behandlingsgrunnlagData, getPerson(), kodeverkService);
        this.arbeidsstedGrunnlag = new ArbeidsstedGrunnlag(
            avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(behandling.getId()),
            getAvklarteVirksomheterGrunnlag(),
            behandlingsgrunnlagData
        );
        this.medfolgendeFamilieGrunnlag = new MedfolgendeFamilieGrunnlag(
            avklartefaktaService.hentAvklarteMedfølgendeFamiliemedlemmer(behandling.getId()),
            person
        );
    }

    @Override
    public Behandling getBehandling() {
        return brevbestilling.getBehandling();
    }

    public Brevbestilling getBrevbestilling() {
        return brevbestilling;
    }

    public BehandlingsgrunnlagData getBehandlingsgrunnlagData() {
        return behandlingsgrunnlagData;
    }

    public PersonDokument getPerson() {
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

    public MedfolgendeFamilieGrunnlag getMedfolgendeFamilieGrunnlag() {
        return medfolgendeFamilieGrunnlag;
    }
}