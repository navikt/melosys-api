package no.nav.melosys.service.dokument.sed.datagrunnlag;

import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.BostedGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class SedDataGrunnlagUtenSoknad implements SedDataGrunnlag {
    private final Behandling behandling;
    private final PersonDokument personDokument;
    private final BostedGrunnlag bostedGrunnlag;

    public SedDataGrunnlagUtenSoknad(Behandling behandling, KodeverkService kodeverkService) throws TekniskException {
        this.behandling = behandling;
        this.personDokument = behandling.hentPersonDokument();
        this.bostedGrunnlag = new BostedGrunnlag(null, personDokument, kodeverkService);
    }

    @Override
    public Behandling getBehandling() {
        return behandling;
    }

    public PersonDokument getPerson() {
        return personDokument;
    }

    @Override
    public BostedGrunnlag getBostedGrunnlag() {
        return bostedGrunnlag;
    }
}
