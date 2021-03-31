package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class MuligMottakerDto {
    private final String mottakerNavn;
    private final String dokumentNavn;
    private final Aktoersroller aktoerrolle;
    private final String orgnr;
    private final String aktørId;

    private MuligMottakerDto(Builder builder) {
        this.mottakerNavn = builder.mottakerNavn;
        this.dokumentNavn = builder.dokumentNavn;
        this.aktoerrolle = builder.aktoerrolle;
        this.orgnr = builder.orgnr;
        this.aktørId = builder.aktørId;
    }

    public String getMottakerNavn() {
        return mottakerNavn;
    }

    public String getDokumentNavn() {
        return dokumentNavn;
    }

    public Aktoersroller getAktoerrolle() {
        return aktoerrolle;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public String getAktørId() {
        return aktørId;
    }

    public static final class Builder {
        private String mottakerNavn;
        private String dokumentNavn;
        private Aktoersroller aktoerrolle;
        private String orgnr;
        private String aktørId;

        public Builder medMottakerNavn(String mottakerNavn) {
            this.mottakerNavn = mottakerNavn;
            return this;
        }

        public Builder medDokumentNavn(String dokumentNavn) {
            this.dokumentNavn = dokumentNavn;
            return this;
        }

        public Builder medAktoerrolle(Aktoersroller aktoerrolle) {
            this.aktoerrolle = aktoerrolle;
            return this;
        }

        public Builder medOrgnr(String orgnr) {
            this.orgnr = orgnr;
            return this;
        }

        public Builder medAktørId(String aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public MuligMottakerDto build() {
            return new MuligMottakerDto(this);
        }
    }
}
