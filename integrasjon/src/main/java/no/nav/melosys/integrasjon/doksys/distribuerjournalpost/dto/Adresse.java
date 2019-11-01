package no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto;

public final class Adresse {

    private String adresseType;
    private String adresselinje1;
    private String adresselinje2;
    private String adresselinje3;
    private String postnummer;
    private String poststed;
    private String land;

    public Adresse(String adresseType, String adresselinje1, String adresselinje2, String adresselinje3, String postnummer, String poststed, String land) {
        this.adresseType = adresseType;
        this.adresselinje1 = adresselinje1;
        this.adresselinje2 = adresselinje2;
        this.adresselinje3 = adresselinje3;
        this.postnummer = postnummer;
        this.poststed = poststed;
        this.land = land;
    }

    public Adresse() {
    }

    public static AdresseBuilder builder() {
        return new AdresseBuilder();
    }

    public String getAdresseType() {
        return adresseType;
    }

    public String getAdresselinje1() {
        return adresselinje1;
    }

    public String getAdresselinje2() {
        return adresselinje2;
    }

    public String getAdresselinje3() {
        return adresselinje3;
    }

    public String getPostnummer() {
        return postnummer;
    }

    public String getPoststed() {
        return poststed;
    }

    public String getLand() {
        return land;
    }

    public static class AdresseBuilder {
        private String adressetype;
        private String adresselinje1;
        private String adresselinje2;
        private String adresselinje3;
        private String postnummer;
        private String poststed;
        private String land;

        AdresseBuilder() {
        }

        public Adresse.AdresseBuilder adressetype(String adressetype) {
            this.adressetype = adressetype;
            return this;
        }

        public Adresse.AdresseBuilder adresselinje1(String adresselinje1) {
            this.adresselinje1 = adresselinje1;
            return this;
        }

        public Adresse.AdresseBuilder adresselinje2(String adresselinje2) {
            this.adresselinje2 = adresselinje2;
            return this;
        }

        public Adresse.AdresseBuilder adresselinje3(String adresselinje3) {
            this.adresselinje3 = adresselinje3;
            return this;
        }

        public Adresse.AdresseBuilder postnummer(String postnummer) {
            this.postnummer = postnummer;
            return this;
        }

        public Adresse.AdresseBuilder poststed(String poststed) {
            this.poststed = poststed;
            return this;
        }

        public Adresse.AdresseBuilder land(String land) {
            this.land = land;
            return this;
        }

        public Adresse build() {
            return new Adresse(adressetype, adresselinje1, adresselinje2, adresselinje3, postnummer, poststed, land);
        }
    }
}
