package no.nav.melosys.domain.eessi.sed;

import java.util.ArrayList;
import java.util.List;

public class SedGrunnlagA003Dto extends SedGrunnlagDto {
    private List<Bestemmelse> overgangsregelbestemmelser = new ArrayList<>();
    private List<Virksomhet> norskeArbeidsgivendeVirksomheter = new ArrayList<>();

    public List<Bestemmelse> getOvergangsregelbestemmelser() {
        return overgangsregelbestemmelser;
    }

    public void setOvergangsregelbestemmelser(List<Bestemmelse> overgangsregelbestemmelser) {
        this.overgangsregelbestemmelser = overgangsregelbestemmelser;
    }

    public List<Virksomhet> getNorskeArbeidsgivendeVirksomheter() {
        return norskeArbeidsgivendeVirksomheter;
    }

    public void setNorskeArbeidsgivendeVirksomheter(List<Virksomhet> norskeArbeidsgivendeVirksomheter) {
        this.norskeArbeidsgivendeVirksomheter = norskeArbeidsgivendeVirksomheter;
    }
}
