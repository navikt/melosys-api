package no.nav.melosys.service.brev.brevmalliste;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class BrevAdresse {
    private final String mottakerNavn;
    private final String orgnr;
    private final List<String> adresselinjer;
    private final String postnr;
    private final String poststed;
    private final String region;
    private final String land;

    public BrevAdresse(Builder builder) {
        this.mottakerNavn = builder.mottakerNavn;
        this.orgnr = builder.orgnr;
        this.adresselinjer = builder.adresselinjer;
        this.postnr = builder.postnr;
        this.poststed = builder.poststed;
        this.region = builder.region;
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

    public String getRegion() {
        return region;
    }

    public String getLand() {
        return land;
    }

    public boolean isAdresselinjerEmpty() {
        return adresselinjer == null || adresselinjer.stream().allMatch(String::isBlank);
    }

    public boolean isPostnrEmpty() {
        return postnr == null || postnr.isBlank();
    }

    public static class Builder {
        public String mottakerNavn;
        public String orgnr;
        public List<String> adresselinjer;
        public String postnr;
        public String poststed;
        public String region;
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
            this.adresselinjer = adresselinjer != null
                ? adresselinjer.stream().filter(StringUtils::isNotEmpty).collect(Collectors.toList())
                : null;
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

        public Builder medRegion(String region) {
            this.region = region;
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
