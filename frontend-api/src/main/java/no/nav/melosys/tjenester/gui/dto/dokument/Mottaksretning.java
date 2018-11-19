package no.nav.melosys.tjenester.gui.dto.dokument;

import no.nav.melosys.domain.Kodeverk;
import no.nav.melosys.domain.arkiv.Journalposttype;

public enum Mottaksretning implements Kodeverk {

    INN("INN", "Inngående dokument"),
    UT("UT", "Utgående dokument"),
    NOTAT("NOTAT", "Internt notat");

    private String kode;
    private String beskrivelse;

    Mottaksretning(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }

    public static Mottaksretning av(Journalposttype journalposttype) {
        Mottaksretning retning;
        switch (journalposttype) {
            case INN: retning = Mottaksretning.INN;
                break;
            case UT: retning = Mottaksretning.UT;
                break;
            case NOTAT: retning = Mottaksretning.NOTAT;
                break;
            default:
                throw new IllegalArgumentException("Journalposttype " + journalposttype.getKode() + " støttes ikke.");
        }
        return retning;
    }
}
