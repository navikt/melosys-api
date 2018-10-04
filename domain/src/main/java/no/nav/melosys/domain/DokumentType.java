package no.nav.melosys.domain;

import no.nav.melosys.exception.IkkeFunnetException;

/**
 * Dokumenttyper som kan produseres.
 */
public enum DokumentType implements Kodeverk {
    //FIXME Koder erstattes med riktig dokumenttypeID når disse er bestilt
    A1("TODO-11", "Dokument A1"),
    AVSLAG_ARBEIDSTAGER("TODO-12", "Vedtaksbrev avslag til arbeidstager"),
    AVSLAG_ARBEIDSGIVER("TODO-13", "Vedtaksbrev avslag til arbeidstager"),
    BEHANDLINGSTID("TODO-14", "Orienteringsbrev om forventet saksbehandlingstid"),
    FORVALTNINGSMELDING("000082", "Melding om forventet saksbehandlingstid"),
    HENLEGGELSE("000072", "Henleggelse"),
    INNVILGELSE("000048", "Vedtaksbrev om bekreftelse på trygderettigheter "),
    KLAGE("TODO-17", "Klageoversendelse"),
    KLAGE_AVVIST("TODO-18", "Avvisning av klage"),
    MANGLENDE_OPPL("000074", "Innhente manglende opplysninger"),
    STATUS("TODO-110", "Orienteringsbrev om status i saken"),
    VEDTAK_BARN("TODO-111", "Vedtaksbrev for medfølgende barn");

    private static final DokumentType[] VALUES = values();

    private String kode;
    private String beskrivelse;

    DokumentType(String kode, String beskrivelse) {
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

    public static DokumentType forKode(String kode) throws IkkeFunnetException {
        for (DokumentType type: VALUES) {
            if (type.getKode().equals(kode)) {
                return type;
            }
        }
        throw new IkkeFunnetException("DokumentType med kode " + kode + " finnes ikke");
    }
}
