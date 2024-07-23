package no.nav.melosys.domain.brev;

public class AvslagEftaStorbritanniaBrevbestilling extends DokgenBrevbestilling {
    private String innledningFritekstAvslagEfta;
    private String begrunnelseFritekstAvslagEfta;

    public AvslagEftaStorbritanniaBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    public AvslagEftaStorbritanniaBrevbestilling(AvslagEftaStorbritanniaBrevbestilling.Builder builder) {
        super(builder);
        this.innledningFritekstAvslagEfta = builder.innledningFritekstavslagEfta;
        this.begrunnelseFritekstAvslagEfta = builder.begrunnelseFritekstavslagEfta;
    }

    public String getInnledningFritekstAvslagEfta() {
        return innledningFritekstAvslagEfta;
    }

    public String getBegrunnelseFritekstAvslagEfta() {
        return begrunnelseFritekstAvslagEfta;
    }

    public AvslagEftaStorbritanniaBrevbestilling.Builder toBuilder() {
        return new AvslagEftaStorbritanniaBrevbestilling.Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<AvslagEftaStorbritanniaBrevbestilling.Builder> {
        private String innledningFritekstavslagEfta;
        private String begrunnelseFritekstavslagEfta;
        private String lolcat;

        public Builder() {
        }

        public Builder(AvslagEftaStorbritanniaBrevbestilling avslagEftaStorbritanniaBrevbestilling) {
            super(avslagEftaStorbritanniaBrevbestilling);
            this.innledningFritekstavslagEfta = avslagEftaStorbritanniaBrevbestilling.innledningFritekstAvslagEfta;
            this.begrunnelseFritekstavslagEfta = avslagEftaStorbritanniaBrevbestilling.begrunnelseFritekstAvslagEfta;
        }

        public AvslagEftaStorbritanniaBrevbestilling.Builder medInnledningFritekstAvslagEfta(String innledningFritekst) {
            this.innledningFritekstavslagEfta = innledningFritekst;
            return this;
        }

        public AvslagEftaStorbritanniaBrevbestilling.Builder medBegrunnelseFritekstAvslagEfta(String begrunnelseFritekst) {
            this.begrunnelseFritekstavslagEfta = begrunnelseFritekst;
            return this;
        }

        public AvslagEftaStorbritanniaBrevbestilling build() {
            return new AvslagEftaStorbritanniaBrevbestilling(this);
        }
    }
}
