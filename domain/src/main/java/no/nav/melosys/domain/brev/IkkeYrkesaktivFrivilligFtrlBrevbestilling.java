package no.nav.melosys.domain.brev;

import java.util.List;

import no.nav.melosys.domain.dokument.felles.Periode;

public class IkkeYrkesaktivFrivilligFtrlBrevbestilling extends DokgenBrevbestilling {

    private boolean flereLandUkjentHvilke;
    private List<String> land;
    private String trygdedekning;
    private String bestemmelse;
    private String nyVurderingBakgrunn;
    private String innledningFritekst;
    private String begrunnelseFritekst;
    private String ikkeYrkesaktivRelasjonType;
    private boolean avslåttMedlemskapsperiodeFørMottaksdatoHelsedel;
    private boolean avslåttMedlemskapsperiodeFørMottaksdatoFullDekning;

    public IkkeYrkesaktivFrivilligFtrlBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private IkkeYrkesaktivFrivilligFtrlBrevbestilling(Builder builder) {
        super(builder);
        this.flereLandUkjentHvilke = builder.flereLandUkjentHvilke;
        this.bestemmelse = builder.bestemmelse;
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
        this.innledningFritekst = builder.innledningFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.ikkeYrkesaktivRelasjonType = builder.ikkeYrkesaktivRelasjonType;
        this.land = builder.land;
        this.trygdedekning = builder.trygdedekning;
        this.avslåttMedlemskapsperiodeFørMottaksdatoHelsedel = builder.avslåttMedlemskapsperiodeFørMottaksdatoHelsedel;
        this.avslåttMedlemskapsperiodeFørMottaksdatoFullDekning = builder.avslåttMedlemskapsperiodeFørMottaksdatoFullDekning;
    }

    public boolean getFlereLandUkjentHvilke() {
        return flereLandUkjentHvilke;
    }

    public String getTrygdedekning() {
        return trygdedekning;
    }

    public String getBestemmelse() {
        return bestemmelse;
    }

    public String getNyVurderingBakgrunn() {
        return nyVurderingBakgrunn;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getIkkeYrkesaktivRelasjonType() {
        return ikkeYrkesaktivRelasjonType;
    }

    public boolean isAvslåttMedlemskapsperiodeFørMottaksdatoHelsedel() {
        return avslåttMedlemskapsperiodeFørMottaksdatoHelsedel;
    }

    public boolean isAvslåttMedlemskapsperiodeFørMottaksdatoFullDekning() {
        return avslåttMedlemskapsperiodeFørMottaksdatoFullDekning;
    }


    public List<String> getLand() {
        return land;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private boolean flereLandUkjentHvilke;
        private List<String> land;
        private String trygdedekning;
        private String bestemmelse;
        private String nyVurderingBakgrunn;
        private String innledningFritekst;
        private String begrunnelseFritekst;
        private boolean avslåttMedlemskapsperiodeFørMottaksdatoHelsedel;
        private boolean avslåttMedlemskapsperiodeFørMottaksdatoFullDekning;
        private String ikkeYrkesaktivRelasjonType;

        public Builder() {
        }

        public Builder(IkkeYrkesaktivFrivilligFtrlBrevbestilling innvilgelseBrevbestilling) {
            super(innvilgelseBrevbestilling);
            this.flereLandUkjentHvilke = innvilgelseBrevbestilling.flereLandUkjentHvilke;
            this.trygdedekning = innvilgelseBrevbestilling.trygdedekning;
            this.bestemmelse = innvilgelseBrevbestilling.bestemmelse;
            this.nyVurderingBakgrunn = innvilgelseBrevbestilling.nyVurderingBakgrunn;
            this.innledningFritekst = innvilgelseBrevbestilling.innledningFritekst;
            this.begrunnelseFritekst = innvilgelseBrevbestilling.begrunnelseFritekst;
            this.ikkeYrkesaktivRelasjonType = innvilgelseBrevbestilling.ikkeYrkesaktivRelasjonType;
            this.land = innvilgelseBrevbestilling.land;
            this.avslåttMedlemskapsperiodeFørMottaksdatoHelsedel = innvilgelseBrevbestilling.avslåttMedlemskapsperiodeFørMottaksdatoHelsedel;
            this.avslåttMedlemskapsperiodeFørMottaksdatoFullDekning = innvilgelseBrevbestilling.avslåttMedlemskapsperiodeFørMottaksdatoFullDekning;
        }

        public Builder medBestemmelse(String bestemmelse) {
            this.bestemmelse = bestemmelse;
            return this;
        }

        public Builder medNyVurderingBakgrunn(String nyVurderingBakgrunn) {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn;
            return this;
        }

        public Builder medInnledningFritekst(String innledningFritekst) {
            this.innledningFritekst = innledningFritekst;
            return this;
        }

        public Builder medBegrunnelseFritekst(String begrunnelseFritekst) {
            this.begrunnelseFritekst = begrunnelseFritekst;
            return this;
        }

        public Builder medIkkeYrkesaktivRelasjonType(String ikkeYrkesaktivRelasjonType) {
            this.ikkeYrkesaktivRelasjonType = ikkeYrkesaktivRelasjonType;
            return this;
        }

        public Builder medTrygdedekning(String trygdedekning) {
            this.trygdedekning = trygdedekning;
            return this;
        }

        public Builder medAvslåttMedlemskapsperiodeFørMottaksdatoHelsedel(boolean avslåttMedlemskapsperiodeFørMottaksdatoHelsedel) {
            this.avslåttMedlemskapsperiodeFørMottaksdatoHelsedel = avslåttMedlemskapsperiodeFørMottaksdatoHelsedel;
            return this;
        }

        public Builder medAvslåttMedlemskapsperiodeFørMottaksdatoFullDekning(boolean avslåttMedlemskapsperiodeFørMottaksdatoFullDekning) {
            this.avslåttMedlemskapsperiodeFørMottaksdatoFullDekning = avslåttMedlemskapsperiodeFørMottaksdatoFullDekning;
            return this;
        }

        public IkkeYrkesaktivFrivilligFtrlBrevbestilling.Builder medFlereLandUkjentHvilke(boolean flereLandUkjentHvilke) {
            this.flereLandUkjentHvilke = flereLandUkjentHvilke;
            return this;
        }

        public Builder medLand(List<String> land) {
            this.land = land;
            return this;
        }

        public IkkeYrkesaktivFrivilligFtrlBrevbestilling build() {
            return new IkkeYrkesaktivFrivilligFtrlBrevbestilling(this);
        }
    }
}
