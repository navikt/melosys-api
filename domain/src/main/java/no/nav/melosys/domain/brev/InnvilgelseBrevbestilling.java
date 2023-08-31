package no.nav.melosys.domain.brev;

public class InnvilgelseBrevbestilling extends DokgenBrevbestilling {
    private String innledningFritekst;
    private String begrunnelseFritekst;
    private String ektefelleFritekst;
    private String barnFritekst;
    private boolean virksomhetArbeidsgiverSkalHaKopi;
    private String nyVurderingBakgrunn;

    public InnvilgelseBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    protected InnvilgelseBrevbestilling(InnvilgelseBrevbestilling.Builder builder) {
        super(builder);
        this.innledningFritekst = builder.innledningFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.ektefelleFritekst = builder.ektefelleFritekst;
        this.barnFritekst = builder.barnFritekst;
        this.virksomhetArbeidsgiverSkalHaKopi = builder.virksomhetArbeidsgiverSkalHaKopi;
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getEktefelleFritekst() {
        return ektefelleFritekst;
    }

    public String getBarnFritekst() {
        return barnFritekst;
    }

    public boolean isVirksomhetArbeidsgiverSkalHaKopi() {
        return virksomhetArbeidsgiverSkalHaKopi;
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
        private String ektefelleFritekst;
        private String barnFritekst;
        private boolean virksomhetArbeidsgiverSkalHaKopi;
        private String nyVurderingBakgrunn;

        public Builder() {
        }

        public Builder(InnvilgelseBrevbestilling innvilgelseBrevbestilling) {
            super(innvilgelseBrevbestilling);
            this.innledningFritekst = innvilgelseBrevbestilling.innledningFritekst;
            this.begrunnelseFritekst = innvilgelseBrevbestilling.begrunnelseFritekst;
            this.ektefelleFritekst = innvilgelseBrevbestilling.ektefelleFritekst;
            this.barnFritekst = innvilgelseBrevbestilling.barnFritekst;
            this.virksomhetArbeidsgiverSkalHaKopi = innvilgelseBrevbestilling.virksomhetArbeidsgiverSkalHaKopi;
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

        public Builder medEktefelleFritekst(String ektefelleFritekst) {
            this.ektefelleFritekst = ektefelleFritekst;
            return this;
        }

        public Builder medBarnFritekst(String barnFritekst) {
            this.barnFritekst = barnFritekst;
            return this;
        }

        public Builder medVirksomhetArbeidsgiverSkalHaKopi(boolean virksomhetArbeidsgiverSkalHaKopi) {
            this.virksomhetArbeidsgiverSkalHaKopi = virksomhetArbeidsgiverSkalHaKopi;
            return this;
        }

        public Builder medNyVurderingBakgrunn(String nyVurderingBakgrunn) {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn;
            return this;
        }

        public InnvilgelseBrevbestilling build() {
            return new InnvilgelseBrevbestilling(this);
        }
    }
}
