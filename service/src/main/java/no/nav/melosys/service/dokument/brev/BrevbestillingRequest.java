package no.nav.melosys.service.dokument.brev;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public class BrevbestillingRequest {
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
    private String bestillersId;
    private String fritekstTittel;
    private Distribusjonstype distribusjonstype;
    private String fritekst;
    private boolean kontaktopplysninger;
    private String nyVurderingBakgrunn;
    private List<SaksvedleggDto> saksVedlegg;
    private List<FritekstvedleggDto> fritekstvedlegg;
    private String dokumentTittel;

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

    public BrevbestillingRequest() {
    }

    // Må ha mulighet for å sette produserbartdokument pga bakoverkompabilitet
    public void setProduserbardokument(Produserbaredokumenter produserbardokument) {
        this.produserbardokument = produserbardokument;
    }

    public BrevbestillingRequest(Builder builder) {
        this.produserbardokument = builder.produserbardokument;
        this.mottaker = builder.mottaker;
        this.orgNr = builder.orgNr;
        this.institusjonId = builder.institusjonId;
        this.innledningFritekst = builder.innledningFritekst;
        this.manglerFritekst = builder.manglerFritekst;
        this.ektefelleFritekst = builder.ektefelleFritekst;
        this.barnFritekst = builder.barnFritekst;
        this.kontaktpersonNavn = builder.kontaktpersonNavn;
        this.kopiMottakere = builder.kopiMottakere;
        this.bestillersId = builder.bestillersId;
        this.fritekstTittel = builder.fritekstTittel;
        this.distribusjonstype = builder.distribusjonstype;
        this.fritekst = builder.fritekst;
        this.kontaktopplysninger = builder.kontaktopplysninger;
        this.begrunnelseKode = builder.begrunnelseKode;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.ytterligereInformasjon = builder.ytterligereInformasjon;
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
        this.saksVedlegg = builder.saksVedlegg;
        this.fritekstvedlegg = builder.fritekstvedlegg;
        this.dokumentTittel = builder.dokumentTittel;
    }

    public List<FritekstvedleggDto> getFritekstvedlegg() {
        return fritekstvedlegg;
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

    public Distribusjonstype getDistribusjonstype() {
        return distribusjonstype;
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

    public String getBestillersId() {
        return bestillersId;
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

    public List<SaksvedleggDto> getSaksVedlegg() {
        return saksVedlegg;
    }

    public String getDokumentTittel() {
        return dokumentTittel;
    }

    public static class Builder {
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
        private String bestillersId;
        private String fritekstTittel;
        private String fritekst;
        private Distribusjonstype distribusjonstype;
        private boolean kontaktopplysninger;
        private String begrunnelseKode;
        private String ytterligereInformasjon;
        private String nyVurderingBakgrunn;
        private List<SaksvedleggDto> saksVedlegg;
        public List<FritekstvedleggDto> fritekstvedlegg;
        public String dokumentTittel;

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

        public Builder medInstitusjonId(String institusjonId) {
            this.institusjonId = institusjonId;
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

        public Builder medBestillersId(String bestillersId) {
            this.bestillersId = bestillersId;
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

        public Builder medDistribusjonsType(Distribusjonstype distribusjonstype) {
            this.distribusjonstype = distribusjonstype;
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

        public Builder medSaksvedlegg(List<SaksvedleggDto> saksVedlegg) {
            this.saksVedlegg = saksVedlegg;
            return this;
        }

        public Builder medFritekstvedlegg(List<FritekstvedleggDto> fritekstvedlegg) {
            this.fritekstvedlegg = fritekstvedlegg;
            return this;
        }

        public Builder medDokumentTittel(String dokumentTittel) {
            this.dokumentTittel = dokumentTittel;
            return this;
        }

        public BrevbestillingRequest build() {
            return new BrevbestillingRequest(this);
        }
    }
}
