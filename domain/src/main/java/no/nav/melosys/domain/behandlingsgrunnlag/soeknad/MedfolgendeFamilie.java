package no.nav.melosys.domain.behandlingsgrunnlag.soeknad;

import java.util.UUID;

public class MedfolgendeFamilie {
    public String uuid;
    public String fnr;
    public String navn;
    public Relasjonsrolle relasjonsrolle;

    public enum Relasjonsrolle {
        BARN, EKTEFELLE_SAMBOER
    }

    public static MedfolgendeFamilie tilBarnFraFnrOgNavn(String fnr, String navn) {
        return tilMedfolgendeFamilie(UUID.randomUUID().toString(), fnr, navn, Relasjonsrolle.BARN);
    }

    public static MedfolgendeFamilie tilEktefelleSamboerFraFnrOgNavn(String fnr, String navn) {
        return tilMedfolgendeFamilie(UUID.randomUUID().toString(), fnr, navn, Relasjonsrolle.EKTEFELLE_SAMBOER);
    }

    public static MedfolgendeFamilie tilMedfolgendeFamilie(String uuid, String fnr, String navn, Relasjonsrolle rolle) {
        MedfolgendeFamilie medfolgendeFamilie = new MedfolgendeFamilie();
        medfolgendeFamilie.uuid = uuid;
        medfolgendeFamilie.fnr = fnr;
        medfolgendeFamilie.navn = navn;
        medfolgendeFamilie.relasjonsrolle = rolle;
        return medfolgendeFamilie;
    }

    public String getUuid() {
        return uuid;
    }

    public String getFnr() {
        return fnr;
    }

    public Relasjonsrolle getRelasjonsrolle() {
        return relasjonsrolle;
    }

    public boolean erBarn() {
        return relasjonsrolle == Relasjonsrolle.BARN;
    }

    public boolean erEktefelleSamboer() {
        return relasjonsrolle == Relasjonsrolle.EKTEFELLE_SAMBOER;
    }
}
