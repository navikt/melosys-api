package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

public enum Brukertype {
    AKTOERID,
    FNR,
    ORGNR;

    public static boolean erPerson(Brukertype type) {
        return type == FNR || type == AKTOERID;
    }
}
