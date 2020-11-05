package no.nav.melosys.domain.behandlingsgrunnlag.soeknad;

public class MedfolgendeBarn {
    public String fnr;
    public String navn;

    public static MedfolgendeBarn fraFnr(String fnr) {
        MedfolgendeBarn medfolgendeBarn = new MedfolgendeBarn();
        medfolgendeBarn.fnr = fnr;
        return medfolgendeBarn;
    }
}
