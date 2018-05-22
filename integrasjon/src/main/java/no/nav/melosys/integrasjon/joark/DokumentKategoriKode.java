package no.nav.melosys.integrasjon.joark;

import no.nav.melosys.domain.Kodeverk;

/**
 * Enum for Joark koder i K_KATEGORI_T.
 */
public enum DokumentKategoriKode implements Kodeverk {
    B("B", "Brev"),
    ELEKTRONISK_DIALOG("ELEKTRONISK_DIALOG", "Elektronisk dialog"),
    EP("EP", "E-post"),
    ES("ES", "Elektronisk skjema"),
    E_BLANKETT("E_BLANKETT", "E-blankett"),
    F("F", "Faktura"),
    FORVALTNINGSNOTAT("FORVALTNINGSNOTAT", "Forvaltningsnotat"),
    IB("IB", "Infobrev"),
    IS("IS", "Ikke tolkbart skjema"),
    KA("KA", "Klage eller anke"),
    KD("KD", "Konvertert fra elektronisk arkiv"),
    KM("KM", "Konvertert fra papirarkiv (skannet)"),
    KS("KS", "Konverterte data fra system"),
    PUBL_BLANKETT_EOS("PUBL_BLANKETT_EOS", "Publikumsblankett EØS"),
    SED("SED", "SED"),
    SOK("SOK", "Søknad"),
    SYS_SED("SYS_SED", "SystemSED"),
    TS("TS", "Tolkbart skjema"),
    VB("VB", "Vedtaksbrev");

    private String kode;
    private String beskrivelse;

    DokumentKategoriKode(String kode, String beskrivelse) {
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
