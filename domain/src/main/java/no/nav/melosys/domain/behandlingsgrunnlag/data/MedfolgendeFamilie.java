package no.nav.melosys.domain.behandlingsgrunnlag.data;

import java.util.UUID;

import no.nav.commons.foedselsnummer.FoedselsNr;
import no.nav.melosys.exception.FunksjonellException;

public class MedfolgendeFamilie {
    private String uuid;
    private String fnr;
    private String navn;
    private Relasjonsrolle relasjonsrolle;

    private MedfolgendeFamilie() {
    }

    private MedfolgendeFamilie(String uuid, String fnr, String navn, Relasjonsrolle relasjonsrolle) {
        this.uuid = uuid;
        this.fnr = fnr;
        this.navn = navn;
        this.relasjonsrolle = relasjonsrolle;
    }

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
        return new MedfolgendeFamilie(uuid, fnr, navn, rolle);
    }

    public String getUuid() {
        return uuid;
    }

    public String getNavn() {
        return navn;
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

    public IdentType utledIdentType() {
        if (fnr.length() < 11 && fnr.contains(".")) {
            return IdentType.DATO;
        }

        FoedselsNr foedselsNr = new FoedselsNr(fnr);
        if (!foedselsNr.getGyldigeKontrollsiffer()) {
            throw new FunksjonellException("Ikke et gyldig fødselsnummer, kontrollsiffer er ikke riktig");
        }

        return foedselsNr.getDNummer() ? IdentType.DNR : IdentType.FNR;
    }
}
