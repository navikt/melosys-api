package no.nav.melosys.domain.arkiv;

import java.util.Objects;

// JournalpostID + dokumentID identifiserer et dokument i arkivet.
public class DokumentReferanse {
    private final String journalpostID;
    private final String dokumentID;

    public DokumentReferanse(String journalpostId, String dokumentId) {
        this.journalpostID = journalpostId;
        this.dokumentID = dokumentId;
    }

    public String getJournalpostID() {
        return journalpostID;
    }

    public String getDokumentID() {
        return dokumentID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DokumentReferanse that = (DokumentReferanse) o;
        return Objects.equals(journalpostID, that.journalpostID) && Objects.equals(dokumentID, that.dokumentID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostID, dokumentID);
    }
}
