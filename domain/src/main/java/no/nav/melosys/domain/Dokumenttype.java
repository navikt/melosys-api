package no.nav.melosys.domain;

public enum Dokumenttype implements Kodeverk {
    ATTEST_A1("ATTEST_A1", "Attest A1"),
    INNVILGELSE_YRKESAKTIV("INNVILGELSE_YRKESAKTIV", "Innvilgelse yrkesaktiv"),
    MELDING_FORVENTET_SAKSBEHANDLINGSTID("MELDING_FORVENTET_SAKSBEHANDLINGSTID", "Melding om forventet sakbehandlingstid"),
    MELDING_HENLAGT_SAK("MELDING_HENLAGT_SAK", "Melding om henlagt sak"),
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
