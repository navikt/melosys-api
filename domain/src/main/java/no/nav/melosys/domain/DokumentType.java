package no.nav.melosys.domain;

import no.nav.melosys.exception.FunksjonellException;

/**
 * Dokumenttyper som kan produseres.
 */
public enum DokumentType implements Kodeverk {
    //FIXME Koder erstattes med riktig dokumenttypeID når disse er bestilt
    A1("TBC-1", "Dokument A1"),
    AVSLAG_ARBEIDSTAGER("TBC-2", "Vedtaksbrev avslag til arbeidstager"),
    AVSLAG_ARBEIDSGIVER("TBC-3", "Vedtaksbrev avslag til arbeidstager"),
    BEHANDLINGSTID("TBC-4", "Orienteringsbrev om forventet saksbehandlingstid"),
    HENLEGGELSE("TBC-5", "Henleggelse"),
    INNVILGELSE("TBC-6", "Vedtaksbrev om bekreftelse på trygderettigheter "),
    KLAGE("TBC-7", "Klageoversendelse"),
    KLAGE_AVVIST("TBC-8", "Avvisning av klage"),
    MANGLENDE_OPPL("TBC-9", "Innhente manglende opplysninger"),
    STATUS("TBC-10", "Orienteringsbrev om status i saken"),
    VEDTAK_BARN("TBC-11", "Vedtaksbrev for medfølgende barn");

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

    public static DokumentType forKode(String kode) throws FunksjonellException {
        for (DokumentType type: VALUES) {
            if (type.getKode().equals(kode)) {
                return type;
            }
        }
        throw new FunksjonellException("DokumentType med kode " + kode + " finnes ikke");
    }
}
