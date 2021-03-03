package no.nav.melosys.service.dokument.brev;

import java.util.Objects;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class BrevbestillingDto {

    private Aktoersroller mottaker;
    private String orgNr;
    private String innledningFritekst;
    private String manglerFritekst;
    private String kontaktperson;
    private boolean sendKopi;

    /**
     * @deprecated Benyttes i doksys, kommer til å bli erstattet av dokgen-variabel
     */
    @Deprecated
    private String fritekst;

    /**
     * @deprecated Benyttes i doksys, kommer til å bli erstattet av dokgen-variabel
     */
    @Deprecated
    private String begrunnelseKode;

    /**
     * @deprecated Benyttes i doksys, kommer til å bli erstattet av dokgen-variabel
     */
    @Deprecated
    private String ytterligereInformasjon;

    public BrevbestillingDto() {
    }

    public BrevbestillingDto(Builder builder) {
        this.mottaker = builder.mottaker;
        this.orgNr = builder.orgNr;
        this.innledningFritekst = builder.innledningFritekst;
        this.manglerFritekst = builder.manglerFritekst;
        this.kontaktperson = builder.kontaktperson;
        this.sendKopi = builder.sendKopi;
        this.fritekst = builder.fritekst;
        this.begrunnelseKode = builder.begrunnelseKode;
        this.ytterligereInformasjon = builder.ytterligereInformasjon;
    }

    public Aktoersroller getMottaker() {
        return mottaker;
    }

    public String getOrgNr() {
        return orgNr;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getManglerFritekst() {
        return manglerFritekst;
    }

    public String getKontaktperson() {
        return kontaktperson;
    }

    public boolean sendKopi() {
        return sendKopi;
    }

    public String getFritekst() {
        return fritekst;
    }

    public String getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public String getYtterligereInformasjon() {
        return ytterligereInformasjon;
    }

    public static class Builder {
        private Aktoersroller mottaker;
        private String orgNr;
        private String innledningFritekst;
        private String manglerFritekst;
        private String kontaktperson;
        private boolean sendKopi = true;
        private String fritekst;
        private String begrunnelseKode;
        private String ytterligereInformasjon;

        public Builder medMottaker(Aktoersroller mottaker) {
            this.mottaker = mottaker;
            return this;
        }

        public Builder medOrgNr(String orgNr) {
            this.orgNr = orgNr;
            return this;
        }

        public Builder medInnledningFritekst(String innledningFritekst) {
            this.innledningFritekst = innledningFritekst;
            return this;
        }

        public Builder medManglerFritekst(String manglerFritekst) {
            this.manglerFritekst = manglerFritekst;
            return this;
        }

        public Builder medKontaktperson(String kontaktperson) {
            this.kontaktperson = kontaktperson;
            return this;
        }

        public Builder sendKopi(boolean sendKopi) {
            this.sendKopi = sendKopi;
            return this;
        }

        public Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }

        public Builder medBegrunnelseKode(String begrunnelseKode) {
            this.begrunnelseKode = begrunnelseKode;
            return this;
        }

        public Builder medYtterligereInformasjon(String ytterligereInformasjon) {
            this.ytterligereInformasjon = ytterligereInformasjon;
            return this;
        }

        public BrevbestillingDto build() {
            return new BrevbestillingDto(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrevbestillingDto that = (BrevbestillingDto) o;
        return sendKopi == that.sendKopi && mottaker == that.mottaker && Objects.equals(orgNr, that.orgNr) &&
            Objects.equals(innledningFritekst, that.innledningFritekst) && Objects.equals(manglerFritekst, that.manglerFritekst) &&
            Objects.equals(kontaktperson, that.kontaktperson) && Objects.equals(fritekst, that.fritekst) &&
            Objects.equals(begrunnelseKode, that.begrunnelseKode) && Objects.equals(ytterligereInformasjon, that.ytterligereInformasjon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mottaker, orgNr, innledningFritekst, manglerFritekst, kontaktperson, sendKopi, fritekst, begrunnelseKode, ytterligereInformasjon);
    }

    @Override
    public String toString() {
        return "BrevbestillingDto{" +
            "mottaker=" + mottaker +
            ", orgNr='" + orgNr + '\'' +
            ", innledningFritekst='" + innledningFritekst + '\'' +
            ", manglerFritekst='" + manglerFritekst + '\'' +
            ", kontaktperson='" + kontaktperson + '\'' +
            ", sendKopi=" + sendKopi +
            ", fritekst='" + fritekst + '\'' +
            ", begrunnelseKode='" + begrunnelseKode + '\'' +
            ", ytterligereInformasjon='" + ytterligereInformasjon + '\'' +
            '}';
    }
}
