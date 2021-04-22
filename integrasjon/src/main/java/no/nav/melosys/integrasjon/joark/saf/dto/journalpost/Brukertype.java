package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

import no.nav.melosys.domain.arkiv.BrukerIdType;

public enum Brukertype {
    AKTOERID,
    FNR,
    ORGNR;

    public static boolean erPerson(Brukertype type) {
        return type == FNR || type == AKTOERID;
    }

    public static BrukerIdType tilDomene(Brukertype type) {
        return switch (type) {
            case FNR -> BrukerIdType.FNR;
            case AKTOERID -> BrukerIdType.AKTØR_ID;
            default -> throw new IllegalArgumentException("Støtter ikke brukertype " + type);
        };
    }
}
