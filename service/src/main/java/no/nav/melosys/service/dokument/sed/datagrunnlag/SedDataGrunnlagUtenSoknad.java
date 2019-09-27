package no.nav.melosys.service.dokument.sed.datagrunnlag;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.AdresseGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class SedDataGrunnlagUtenSoknad implements SedDataGrunnlag {

    private Behandling behandling;
    private PersonDokument personDokument;
    private AdresseGrunnlag adresseGrunnlag;

    public SedDataGrunnlagUtenSoknad(Behandling behandling, KodeverkService kodeverkService) throws TekniskException {
        this.behandling = behandling;
        this.personDokument = SaksopplysningerUtils.hentPersonDokument(behandling);
        this.adresseGrunnlag = new AdresseGrunnlag(null, personDokument, kodeverkService);
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

    public AdresseGrunnlag getAdresseGrunnlag() {
        return adresseGrunnlag;
    }

    public void setAdresseGrunnlag(AdresseGrunnlag adresseGrunnlag) {
        this.adresseGrunnlag = adresseGrunnlag;
    }
}
