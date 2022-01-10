package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class MuligMottakerDto {
    private final String mottakerNavn;
    private final String dokumentNavn;
    private final Aktoersroller rolle;
    private final String orgnr;
    private final String aktørId;
    private final String institusjonskode;

    private MuligMottakerDto(Builder builder) {
        this.mottakerNavn = builder.mottakerNavn;
        this.dokumentNavn = builder.dokumentNavn;
        this.rolle = builder.rolle;
        this.orgnr = builder.orgnr;
        this.aktørId = builder.aktørId;
        this.institusjonskode = builder.institusjonskode;
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

    public String getInstitusjonskode() {
        return institusjonskode;
    }

    public static final class Builder {
        private String mottakerNavn;
        private String dokumentNavn;
        private Aktoersroller rolle;
        private String orgnr;
        private String aktørId;
        private String institusjonskode;

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

        public Builder medInstitusjonskode(String institusjonskode) {
            this.institusjonskode = institusjonskode;
            return this;
        }

        public MuligMottakerDto build() {
            return new MuligMottakerDto(this);
        }
    }
}
