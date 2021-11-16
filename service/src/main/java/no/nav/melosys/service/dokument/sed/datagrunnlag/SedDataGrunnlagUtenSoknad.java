package no.nav.melosys.service.dokument.sed.datagrunnlag;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.dokument.BostedGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class SedDataGrunnlagUtenSoknad implements SedDataGrunnlag {
    private final Behandling behandling;
    private final BostedGrunnlag bostedGrunnlag;
    private final Persondata persondata;
    private final BehandlingsgrunnlagData behandlingsgrunnlagData;

    public SedDataGrunnlagUtenSoknad(Behandling behandling, KodeverkService kodeverkService, Persondata persondata) {
        this.behandling = behandling;
        this.persondata = persondata;
        this.behandlingsgrunnlagData = Optional.ofNullable(behandling.getBehandlingsgrunnlag()).isPresent() ? behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata() : null;
        this.bostedGrunnlag = new BostedGrunnlag(behandlingsgrunnlagData, persondata.finnBostedsadresse().orElse(null), kodeverkService);
    }

    @Override
    public Behandling getBehandling() {
        return behandling;
    }

    @Override
    public BostedGrunnlag getBostedGrunnlag() {
        return bostedGrunnlag;
    }

    @Override
    public Persondata getPersondata() {
        return persondata;
    }
}
