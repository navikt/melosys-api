package no.nav.melosys.domain.msm;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class AltinnDokument {
    private final String soknadID;
    private final String dokumentID;
    private final String tittel;
    private final AltinnDokumentType dokumentType;
    private final String innhold;


    public AltinnDokument(
        @JsonProperty("soknadID") String soknadID,
        @JsonProperty("dokumentID") String dokumentID,
        @JsonProperty("tittel") String tittel,
        @JsonProperty("dokumentType") AltinnDokumentType dokumentType,
        @JsonProperty("innhold") String innhold) {
        this.soknadID = soknadID;
        this.dokumentID = dokumentID;
        this.tittel = tittel;
        this.dokumentType = dokumentType;
        this.innhold = innhold;
    }

    public String getSoknadID() {
        return soknadID;
    }

    public String getDokumentID() {
        return dokumentID;
    }

    public String getTittel() {
        return tittel;
    }

    public AltinnDokumentType getDokumentType() {
        return dokumentType;
    }

    public String getInnhold() {
        return innhold;
    }

    public boolean erSøknad() {
        return dokumentType == AltinnDokumentType.SOKNAD;
    }

    public enum AltinnDokumentType {
        SOKNAD, FULLMAKT
    }
}
