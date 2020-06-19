package no.nav.melosys.domain.inngangsvilkar;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public enum Kategori implements Kodeverk {
    TEKNISK_FEIL("Teknisk feil"),
    IKKE_STOETTET("Det er ikke implementert maskinell støtte for denne forespørselen."),
    VALIDERINGSFEIL("Ikke komplett eller inkonsistent input.");

    private String beskrivelse;

    Kategori(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return this.name();
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }
}
