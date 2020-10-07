package no.nav.melosys.service.kontroll.unntak;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.person.PersonDokument;

public class AnmodningUnntakKontrollData {
    private final PersonDokument personDokument;
    private final BehandlingsgrunnlagData behandlingsgrunnlagData;
    private final Anmodningsperiode anmodningsperiode;

    public AnmodningUnntakKontrollData(PersonDokument personDokument,
                                       BehandlingsgrunnlagData behandlingsgrunnlagData,
                                       Anmodningsperiode anmodningsperiode) {
        this.personDokument = personDokument;
        this.behandlingsgrunnlagData = behandlingsgrunnlagData;
        this.anmodningsperiode = anmodningsperiode;
    }

    public PersonDokument getPersonDokument() {
        return personDokument;
    }

    public BehandlingsgrunnlagData getBehandlingsgrunnlagData() {
        return behandlingsgrunnlagData;
    }

    public Anmodningsperiode getAnmodningsperiode() {
        return anmodningsperiode;
    }
}
