package no.nav.melosys.domain.brev;

import java.util.List;

import no.nav.melosys.domain.dokument.felles.Periode;

public class InnvilgelseFtrlIkkeYrkesaktivPliktigBrevbestilling extends DokgenBrevbestilling {

    private boolean flereLandUkjentHvilke;
    private List<String> land;
    private String bestemmelse;
    private String nyVurderingBakgrunn;
    private String innledningFritekst;
    private String begrunnelseFritekst;
    private String ikkeYrkesaktivOppholdType;
    private String ikkeYrkesaktivRelasjonType;
    private Periode medlemskapsperiode;

    public InnvilgelseFtrlIkkeYrkesaktivPliktigBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private InnvilgelseFtrlIkkeYrkesaktivPliktigBrevbestilling(Builder builder) {
        super(builder);
        this.flereLandUkjentHvilke = builder.flereLandUkjentHvilke;
        this.bestemmelse = builder.bestemmelse;
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
        this.ikkeYrkesaktivOppholdType = builder.ikkeYrkesaktivOppholdType;
        this.innledningFritekst = builder.innledningFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.ikkeYrkesaktivRelasjonType = builder.ikkeYrkesaktivRelasjonType;
        this.medlemskapsperiode = builder.medlemskapsperiode;
        this.land = builder.land;
    }

    public boolean getFlereLandUkjentHvilke() {
        return flereLandUkjentHvilke;
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

    public String getIkkeYrkesaktivOppholdType() {
        return ikkeYrkesaktivOppholdType;
    }

    public String getIkkeYrkesaktivRelasjonType() {
        return ikkeYrkesaktivRelasjonType;
    }

    public Periode getMedlemskapsperiode() {
        return medlemskapsperiode;
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
        private String bestemmelse;
        private String nyVurderingBakgrunn;
        private String innledningFritekst;
        private String begrunnelseFritekst;
        private String ikkeYrkesaktivOppholdType;
        private String ikkeYrkesaktivRelasjonType;
        private Periode medlemskapsperiode;

        public Builder() {
        }

        public Builder(InnvilgelseFtrlIkkeYrkesaktivPliktigBrevbestilling brevbestilling) {
            super(brevbestilling);
            this.flereLandUkjentHvilke = brevbestilling.flereLandUkjentHvilke;
            this.bestemmelse = brevbestilling.bestemmelse;
            this.nyVurderingBakgrunn = brevbestilling.nyVurderingBakgrunn;
            this.innledningFritekst = brevbestilling.innledningFritekst;
            this.begrunnelseFritekst = brevbestilling.begrunnelseFritekst;
            this.ikkeYrkesaktivOppholdType = brevbestilling.ikkeYrkesaktivOppholdType;
            this.ikkeYrkesaktivRelasjonType = brevbestilling.ikkeYrkesaktivRelasjonType;
            this.medlemskapsperiode = brevbestilling.medlemskapsperiode;
            this.land = brevbestilling.land;
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

        public Builder medIkkeYrkesaktivOppholdType(String ikkeYrkesaktivOppholdType) {
            this.ikkeYrkesaktivOppholdType = ikkeYrkesaktivOppholdType;
            return this;
        }

        public Builder medIkkeYrkesaktivRelasjonType(String ikkeYrkesaktivRelasjonType) {
            this.ikkeYrkesaktivRelasjonType = ikkeYrkesaktivRelasjonType;
            return this;
        }

        public Builder medMedlemskapsperiode(Periode medlemskapsperiode) {
            this.medlemskapsperiode = medlemskapsperiode;
            return this;
        }

        public Builder medLand(List<String> land) {
            this.land = land;
            return this;
        }

        public Builder medFlereLandUkjentHvilke(boolean flereLandUkjentHvilke) {
            this.flereLandUkjentHvilke = flereLandUkjentHvilke;
            return this;
        }

        public InnvilgelseFtrlIkkeYrkesaktivPliktigBrevbestilling build() {
            return new InnvilgelseFtrlIkkeYrkesaktivPliktigBrevbestilling(this);
        }
    }
}
