package no.nav.melosys.service.dokument.sed.datagrunnlag;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BostedGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class SedDataGrunnlagUtenSoknad implements SedDataGrunnlag {

    private Behandling behandling;
    private PersonDokument personDokument;
    private BostedGrunnlag bostedGrunnlag;

    public SedDataGrunnlagUtenSoknad(Behandling behandling, KodeverkService kodeverkService) throws TekniskException {
        this.behandling = behandling;
        this.personDokument = SaksopplysningerUtils.hentPersonDokument(behandling);
        this.bostedGrunnlag = new BostedGrunnlag(null, personDokument, kodeverkService);
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public PersonDokument getPerson() {
        return personDokument;
    }

    public void setPerson(PersonDokument personDokument) {
        this.personDokument = personDokument;
    }

    public BostedGrunnlag getBostedGrunnlag() {
        return bostedGrunnlag;
    }

    public void setBostedGrunnlag(BostedGrunnlag bostedGrunnlag) {
        this.bostedGrunnlag = bostedGrunnlag;
    }
}
