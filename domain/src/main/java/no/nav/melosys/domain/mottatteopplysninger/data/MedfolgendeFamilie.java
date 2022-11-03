package no.nav.melosys.domain.mottatteopplysninger.data;

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
        // Tom constructor på grunn av de/serialisering i `MottatteOpplysningerListener`
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
            return new FoedselsNr(fnr).getFoedselsdato();
        }

        LocalDate datoMedKlartÅrstall = finnFørsteMatch(Stream.of(
            "ddMMyyyy", "dd.MM.yyyy", "dd/MM/yyyy", "dd-MM-yyyy"
        ));
        if (datoMedKlartÅrstall != null) return datoMedKlartÅrstall;

        LocalDate datoMedUklartÅrhundre = finnFørsteMatch(Stream.of(
            "ddMMyy", "dd.MM.yy", "dd/MM/yy", "dd-MM-yy"
        ));
        if (datoMedUklartÅrhundre == null) {
            throw new TekniskException("fnr: " + fnr + " kan ikke parsers til fødselsdato");
        }

        if (datoMedUklartÅrhundre.isAfter(LocalDate.now())) {
            // om en person har en dato frem i tiden velger vi å plassere han på 1900 tallet
            return datoMedUklartÅrhundre.minusYears(100);
        }
        return datoMedUklartÅrhundre;
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
