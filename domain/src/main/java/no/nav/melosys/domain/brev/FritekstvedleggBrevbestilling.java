package no.nav.melosys.domain.brev;

import no.nav.melosys.domain.kodeverk.Mottakerroller;

public class FritekstvedleggBrevbestilling extends DokgenBrevbestilling {
    private String fritekstvedleggTittel;
    private String fritekstvedleggTekst;
    private Mottakerroller mottakerType;

    public FritekstvedleggBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    public FritekstvedleggBrevbestilling(FritekstvedleggBrevbestilling.Builder builder) {
        super(builder);
        this.fritekstvedleggTittel = builder.fritekstvedleggTittel;
        this.fritekstvedleggTekst = builder.fritekstvedleggTekst;
        this.mottakerType = builder.mottakerType;
    }

    public String getFritekstvedleggTittel() {
        return fritekstvedleggTittel;
    }

    public String getFritekstvedleggTekst() {
        return fritekstvedleggTekst;
    }

    public Mottakerroller getMottakerType() {
        return mottakerType;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String fritekstvedleggTittel;
        private String fritekstvedleggTekst;
        private Mottakerroller mottakerType;

        public Builder() {
        }

        public Builder(FritekstvedleggBrevbestilling fritekstvedleggBrevbestilling) {
            super(fritekstvedleggBrevbestilling);
            this.fritekstvedleggTittel = fritekstvedleggBrevbestilling.fritekstvedleggTittel;
            this.fritekstvedleggTekst = fritekstvedleggBrevbestilling.fritekstvedleggTekst;
            this.mottakerType = fritekstvedleggBrevbestilling.mottakerType;
        }

        public Builder medFritekstvedleggTittel(String fritekstvedleggTittel) {
            this.fritekstvedleggTittel = fritekstvedleggTittel;
            return this;
        }

        public Builder medFritekstvedleggTekst(String fritekstvedleggTekst) {
            this.fritekstvedleggTekst = fritekstvedleggTekst;
            return this;
        }

        public Builder medMottakerType(Mottakerroller mottakerType) {
            this.mottakerType = mottakerType;
            return this;
        }

        public FritekstvedleggBrevbestilling build() {
            return new FritekstvedleggBrevbestilling(this);
        }
    }
}
