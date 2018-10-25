package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.Kodeverk;

public enum Dokumenttype implements Kodeverk {
    MELDING_MANGLENDE_OPPLYSNINGER("MELDING_MANGLENDE_OPPLYSNINGER", "Melding om manglende opplysninger");

    private String kode;
    private String beskrivelse;

    Dokumenttype(String kode, String beskrivelse) {
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

}
