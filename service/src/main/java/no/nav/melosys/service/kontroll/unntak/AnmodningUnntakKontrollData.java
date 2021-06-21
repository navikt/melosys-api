package no.nav.melosys.service.kontroll.unntak;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.person.Persondata;

public class AnmodningUnntakKontrollData {
    private final Persondata persondata;
    private final BehandlingsgrunnlagData behandlingsgrunnlagData;
    private final Anmodningsperiode anmodningsperiode;

    public AnmodningUnntakKontrollData(Persondata persondata,
                                       BehandlingsgrunnlagData behandlingsgrunnlagData,
                                       Anmodningsperiode anmodningsperiode) {
        this.persondata = persondata;
        this.behandlingsgrunnlagData = behandlingsgrunnlagData;
        this.anmodningsperiode = anmodningsperiode;
    }

    public Persondata getPersonDokument() {
        return persondata;
    }

    public BehandlingsgrunnlagData getBehandlingsgrunnlagData() {
        return behandlingsgrunnlagData;
    }

    public Anmodningsperiode getAnmodningsperiode() {
        return anmodningsperiode;
    }
}
