package no.nav.melosys.service.brev;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BrevAdresse {
    public final String mottakerNavn;
    public final String orgnr;
    public final List<String> adresselinjer;
    public final String postnr;
    public final String poststed;
    public final String land;

    public BrevAdresse(Builder builder) {
        this.mottakerNavn = builder.mottakerNavn;
        this.orgnr = builder.orgnr;
        this.adresselinjer = builder.adresselinjer;
        this.postnr = builder.postnr;
        this.poststed = builder.poststed;
        this.land = builder.land;
    }

    public String getMottakerNavn() {
        return mottakerNavn;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public List<String> getAdresselinjer() {
        return adresselinjer;
    }

    public String getPostnr() {
        return postnr;
    }

    public String getPoststed() {
        return poststed;
    }

    public String getLand() {
        return land;
    }

    public boolean isAdresselinjerEmpty() {
        return adresselinjer.stream().allMatch(String::isBlank);
    }

    public static class Builder {
        public String mottakerNavn;
        public String orgnr;
        public List<String> adresselinjer;
        public String postnr;
        public String poststed;
        public String land;

        public Builder medMottakerNavn(String mottakerNavn) {
            this.mottakerNavn = mottakerNavn;
            return this;
        }

        public Builder medOrgnr(String orgnr) {
            this.orgnr = orgnr;
            return this;
        }

        public Builder medAdresselinjer(List<String> adresselinjer) {
            this.adresselinjer = adresselinjer.stream().filter(Objects::nonNull).collect(Collectors.toList());
            return this;
        }

        public Builder medPostnr(String postnr) {
            this.postnr = postnr;
            return this;
        }

        public Builder medPoststed(String poststed) {
            this.poststed = poststed;
            return this;
        }

        public Builder medLand(String land) {
            this.land = land;
            return this;
        }

        public BrevAdresse build() {
            return new BrevAdresse(this);
        }
    }
}
