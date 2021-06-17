package no.nav.melosys.service.dokument.sed.datagrunnlag;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.dokument.BostedGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class SedDataGrunnlagUtenSoknad implements SedDataGrunnlag {
    private final Behandling behandling;
    private final Persondata persondata;
    private final BostedGrunnlag bostedGrunnlag;

    public SedDataGrunnlagUtenSoknad(Behandling behandling, KodeverkService kodeverkService) {
        this.behandling = behandling;
        this.persondata = behandling.hentPersonDokument();
        this.bostedGrunnlag = new BostedGrunnlag(null, persondata.hentBostedsadresse(), kodeverkService);
    }

    @Override
    public Behandling getBehandling() {
        return behandling;
    }

    public Persondata getPerson() {
        return persondata;
    }

    @Override
    public BostedGrunnlag getBostedGrunnlag() {
        return bostedGrunnlag;
    }
}
