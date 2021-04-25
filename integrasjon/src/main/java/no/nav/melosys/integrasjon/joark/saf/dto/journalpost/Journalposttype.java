package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

public enum Journalposttype {
    I,
    U,
    N;

    public static no.nav.melosys.domain.arkiv.Journalposttype tilDomene(Journalposttype journalposttype) {
        return switch (journalposttype) {
            case I -> no.nav.melosys.domain.arkiv.Journalposttype.INN;
            case U -> no.nav.melosys.domain.arkiv.Journalposttype.UT;
            case N -> no.nav.melosys.domain.arkiv.Journalposttype.NOTAT;
        };
    }
}
