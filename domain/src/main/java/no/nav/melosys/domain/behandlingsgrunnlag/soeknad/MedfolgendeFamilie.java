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
        return tilBarnFraUuidFnrOgNavn(UUID.randomUUID().toString(), fnr, navn);
    }

    public static MedfolgendeFamilie tilEktefelleSamboerFraFnrOgNavn(String fnr, String navn) {
        return tilEktefelleSamboerFraUuidFnrOgNavn(UUID.randomUUID().toString(), fnr, navn);
    }

    public static MedfolgendeFamilie tilBarnFraUuidFnrOgNavn(String uuid, String fnr, String navn) {
        MedfolgendeFamilie medfolgendeFamilie = new MedfolgendeFamilie();
        medfolgendeFamilie.uuid = uuid;
        medfolgendeFamilie.fnr = fnr;
        medfolgendeFamilie.navn = navn;
        medfolgendeFamilie.relasjonsrolle = Relasjonsrolle.BARN;
        return medfolgendeFamilie;
    }

    public static MedfolgendeFamilie tilEktefelleSamboerFraUuidFnrOgNavn(String uuid, String fnr, String navn) {
        MedfolgendeFamilie medfolgendeFamilie = new MedfolgendeFamilie();
        medfolgendeFamilie.uuid = uuid;
        medfolgendeFamilie.fnr = fnr;
        medfolgendeFamilie.navn = navn;
        medfolgendeFamilie.relasjonsrolle = Relasjonsrolle.EKTEFELLE_SAMBOER;
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

    public boolean erEktefelleSamboer() {
        return relasjonsrolle == Relasjonsrolle.EKTEFELLE_SAMBOER;
    }
}
