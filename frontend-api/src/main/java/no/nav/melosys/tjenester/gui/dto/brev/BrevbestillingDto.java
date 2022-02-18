package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.KopiMottaker;

public class BrevbestillingDto {

    private Produserbaredokumenter produserbardokument;
    private Aktoersroller mottaker;
    private String orgNr;
    private String institusjonId;
    private String innledningFritekst;
    private String manglerFritekst;
    private String begrunnelseFritekst;
    private String ektefelleFritekst;
    private String barnFritekst;
    private String kontaktpersonNavn;
    private List<KopiMottaker> kopiMottakere;
    private String fritekstTittel;
    private String fritekst;
    private boolean kontaktopplysninger;
    private String nyVurderingBakgrunn;

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

    public BrevbestillingRequest.Builder tilRequestBuilder() {
        return new BrevbestillingRequest.Builder()
            .medProduserbardokument(this.getProduserbardokument())
            .medMottaker(this.getMottaker())
            .medOrgNr(this.getOrgNr())
            .medInstitusjonId(this.getInstitusjonId())
            .medInnledningFritekst(this.getInnledningFritekst())
            .medManglerFritekst(this.getManglerFritekst())
            .medBegrunnelseFritekst(this.getBegrunnelseFritekst())
            .medKontaktpersonNavn(this.getKontaktpersonNavn())
            .medKopiMottakere(this.getKopiMottakere())
            .medEktefelleFritekst(this.getEktefelleFritekst())
            .medBarnFritekst(this.getBarnFritekst())
            .medFritekstTittel(this.getFritekstTittel())
            .medFritekst(this.getFritekst())
            .medKontaktopplysninger(this.isKontaktopplysninger())
            .medBegrunnelseKode(this.getBegrunnelseKode())
            .medYtterligereInformasjon(this.getYtterligereInformasjon())
            .medNyVurderingBakgrunn(this.getNyVurderingBakgrunn());
    }

    public BrevbestillingDto(Builder builder) {
        this.produserbardokument = builder.produserbardokument;
        this.mottaker = builder.mottaker;
        this.orgNr = builder.orgNr;
        this.innledningFritekst = builder.innledningFritekst;
        this.manglerFritekst = builder.manglerFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.ektefelleFritekst = builder.ektefelleFritekst;
        this.barnFritekst = builder.barnFritekst;
        this.kontaktpersonNavn = builder.kontaktpersonNavn;
        this.kopiMottakere = builder.kopiMottakere;
        this.fritekstTittel = builder.fritekstTittel;
        this.fritekst = builder.fritekst;
        this.kontaktopplysninger = builder.kontaktopplysninger;
        this.begrunnelseKode = builder.begrunnelseKode;
        this.ytterligereInformasjon = builder.ytterligereInformasjon;
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
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

    public String getInstitusjonId() {
        return institusjonId;
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

    public String getEktefelleFritekst() {
        return ektefelleFritekst;
    }

    public String getBarnFritekst() {
        return barnFritekst;
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

    public String getFritekstTittel() {
        return fritekstTittel;
    }

    public String getFritekst() {
        return fritekst;
    }

    public boolean isKontaktopplysninger() {
        return kontaktopplysninger;
    }

    public String getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public String getYtterligereInformasjon() {
        return ytterligereInformasjon;
    }

    public String getNyVurderingBakgrunn() {
        return nyVurderingBakgrunn;
    }

    public static class Builder {
        private Produserbaredokumenter produserbardokument;
        private Aktoersroller mottaker;
        private String orgNr;
        private String innledningFritekst;
        private String manglerFritekst;
        private String begrunnelseFritekst;
        private String ektefelleFritekst;
        private String barnFritekst;
        private String kontaktpersonNavn;
        private List<KopiMottaker> kopiMottakere;
        private String fritekstTittel;
        private String fritekst;
        public boolean kontaktopplysninger;
        private String begrunnelseKode;
        private String ytterligereInformasjon;
        private String nyVurderingBakgrunn;

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

        public Builder medEktefelleFritekst(String ektefelleFritekst) {
            this.ektefelleFritekst = ektefelleFritekst;
            return this;
        }

        public Builder medBarnFritekst(String barnFritekst) {
            this.barnFritekst = barnFritekst;
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

        public Builder medBegrunnelseKode(String begrunnelseKode) {
            this.begrunnelseKode = begrunnelseKode;
            return this;
        }

        public Builder medYtterligereInformasjon(String ytterligereInformasjon) {
            this.ytterligereInformasjon = ytterligereInformasjon;
            return this;
        }

        public Builder medNyVurderingBakgrunn(String nyVurderingBakgrunn) {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn;
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
        return kontaktopplysninger == that.kontaktopplysninger && produserbardokument == that.produserbardokument
            && mottaker == that.mottaker && Objects.equals(orgNr, that.orgNr)
            && Objects.equals(innledningFritekst, that.innledningFritekst) && Objects.equals(manglerFritekst, that.manglerFritekst)
            && Objects.equals(begrunnelseFritekst, that.begrunnelseFritekst) && Objects.equals(ektefelleFritekst, that.ektefelleFritekst)
            && Objects.equals(barnFritekst, that.barnFritekst) && Objects.equals(kontaktpersonNavn, that.kontaktpersonNavn)
            && Objects.equals(kopiMottakere, that.kopiMottakere) && Objects.equals(fritekstTittel, that.fritekstTittel)
            && Objects.equals(fritekst, that.fritekst) && Objects.equals(begrunnelseKode, that.begrunnelseKode)
            && Objects.equals(ytterligereInformasjon, that.ytterligereInformasjon)
            && Objects.equals(nyVurderingBakgrunn, that.nyVurderingBakgrunn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(produserbardokument, mottaker, orgNr, innledningFritekst, manglerFritekst,
            begrunnelseFritekst, ektefelleFritekst, barnFritekst, kontaktpersonNavn, kopiMottakere, fritekstTittel,
            fritekst, kontaktopplysninger, begrunnelseKode, ytterligereInformasjon, nyVurderingBakgrunn);
    }

    @Override
    public String toString() {
        return "BrevbestillingDto{" +
            "produserbardokument=" + produserbardokument +
            ", mottaker=" + mottaker +
            ", orgNr='" + orgNr + '\'' +
            ", innledningFritekst='" + innledningFritekst + '\'' +
            ", manglerFritekst='" + manglerFritekst + '\'' +
            ", begrunnelseFritekst='" + begrunnelseFritekst + '\'' +
            ", ektefelleFritekst='" + ektefelleFritekst + '\'' +
            ", barnFritekst='" + barnFritekst + '\'' +
            ", kontaktpersonNavn='" + kontaktpersonNavn + '\'' +
            ", kopiMottakere=" + kopiMottakere +
            ", fritekstTittel='" + fritekstTittel + '\'' +
            ", fritekst='" + fritekst + '\'' +
            ", kontaktopplysninger=" + kontaktopplysninger +
            ", begrunnelseKode='" + begrunnelseKode + '\'' +
            ", ytterligereInformasjon='" + ytterligereInformasjon + '\'' +
            ", nyVurderingBakgrunn='" + nyVurderingBakgrunn + '\'' +
            '}';
    }
}
