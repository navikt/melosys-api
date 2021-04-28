package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

import no.nav.melosys.domain.arkiv.BrukerIdType;

public enum Brukertype {
    AKTOERID,
    FNR,
    ORGNR;

    public static boolean erPerson(Brukertype type) {
        return type == FNR || type == AKTOERID;
    }

    BrukerIdType tilDomene() {
        return switch (this) {
            case FNR -> BrukerIdType.FOLKEREGISTERIDENT;
            case AKTOERID -> BrukerIdType.AKTØR_ID;
            case ORGNR -> BrukerIdType.ORGNR;
        };
    }
}
