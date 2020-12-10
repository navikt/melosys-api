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
        return tilBarnFraUuidFnrOgNavn(UUID.randomUUID().toString(), fnr, null);
    }

    public static MedfolgendeFamilie tilBarnFraUuidFnrOgNavn(String uuid, String fnr, String navn) {
        MedfolgendeFamilie medfolgendeFamilie = new MedfolgendeFamilie();
        medfolgendeFamilie.uuid = uuid;
        medfolgendeFamilie.fnr = fnr;
        medfolgendeFamilie.navn = navn;
        medfolgendeFamilie.relasjonsrolle = Relasjonsrolle.BARN;
        return medfolgendeFamilie;
    }

    public String getUuid() {
        return uuid;
    }

    public String getFnr() {
        return fnr;
    }

    public boolean erBarn() {
        return relasjonsrolle == Relasjonsrolle.BARN;
    }
}
