package no.nav.melosys.domain.brev;

public class MangelbrevBrevbestilling extends DokgenBrevbestilling {
    private String manglerInfoFritekst;
    private String innledningFritekst;
    private String fullmektigNavn;
    private boolean brukerSkalHaKopi;

    public MangelbrevBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private MangelbrevBrevbestilling(MangelbrevBrevbestilling.Builder builder) {
        super(builder);
        this.manglerInfoFritekst = builder.manglerInfoFritekst;
        this.innledningFritekst = builder.innledningFritekst;
        this.fullmektigNavn = builder.fullmektigNavn;
        this.brukerSkalHaKopi = builder.brukerSkalHaKopi;
    }

    public String getManglerInfoFritekst() {
        return manglerInfoFritekst;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getFullmektigNavn() {
        return fullmektigNavn;
    }

    public boolean isBrukerSkalHaKopi() {
        return brukerSkalHaKopi;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String manglerInfoFritekst;
        private String innledningFritekst;
        private String fullmektigNavn;
        private boolean brukerSkalHaKopi;

        public Builder() {
        }

        public Builder(MangelbrevBrevbestilling mangelbrevBrevbestilling) {
            super(mangelbrevBrevbestilling);
            this.manglerInfoFritekst = mangelbrevBrevbestilling.manglerInfoFritekst;
            this.innledningFritekst = mangelbrevBrevbestilling.innledningFritekst;
            this.fullmektigNavn = mangelbrevBrevbestilling.fullmektigNavn;
            this.brukerSkalHaKopi = mangelbrevBrevbestilling.brukerSkalHaKopi;
        }

        public Builder medManglerInfoFritekst(String manglerInfoFritekst) {
            this.manglerInfoFritekst = manglerInfoFritekst;
            return this;
        }

        public Builder medInnledningFritekst(String innledningFritekst) {
            this.innledningFritekst = innledningFritekst;
            return this;
        }

        public Builder medFullmektigNavn(String fullmektigNavn) {
            this.fullmektigNavn = fullmektigNavn;
            return this;
        }

        public Builder medBrukerSkalHaKopi(boolean brukerSkalHaKopi) {
            this.brukerSkalHaKopi = brukerSkalHaKopi;
            return this;
        }

        public MangelbrevBrevbestilling build() {
            return new MangelbrevBrevbestilling(this);
        }
    }
}
