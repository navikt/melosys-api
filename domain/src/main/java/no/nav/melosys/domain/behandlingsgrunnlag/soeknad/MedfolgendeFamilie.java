package no.nav.melosys.domain.behandlingsgrunnlag.soeknad;

public class MedfolgendeFamilie {
    public String fnr;
    public String navn;
    public Relasjonsrolle relasjonsrolle;

    public enum Relasjonsrolle {
        BARN
    }

    public static MedfolgendeFamilie tilBarnFraFnr(String fnr) {
        MedfolgendeFamilie medfolgendeFamilie = new MedfolgendeFamilie();
        medfolgendeFamilie.fnr = fnr;
        medfolgendeFamilie.relasjonsrolle = Relasjonsrolle.BARN;
        return medfolgendeFamilie;
    }

    public String getFnr() {
        return fnr;
    }

    static boolean erBarn(MedfolgendeFamilie medfolgendeFamilie) {
        return medfolgendeFamilie.relasjonsrolle == Relasjonsrolle.BARN;
    }
}
