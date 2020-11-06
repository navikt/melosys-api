package no.nav.melosys.domain.behandlingsgrunnlag.soeknad;

public class MedfolgendeFamilie {
    public String fnr;
    public String navn;
    public Relasjon relasjon;

    public enum Relasjon {
        BARN
    }

    public static MedfolgendeFamilie tilBarnFraFnr(String fnr) {
        MedfolgendeFamilie medfolgendeFamilie = new MedfolgendeFamilie();
        medfolgendeFamilie.fnr = fnr;
        medfolgendeFamilie.relasjon = Relasjon.BARN;
        return medfolgendeFamilie;
    }
}
