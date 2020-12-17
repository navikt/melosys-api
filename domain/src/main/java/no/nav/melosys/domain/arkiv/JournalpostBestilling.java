package no.nav.melosys.domain.arkiv;

public final class JournalpostBestilling {
    private final String tittel;
    private final String brevkode;
    private final String brukerFnr;
    private final String avsenderNavn;
    private final String avsenderId;
    private final String arkivSakId;
    private final byte[] pdf;

    public JournalpostBestilling(String tittel, String brevkode, String brukerFnr, String avsenderNavn, String avsenderId, String arkivSakId, byte[] pdf) {
        this.tittel = tittel;
        this.brevkode = brevkode;
        this.brukerFnr = brukerFnr;
        this.avsenderNavn = avsenderNavn;
        this.avsenderId = avsenderId;
        this.arkivSakId = arkivSakId;
        this.pdf = pdf;
    }

    public String getTittel() {
        return tittel;
    }

    public String getBrevkode() {
        return brevkode;
    }

    public String getBrukerFnr() {
        return brukerFnr;
    }

    public String getAvsenderNavn() {
        return avsenderNavn;
    }

    public String getAvsenderId() {
        return avsenderId;
    }

    public String getArkivSakId() {
        return arkivSakId;
    }

    public byte[] getPdf() {
        return pdf;
    }
}
