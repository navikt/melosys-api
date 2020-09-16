package no.nav.melosys.service.kontroll.unntak;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.person.PersonDokument;

public class AnmodningUnntakKontrollData {
    private final PersonDokument personDokument;
    private final BehandlingsgrunnlagData behandlingsgrunnlagData;

    public AnmodningUnntakKontrollData(PersonDokument personDokument, BehandlingsgrunnlagData behandlingsgrunnlagData) {
        this.personDokument = personDokument;
        this.behandlingsgrunnlagData = behandlingsgrunnlagData;
    }

    public PersonDokument getPersonDokument() {
        return personDokument;
    }

    public BehandlingsgrunnlagData getBehandlingsgrunnlagData() {
        return behandlingsgrunnlagData;
    }
}
