package no.nav.melosys.service.brev.muligemottakere;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class MuligMottakerDto {
    private final String mottakerNavn;
    private final String dokumentNavn;
    private final Aktoersroller rolle;
    private final String orgnr;
    private final String aktørId;
    private final String institusjonId;

    private MuligMottakerDto(Builder builder) {
        this.mottakerNavn = builder.mottakerNavn;
        this.dokumentNavn = builder.dokumentNavn;
        this.rolle = builder.rolle;
        this.orgnr = builder.orgnr;
        this.aktørId = builder.aktørId;
        this.institusjonId = builder.institusjonId;
    }

    public String getMottakerNavn() {
        return mottakerNavn;
    }

    public String getDokumentNavn() {
        return dokumentNavn;
    }

    public Aktoersroller getRolle() {
        return rolle;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public String getAktørId() {
        return aktørId;
    }

    public String getInstitusjonId() {
        return institusjonId;
    }

    public static final class Builder {
        private String mottakerNavn;
        private String dokumentNavn;
        private Aktoersroller rolle;
        private String orgnr;
        private String aktørId;
        private String institusjonId;

        public Builder medMottakerNavn(String mottakerNavn) {
            this.mottakerNavn = mottakerNavn;
            return this;
        }

        public Builder medDokumentNavn(String dokumentNavn) {
            this.dokumentNavn = dokumentNavn;
            return this;
        }

        public Builder medRolle(Aktoersroller aktoerrolle) {
            this.rolle = aktoerrolle;
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

        public Builder medInstitusjonId(String institusjonId) {
            this.institusjonId = institusjonId;
            return this;
        }

        public MuligMottakerDto build() {
            return new MuligMottakerDto(this);
        }
    }
}
