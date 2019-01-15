
package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;


@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class Nav {


    private List<Arbeidssted> arbeidssted;

    private Bruker bruker;

    private Selvstendig selvstendig;

    private String ytterligereinformasjon;

    private List<Arbeidsgiver> arbeidsgiver;

    public List<Arbeidsgiver> getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(List<Arbeidsgiver> arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public List<Arbeidssted> getArbeidssted() {
        return arbeidssted;
    }

    public void setArbeidssted(List<Arbeidssted> arbeidssted) {
        this.arbeidssted = arbeidssted;
    }

    public Bruker getBruker() {
        return bruker;
    }

    public void setBruker(Bruker bruker) {
        this.bruker = bruker;
    }

    public Selvstendig getSelvstendig() {
        return selvstendig;
    }

    public void setSelvstendig(Selvstendig selvstendig) {
        this.selvstendig = selvstendig;
    }

    public String getYtterligereinformasjon() {
        return ytterligereinformasjon;
    }

    public void setYtterligereinformasjon(String ytterligereinformasjon) {
        this.ytterligereinformasjon = ytterligereinformasjon;
    }

}
