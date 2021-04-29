package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

import no.nav.melosys.domain.kodeverk.Avsendertyper;

public enum AvsenderMottakerType {
    FNR,
    ORGNR,
    HPRNR,
    UTL_ORG,
    NULL,
    UKJENT;

    public static Avsendertyper tilDomene(AvsenderMottakerType avsenderMottakerType) {
        return switch (avsenderMottakerType) {
            case FNR -> Avsendertyper.PERSON;
            case ORGNR -> Avsendertyper.ORGANISASJON;
            case UTL_ORG -> Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET;
            default -> throw new IllegalArgumentException("Støtter ikke avsendertype " + avsenderMottakerType);
        };
    }
}
