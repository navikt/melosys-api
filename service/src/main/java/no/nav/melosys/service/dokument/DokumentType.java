package no.nav.melosys.service.dokument;

/**
 * Dokumenttyper som kan produseres.
 */
public enum DokumentType {
    // FIXME: Koder erstattes med riktig dokumenttypeID når disse er bestilt
    A1("TODO-11"),
    AVSLAG_ARBEIDSTAGER("TODO-12"),
    AVSLAG_ARBEIDSGIVER("TODO-13"),
    BEHANDLINGSTID("TODO-14"),
    MELDING_FORVENTET_SAKSBEHANDLINGSTID("000082"),
    HENLEGGELSE("000072"),
    INNVILGELSE("000048"),
    KLAGE("TODO-17"),
    KLAGE_AVVIST("TODO-18"),
    MELDING_MANGLENDE_OPPLYSNINGER("000074"),
    STATUS("TODO-110"),
    VEDTAK_BARN("TODO-111");

    private String kode;

    DokumentType(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
