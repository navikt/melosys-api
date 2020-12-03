package no.nav.melosys.domain.behandlingsgrunnlag.soeknad;

import java.util.UUID;

public class MedfolgendeFamilie {
    public String uuid;
    public String fnr;
    public String navn;
    public Relasjonsrolle relasjonsrolle;

    public enum Relasjonsrolle {
        BARN
    }

    public static MedfolgendeFamilie tilBarnFraFnr(String fnr) {
        MedfolgendeFamilie medfolgendeFamilie = new MedfolgendeFamilie();
        medfolgendeFamilie.uuid = UUID.randomUUID().toString();
        medfolgendeFamilie.fnr = fnr;
        medfolgendeFamilie.relasjonsrolle = Relasjonsrolle.BARN;
        return medfolgendeFamilie;
    }

    public String getFnr() {
        return fnr;
    }

    public boolean erBarn() {
        return relasjonsrolle == Relasjonsrolle.BARN;
    }
}
