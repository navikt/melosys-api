package no.nav.melosys.domain.brev;

public class AvslagEftaStorbritanniaBrevbestilling extends DokgenBrevbestilling {

    private String innledningFritekst;
    private String begrunnelseFritekst;

    public AvslagEftaStorbritanniaBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    public AvslagEftaStorbritanniaBrevbestilling(AvslagEftaStorbritanniaBrevbestilling.Builder builder) {
        super(builder);
        this.innledningFritekst = builder.innledningFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public AvslagEftaStorbritanniaBrevbestilling.Builder toBuilder() {
        return new AvslagEftaStorbritanniaBrevbestilling.Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<AvslagEftaStorbritanniaBrevbestilling.Builder> {
        private String innledningFritekst;
        private String begrunnelseFritekst;

        public Builder() {
        }

        public Builder(AvslagEftaStorbritanniaBrevbestilling fritekstbrevBrevbestilling) {
            super(fritekstbrevBrevbestilling);
            this.innledningFritekst = fritekstbrevBrevbestilling.innledningFritekst;
            this.begrunnelseFritekst = fritekstbrevBrevbestilling.begrunnelseFritekst;
        }

        public AvslagEftaStorbritanniaBrevbestilling.Builder medInnledningFritekst(String innledningFritekst) {
            this.innledningFritekst = innledningFritekst;
            return this;
        }

        public AvslagEftaStorbritanniaBrevbestilling.Builder medBegrunnelseFritekst(String begrunnelseFritekst) {
            this.begrunnelseFritekst = begrunnelseFritekst;
            return this;
        }

        public AvslagEftaStorbritanniaBrevbestilling build() {
            return new AvslagEftaStorbritanniaBrevbestilling(this);
        }
    }
}
