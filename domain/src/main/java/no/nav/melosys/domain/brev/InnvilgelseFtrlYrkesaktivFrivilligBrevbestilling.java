package no.nav.melosys.domain.brev;

public class InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling extends DokgenBrevbestilling {
    private String nyVurderingBakgrunn;
    private String innledningFritekst;
    private String begrunnelseFritekst;
    private String trygdeavgiftFritekst;

    public InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling(Builder builder) {
        super(builder);
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
        this.innledningFritekst = builder.innledningFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.trygdeavgiftFritekst = builder.trygdeavgiftFritekst;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getTrygdeavgiftFritekst() {
        return trygdeavgiftFritekst;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public String getNyVurderingBakgrunn() {
        return nyVurderingBakgrunn;
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String nyVurderingBakgrunn;
        private String innledningFritekst;
        private String begrunnelseFritekst;
        private String trygdeavgiftFritekst;

        public Builder() {
        }

        public Builder(InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling brevbestilling) {
            super(brevbestilling);
            this.nyVurderingBakgrunn = brevbestilling.nyVurderingBakgrunn;
            this.innledningFritekst = brevbestilling.innledningFritekst;
            this.begrunnelseFritekst = brevbestilling.begrunnelseFritekst;
            this.trygdeavgiftFritekst = brevbestilling.trygdeavgiftFritekst;
        }

        public Builder medInnledningFritekst(String innledningFritekst) {
            this.innledningFritekst = innledningFritekst;
            return this;
        }

        public Builder medBegrunnelseFritekst(String begrunnelseFritekst) {
            this.begrunnelseFritekst = begrunnelseFritekst;
            return this;
        }

        public Builder medTrygdeavgiftFritekst(String trygdeavgiftFritekst) {
            this.trygdeavgiftFritekst = trygdeavgiftFritekst;
            return this;
        }

        public Builder medNyVurderingBakgrunn(String nyVurderingBakgrunn) {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn;
            return this;
        }

        public InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling build() {
            return new InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling(this);
        }
    }
}
