package no.nav.melosys.domain.brev;

public class InnvilgelseEftaStorbritanniaBrevbestilling extends DokgenBrevbestilling {
    private String innledningFritekst;
    private String begrunnelseFritekst;
    private String innvilgelseFritekst;
    private String nyVurderingBakgrunn;

    public InnvilgelseEftaStorbritanniaBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private InnvilgelseEftaStorbritanniaBrevbestilling(InnvilgelseEftaStorbritanniaBrevbestilling.Builder builder) {
        super(builder);
        this.innledningFritekst = builder.innledningFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.innvilgelseFritekst = builder.innvilgelseFritekst;
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getInnvilgelseFritekst() {
        return innvilgelseFritekst;
    }

    public String getNyVurderingBakgrunn() {
        return nyVurderingBakgrunn;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String innledningFritekst;
        private String begrunnelseFritekst;
        private String innvilgelseFritekst;
        private String nyVurderingBakgrunn;

        public Builder() {
        }

        public Builder(InnvilgelseEftaStorbritanniaBrevbestilling innvilgelseBrevbestilling) {
            super(innvilgelseBrevbestilling);
            this.innledningFritekst = innvilgelseBrevbestilling.innledningFritekst;
            this.begrunnelseFritekst = innvilgelseBrevbestilling.begrunnelseFritekst;
            this.innvilgelseFritekst = innvilgelseBrevbestilling.innvilgelseFritekst;
            this.nyVurderingBakgrunn = innvilgelseBrevbestilling.nyVurderingBakgrunn;
        }

        public Builder medInnledningFritekst(String innledningFritekst) {
            this.innledningFritekst = innledningFritekst;
            return this;
        }

        public Builder medBegrunnelseFritekst(String begrunnelseFritekst) {
            this.begrunnelseFritekst = begrunnelseFritekst;
            return this;
        }

        public Builder medInnvilgelseFritekst(String innvilgelseFritekst) {
            this.innvilgelseFritekst = innvilgelseFritekst;
            return this;
        }

        public Builder medNyVurderingBakgrunn(String nyVurderingBakgrunn) {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn;
            return this;
        }

        public InnvilgelseEftaStorbritanniaBrevbestilling build() {
            return new InnvilgelseEftaStorbritanniaBrevbestilling(this);
        }
    }
}
