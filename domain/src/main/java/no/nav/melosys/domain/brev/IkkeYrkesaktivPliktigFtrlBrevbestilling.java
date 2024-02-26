package no.nav.melosys.domain.brev;

import java.util.List;

import no.nav.melosys.domain.dokument.felles.Periode;

public class IkkeYrkesaktivPliktigFtrlBrevbestilling extends DokgenBrevbestilling {
    private List<String> land;
    private String bestemmelse;
    private String nyVurderingBakgrunn;
    private String innledningFritekst;
    private String begrunnelseFritekst;
    private String ikkeYrkesaktivOppholdType;
    private String ikkeYrkesaktivRelasjonType;
    private Periode medlemskapsperiode;

    public IkkeYrkesaktivPliktigFtrlBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private IkkeYrkesaktivPliktigFtrlBrevbestilling(Builder builder) {
        super(builder);
        this.bestemmelse = builder.bestemmelse;
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
        this.ikkeYrkesaktivOppholdType = builder.ikkeYrkesaktivOppholdType;
        this.innledningFritekst = builder.innledningFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.ikkeYrkesaktivRelasjonType = builder.ikkeYrkesaktivRelasjonType;
        this.medlemskapsperiode = builder.medlemskapsperiode;
        this.land = builder.land;
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

        public Builder(IkkeYrkesaktivPliktigFtrlBrevbestilling innvilgelseBrevbestilling) {
            super(innvilgelseBrevbestilling);
            this.bestemmelse = innvilgelseBrevbestilling.bestemmelse;
            this.nyVurderingBakgrunn = innvilgelseBrevbestilling.nyVurderingBakgrunn;
            this.innledningFritekst = innvilgelseBrevbestilling.innledningFritekst;
            this.begrunnelseFritekst = innvilgelseBrevbestilling.begrunnelseFritekst;
            this.ikkeYrkesaktivOppholdType = innvilgelseBrevbestilling.ikkeYrkesaktivOppholdType;
            this.ikkeYrkesaktivRelasjonType = innvilgelseBrevbestilling.ikkeYrkesaktivRelasjonType;
            this.medlemskapsperiode = innvilgelseBrevbestilling.medlemskapsperiode;
            this.land = innvilgelseBrevbestilling.land;
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

        public IkkeYrkesaktivPliktigFtrlBrevbestilling build() {
            return new IkkeYrkesaktivPliktigFtrlBrevbestilling(this);
        }
    }
}
