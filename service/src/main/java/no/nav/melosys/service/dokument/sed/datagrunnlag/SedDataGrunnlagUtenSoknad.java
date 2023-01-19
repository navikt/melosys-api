package no.nav.melosys.service.dokument.sed.datagrunnlag;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.dokument.BostedGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class SedDataGrunnlagUtenSoknad implements SedDataGrunnlag {
    private final Behandling behandling;
    private final BostedGrunnlag bostedGrunnlag;
    private final Persondata persondata;

    public SedDataGrunnlagUtenSoknad(Behandling behandling, KodeverkService kodeverkService, Persondata persondata) {
        this.behandling = behandling;
        this.persondata = persondata;
        MottatteOpplysningerData mottatteOpplysningerData =
            Optional.ofNullable(behandling.getMottatteOpplysninger()).isPresent() ? behandling.getMottatteOpplysninger().getMottatteOpplysningerData() : null;
        this.bostedGrunnlag = new BostedGrunnlag(
            mottatteOpplysningerData,
            persondata.finnBostedsadresse().orElse(null),
            persondata.finnKontaktadresse().orElse(null),
            kodeverkService);
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
