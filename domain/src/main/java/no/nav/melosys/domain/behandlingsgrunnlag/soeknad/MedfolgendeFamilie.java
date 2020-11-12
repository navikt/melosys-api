package no.nav.melosys.domain.behandlingsgrunnlag.soeknad;

public class MedfolgendeFamilie {
    public String fnr;
    public String navn;
    public Relasjon relasjonsrolle;

    public enum Relasjon {
        BARN
    }

    public static MedfolgendeFamilie tilBarnFraFnr(String fnr) {
        MedfolgendeFamilie medfolgendeFamilie = new MedfolgendeFamilie();
        medfolgendeFamilie.fnr = fnr;
        medfolgendeFamilie.relasjonsrolle = Relasjon.BARN;
        return medfolgendeFamilie;
    }
}
