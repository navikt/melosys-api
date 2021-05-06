package no.nav.melosys.service.dokument.brev;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public class BrevbestillingDto {

    private Produserbaredokumenter produserbardokument;
    private Aktoersroller mottaker;
    private String orgNr;
    private String innledningFritekst;
    private String manglerFritekst;
    private String begrunnelseFritekst;
    private String kontaktpersonNavn;
    private List<KopiMottaker> kopiMottakere;

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
        this.produserbardokument = builder.produserbardokument;
        this.mottaker = builder.mottaker;
        this.orgNr = builder.orgNr;
        this.innledningFritekst = builder.innledningFritekst;
        this.manglerFritekst = builder.manglerFritekst;
        this.kontaktpersonNavn = builder.kontaktpersonNavn;
        this.kopiMottakere = builder.kopiMottakere;
        this.fritekst = builder.fritekst;
        this.begrunnelseKode = builder.begrunnelseKode;
        this.ytterligereInformasjon = builder.ytterligereInformasjon;
    }

    public Produserbaredokumenter getProduserbardokument() {
        return produserbardokument;
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

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getKontaktpersonNavn() {
        return kontaktpersonNavn;
    }

    public List<KopiMottaker> getKopiMottakere() {
        if (kopiMottakere == null) {
            kopiMottakere = new ArrayList<>();
        }
        return kopiMottakere;
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
        private Produserbaredokumenter produserbardokument;
        private Aktoersroller mottaker;
        private String orgNr;
        private String innledningFritekst;
        private String manglerFritekst;
        private String begrunnelseFritekst;
        private String kontaktpersonNavn;
        private List<KopiMottaker> kopiMottakere;
        private String fritekst;
        private String begrunnelseKode;
        private String ytterligereInformasjon;

        public Builder medProduserbardokument(Produserbaredokumenter produserbardokument) {
            this.produserbardokument = produserbardokument;
            return this;
        }

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

        public Builder medBegrunnelseFritekst(String begrunnelseFritekst) {
            this.begrunnelseFritekst = begrunnelseFritekst;
            return this;
        }

        public Builder medKontaktpersonNavn(String kontaktpersonNavn) {
            this.kontaktpersonNavn = kontaktpersonNavn;
            return this;
        }

        public Builder medKopiMottakere(List<KopiMottaker> kopiMottakere) {
            this.kopiMottakere = kopiMottakere;
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
        return mottaker == that.mottaker && Objects.equals(orgNr, that.orgNr) &&
            Objects.equals(innledningFritekst, that.innledningFritekst) && Objects.equals(manglerFritekst, that.manglerFritekst) &&
            Objects.equals(kontaktpersonNavn, that.kontaktpersonNavn) && Objects.equals(fritekst, that.fritekst) &&
            Objects.equals(begrunnelseKode, that.begrunnelseKode) && Objects.equals(ytterligereInformasjon, that.ytterligereInformasjon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mottaker, orgNr, innledningFritekst, manglerFritekst, kontaktpersonNavn, fritekst, begrunnelseKode, ytterligereInformasjon);
    }

    @Override
    public String toString() {
        return "BrevbestillingDto{" +
            "mottaker=" + mottaker +
            ", orgNr='" + orgNr + '\'' +
            ", innledningFritekst='" + innledningFritekst + '\'' +
            ", manglerFritekst='" + manglerFritekst + '\'' +
            ", kontaktpersonNavn='" + kontaktpersonNavn + '\'' +
            ", fritekst='" + fritekst + '\'' +
            ", begrunnelseKode='" + begrunnelseKode + '\'' +
            ", ytterligereInformasjon='" + ytterligereInformasjon + '\'' +
            '}';
    }
}
