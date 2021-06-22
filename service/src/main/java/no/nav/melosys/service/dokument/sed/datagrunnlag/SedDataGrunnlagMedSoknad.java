package no.nav.melosys.service.dokument.sed.datagrunnlag;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.ArbeidsstedGrunnlag;
import no.nav.melosys.service.dokument.BostedGrunnlag;
import no.nav.melosys.service.dokument.brev.datagrunnlag.AvklarteVirksomheterGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class SedDataGrunnlagMedSoknad implements SedDataGrunnlag {
    private final Behandling behandling;
    private final BehandlingsgrunnlagData behandlingsgrunnlagData;
    private final PersonDokument person;
    private final AvklarteVirksomheterGrunnlag avklarteVirksomheterGrunnlag;
    private final BostedGrunnlag bostedGrunnlag;
    private final ArbeidsstedGrunnlag arbeidsstedGrunnlag;

    public SedDataGrunnlagMedSoknad(Behandling behandling,
                                    KodeverkService kodeverkService,
                                    AvklarteVirksomheterService avklarteVirksomheterService,
                                    AvklartefaktaService avklartefaktaService) {
        this.behandling = behandling;
        this.behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        this.person = behandling.hentPersonDokument();
        this.avklarteVirksomheterGrunnlag = new AvklarteVirksomheterGrunnlag(behandling, avklarteVirksomheterService);
        this.bostedGrunnlag = new BostedGrunnlag(behandlingsgrunnlagData, person.hentBostedsadresse().orElse(null),
            kodeverkService);
        this.arbeidsstedGrunnlag = new ArbeidsstedGrunnlag(
            avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(behandling.getId()),
            getAvklarteVirksomheterGrunnlag(),
            behandlingsgrunnlagData
        );
    }

    @Override
    public Behandling getBehandling() {
        return behandling;
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
}
