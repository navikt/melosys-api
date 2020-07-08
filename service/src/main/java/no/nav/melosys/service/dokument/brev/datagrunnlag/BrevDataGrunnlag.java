package no.nav.melosys.service.dokument.brev.datagrunnlag;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.BostedGrunnlag;
import no.nav.melosys.service.dokument.DataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class BrevDataGrunnlag implements DataGrunnlag {
    private final Behandling behandling;
    private final BehandlingsgrunnlagData behandlingsgrunnlagData;
    private final PersonDokument person;

    private final AvklarteVirksomheterGrunnlag avklarteVirksomheterGrunnlag;
    private final BostedGrunnlag bostedGrunnlag;
    private final ArbeidsstedGrunnlag arbeidsstedGrunnlag;

    public BrevDataGrunnlag(Behandling behandling,
                            KodeverkService kodeverkService,
                            AvklarteVirksomheterService avklarteVirksomheterService,
                            AvklartefaktaService avklartefaktaService) throws TekniskException {
        this.behandling = behandling;
        this.behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);
        this.avklarteVirksomheterGrunnlag = new AvklarteVirksomheterGrunnlag(behandling, avklarteVirksomheterService, kodeverkService);
        this.bostedGrunnlag = new BostedGrunnlag(behandlingsgrunnlagData, getPerson(), kodeverkService);
        this.arbeidsstedGrunnlag = new ArbeidsstedGrunnlag(behandling, behandlingsgrunnlagData, getAvklarteVirksomheterGrunnlag(), avklartefaktaService);
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