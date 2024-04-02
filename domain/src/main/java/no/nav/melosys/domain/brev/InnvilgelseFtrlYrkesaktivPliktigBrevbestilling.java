package no.nav.melosys.domain.brev;

public class InnvilgelseFtrlYrkesaktivPliktigBrevbestilling extends DokgenBrevbestilling {

    private String innvilgelseNyVurderingBakgrunn;
    private String innledningFritekst;
    private String begrunnelseFritekst;
    private String trygdeavgiftFritekst;

    public InnvilgelseFtrlYrkesaktivPliktigBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private InnvilgelseFtrlYrkesaktivPliktigBrevbestilling(Builder builder) {
        super(builder);
        this.innvilgelseNyVurderingBakgrunn = builder.nyVurderingBakgrunn;
        this.innledningFritekst = builder.innledningFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.trygdeavgiftFritekst = builder.trygdeavgiftFritekst;
    }

    public String getTrygdeavgiftFritekst() {
        return trygdeavgiftFritekst;
    }

    public void setTrygdeavgiftFritekst(String trygdeavgiftFritekst) {
        this.trygdeavgiftFritekst = trygdeavgiftFritekst;
    }

    public String getInnvilgelseNyVurderingBakgrunn() {
        return innvilgelseNyVurderingBakgrunn;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String nyVurderingBakgrunn;
        private String innledningFritekst;
        private String begrunnelseFritekst;
        private String trygdeavgiftFritekst;

        public Builder() {
        }

        public Builder(InnvilgelseFtrlYrkesaktivPliktigBrevbestilling brevbestilling) {
            super(brevbestilling);
            this.nyVurderingBakgrunn = brevbestilling.innvilgelseNyVurderingBakgrunn;
            this.innledningFritekst = brevbestilling.innledningFritekst;
            this.begrunnelseFritekst = brevbestilling.begrunnelseFritekst;
            this.trygdeavgiftFritekst = brevbestilling.trygdeavgiftFritekst;
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

        @Override
        public InnvilgelseFtrlYrkesaktivPliktigBrevbestilling build() {
            return new InnvilgelseFtrlYrkesaktivPliktigBrevbestilling(this);
        }
    }
}
