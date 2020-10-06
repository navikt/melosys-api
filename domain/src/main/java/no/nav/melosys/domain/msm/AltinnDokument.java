package no.nav.melosys.domain.msm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public final class AltinnDokument {
    private final String soknadID;
    private final String dokumentID;
    private final String tittel;
    private final AltinnDokumentType dokumentType;
    private final String innhold;
    private final Instant innsendtTidspunkt;

    public AltinnDokument(
        @JsonProperty("soknadID") String soknadID,
        @JsonProperty("dokumentID") String dokumentID,
        @JsonProperty("tittel") String tittel,
        @JsonProperty("dokumentType") String dokumentType,
        @JsonProperty("innhold") String innhold,
        @JsonProperty("innsendtTidspunkt") Instant innsendtTidspunkt) {
        this.soknadID = soknadID;
        this.dokumentID = dokumentID;
        this.tittel = tittel;
        this.dokumentType = AltinnDokumentType.valueOf(dokumentType.toUpperCase());
        this.innhold = innhold;
        this.innsendtTidspunkt = innsendtTidspunkt;
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

    public Instant getInnsendtTidspunkt() {
        return innsendtTidspunkt;
    }

    public boolean erSøknad() {
        return dokumentType == AltinnDokumentType.SOKNAD;
    }

    public enum AltinnDokumentType {
        SOKNAD, FULLMAKT
    }
}
