package no.nav.melosys.domain.behandlingsgrunnlag.soeknad;

import org.apache.commons.lang3.StringUtils;

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

    public String getUuid() {
        return uuid;
    }

    public String getFnr() {
        return fnr;
    }

    public String getNavn() {
        return navn;
    }

    public boolean erBarn() {
        return relasjonsrolle == Relasjonsrolle.BARN;
    }

    public boolean harUuidOgNavn() {
        return StringUtils.isNotBlank(uuid) && StringUtils.isNotBlank(navn);
    }
}
