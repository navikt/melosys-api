package no.nav.melosys.domain.behandlingsgrunnlag.data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import no.nav.commons.foedselsnummer.FoedselsNr;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;

public class MedfolgendeFamilie {
    private String uuid;
    private String fnr;
    private String navn;
    private Relasjonsrolle relasjonsrolle;

    public MedfolgendeFamilie() {
        // Tom constructor på grunn av de/serialisering i `BehandlingsgrunnlagListener`
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
        if (fnr.length() < 11) {
            return IdentType.DATO;
        }

        FoedselsNr foedselsNr = new FoedselsNr(fnr);
        if (!foedselsNr.getGyldigeKontrollsiffer()) {
            throw new FunksjonellException(fnr + " er ikke et gyldig fødselsnummer, kontrollsiffer er ikke riktig");
        }

        return foedselsNr.getDNummer() ? IdentType.DNR : IdentType.FNR;
    }

    public LocalDate datoFraFnr() {
        if (utledIdentType() != IdentType.DATO) {
            throw new TekniskException("Kan bare parse dato når IdentType er DATO");
        }
        LocalDate happyCase = finnFørsteMatch(Stream.of(
            "ddMMyyyy", "dd.MM.yyyy", "dd/MM/yyyy", "dd-MM-yyyy"
        ));
        if (happyCase != null) return happyCase;

        LocalDate dateMedUklartÅrhundre = finnFørsteMatch(Stream.of(
            "ddMMyy", "dd.MM.yy", "dd/MM/yy", "dd-MM-yy"
        ));
        if (dateMedUklartÅrhundre == null) return null;

        if (dateMedUklartÅrhundre.isAfter(LocalDate.now())) {
            // om en person har en dato frem i tiden velger vi å plassere han på 1900 tallet
            return dateMedUklartÅrhundre.minusYears(100);
        }
        return dateMedUklartÅrhundre;
    }

    private LocalDate finnFørsteMatch(Stream<String> stream) {
        return stream.map(this::tryParseFnr)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private LocalDate tryParseFnr(String pattern) {
        try {
            return LocalDate.parse(fnr, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
