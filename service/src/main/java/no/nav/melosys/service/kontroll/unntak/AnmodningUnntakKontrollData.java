package no.nav.melosys.service.kontroll.unntak;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.person.Persondata;

public class AnmodningUnntakKontrollData {
    private final Persondata persondata;
    private final BehandlingsgrunnlagData behandlingsgrunnlagData;
    private final Anmodningsperiode anmodningsperiode;
    private final int antallArbeidsgivere;

    public AnmodningUnntakKontrollData(Persondata persondata,
                                       BehandlingsgrunnlagData behandlingsgrunnlagData,
                                       Anmodningsperiode anmodningsperiode,
                                       int antallArbeidsgivere) {
        this.persondata = persondata;
        this.behandlingsgrunnlagData = behandlingsgrunnlagData;
        this.anmodningsperiode = anmodningsperiode;
        this.antallArbeidsgivere = antallArbeidsgivere;
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

    public int getAntallArbeidsgivere() {
        return antallArbeidsgivere;
    }
}
