package no.nav.melosys.domain.brev;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;

public class FritekstbrevBrevbestilling extends DokgenBrevbestilling {
    private String fritekstTittel;
    private String fritekst;
    private boolean kontaktopplysninger;
    private String navnFullmektig;
    private boolean brukerSkalHaKopi;
    private Aktoersroller mottakerType;
    private Representerer representerer;
    private String dokumentTittel;

    public FritekstbrevBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    public FritekstbrevBrevbestilling(FritekstbrevBrevbestilling.Builder builder) {
        super(builder);
        this.fritekstTittel = builder.fritekstTittel;
        this.fritekst = builder.fritekst;
        this.kontaktopplysninger = builder.kontaktopplysninger;
        this.navnFullmektig = builder.navnFullmektig;
        this.brukerSkalHaKopi = builder.brukerSkalHaKopi;
        this.mottakerType = builder.mottakerType;
        this.representerer = builder.representerer;
        this.dokumentTittel = builder.dokumentTittel;
    }

    public String getFritekstTittel() {
        return fritekstTittel;
    }

    public String getFritekst() {
        return fritekst;
    }

    public boolean isKontaktopplysninger() {
        return kontaktopplysninger;
    }

    public String getNavnFullmektig() {
        return navnFullmektig;
    }

    public boolean isBrukerSkalHaKopi() {
        return brukerSkalHaKopi;
    }

    public String getDokumentTittel() {
        return dokumentTittel;
    }

    public Aktoersroller getMottakerType() {
        return mottakerType;
    }

    public Representerer getRepresenterer() {
        return representerer;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String fritekstTittel;
        private String fritekst;
        private boolean kontaktopplysninger;
        private String navnFullmektig;
        private boolean brukerSkalHaKopi;
        private Aktoersroller mottakerType;
        private Representerer representerer;
        private String dokumentTittel;

        public Builder() {
        }

        public Builder(FritekstbrevBrevbestilling fritekstbrevBrevbestilling) {
            super(fritekstbrevBrevbestilling);
            this.fritekstTittel = fritekstbrevBrevbestilling.fritekstTittel;
            this.fritekst = fritekstbrevBrevbestilling.fritekst;
            this.kontaktopplysninger = fritekstbrevBrevbestilling.kontaktopplysninger;
            this.navnFullmektig = fritekstbrevBrevbestilling.navnFullmektig;
            this.brukerSkalHaKopi = fritekstbrevBrevbestilling.brukerSkalHaKopi;
            this.mottakerType = fritekstbrevBrevbestilling.mottakerType;
            this.representerer = fritekstbrevBrevbestilling.representerer;
            this.dokumentTittel = fritekstbrevBrevbestilling.dokumentTittel;
        }

        public Builder medFritekstTittel(String fritekstTittel) {
            this.fritekstTittel = fritekstTittel;
            return this;
        }

        public Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }

        public Builder medKontaktopplysninger(boolean kontaktopplysninger) {
            this.kontaktopplysninger = kontaktopplysninger;
            return this;
        }

        public Builder medNavnFullmektig(String navnFullmektig) {
            this.navnFullmektig = navnFullmektig;
            return this;
        }

        public Builder medBrukerSkalHaKopi(boolean brukerSkalHaKopi) {
            this.brukerSkalHaKopi = brukerSkalHaKopi;
            return this;
        }

        public Builder medDokumentTittel(String dokumentTittel) {
            this.dokumentTittel = dokumentTittel;
            return this;
        }

        public Builder medMottakerType(Aktoersroller mottakerType) {
            this.mottakerType = mottakerType;
            return this;
        }

        public Builder medRepresenterer(Representerer representerer) {
            this.representerer = representerer;
            return this;
        }

        public FritekstbrevBrevbestilling build() {
            return new FritekstbrevBrevbestilling(this);
        }
    }
}
