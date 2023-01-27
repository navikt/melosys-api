package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.dokument.brev.FritekstvedleggDto;
import no.nav.melosys.service.dokument.brev.KopiMottaker;
import no.nav.melosys.service.dokument.brev.SaksvedleggDto;
import no.nav.melosys.sikkerhet.context.SubjectHandler;

public class BrevbestillingRequest {

    private Produserbaredokumenter produserbardokument;
    private Aktoersroller mottaker;
    private String orgNr;
    private String institusjonId;
    private List<String> orgnrEtater;
    private String innledningFritekst;
    private String manglerFritekst;
    private String begrunnelseFritekst;
    private String ektefelleFritekst;
    private String barnFritekst;
    private String kontaktpersonNavn;
    private List<KopiMottaker> kopiMottakere;
    private String fritekstTittel;
    private String fritekst;
    private Distribusjonstype distribusjonstype;
    private boolean kontaktopplysninger;
    private String nyVurderingBakgrunn;
    private List<SaksvedleggDto> saksvedlegg;
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

    public no.nav.melosys.service.dokument.brev.BrevbestillingDto.Builder tilBrevbestillingDtoBuilder() {
        return new no.nav.melosys.service.dokument.brev.BrevbestillingDto.Builder()
            .medProduserbardokument(this.getProduserbardokument())
            .medMottaker(this.getMottaker())
            .medOrgNr(this.getOrgNr())
            .medInstitusjonId(this.getInstitusjonId())
            .medOrgnrEtater(this.getOrgnrEtater())
            .medInnledningFritekst(this.getInnledningFritekst())
            .medManglerFritekst(this.getManglerFritekst())
            .medBegrunnelseFritekst(this.getBegrunnelseFritekst())
            .medKontaktpersonNavn(this.getKontaktpersonNavn())
            .medKopiMottakere(this.getKopiMottakere())
            .medEktefelleFritekst(this.getEktefelleFritekst())
            .medBarnFritekst(this.getBarnFritekst())
            .medFritekstTittel(this.getFritekstTittel())
            .medFritekst(this.getFritekst())
            .medDistribusjonsType(this.getDistribusjonstype())
            .medKontaktopplysninger(this.isKontaktopplysninger())
            .medBegrunnelseKode(this.getBegrunnelseKode())
            .medYtterligereInformasjon(this.getYtterligereInformasjon())
            .medNyVurderingBakgrunn(this.getNyVurderingBakgrunn())
            .medSaksvedlegg(this.getSaksvedlegg())
            .medFritekstvedlegg(this.getFritekstvedlegg())
            .medDokumentTittel(this.getDokumentTittel());
    }

    public no.nav.melosys.service.dokument.brev.BrevbestillingDto tilBrevbestillingDto() {
        return tilBrevbestillingDtoBuilder()
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build();
    }

    public BrevbestillingRequest(Builder builder) {
        this.produserbardokument = builder.produserbardokument;
        this.mottaker = builder.mottaker;
        this.orgNr = builder.orgNr;
        this.innledningFritekst = builder.innledningFritekst;
        this.orgnrEtater = builder.orgnrEtater;
        this.manglerFritekst = builder.manglerFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.ektefelleFritekst = builder.ektefelleFritekst;
        this.barnFritekst = builder.barnFritekst;
        this.kontaktpersonNavn = builder.kontaktpersonNavn;
        this.kopiMottakere = builder.kopiMottakere;
        this.fritekstTittel = builder.fritekstTittel;
        this.fritekst = builder.fritekst;
        this.distribusjonstype = builder.distribusjonstype;
        this.kontaktopplysninger = builder.kontaktopplysninger;
        this.begrunnelseKode = builder.begrunnelseKode;
        this.ytterligereInformasjon = builder.ytterligereInformasjon;
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
        this.saksvedlegg = builder.saksvedlegg;
        this.fritekstvedlegg = builder.fritekstvedlegg;
        this.dokumentTittel = builder.dokumentTittel;
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

    public List<String> getOrgnrEtater() {
        return orgnrEtater;
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

    public Distribusjonstype getDistribusjonstype() {
        return distribusjonstype;
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

    public List<SaksvedleggDto> getSaksvedlegg() {
        return saksvedlegg;
    }

    public List<FritekstvedleggDto> getFritekstvedlegg() {
        return fritekstvedlegg;
    }

    public String getDokumentTittel() {
        return dokumentTittel;
    }

    public static class Builder {
        private Produserbaredokumenter produserbardokument;
        private Aktoersroller mottaker;
        private String orgNr;
        private String innledningFritekst;
        private List<String> orgnrEtater;
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
        private Distribusjonstype distribusjonstype;
        private String ytterligereInformasjon;
        private String nyVurderingBakgrunn;
        private List<SaksvedleggDto> saksvedlegg;
        private List<FritekstvedleggDto> fritekstvedlegg;
        private String dokumentTittel;

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

        public Builder medOrgnrEtater(List<String> orgnrEtater) {
            this.orgnrEtater = orgnrEtater;
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

        public Builder medSaksvedlegg(List<SaksvedleggDto> saksVedlegg) {
            this.saksvedlegg = saksVedlegg;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrevbestillingRequest that = (BrevbestillingRequest) o;
        return kontaktopplysninger == that.kontaktopplysninger && produserbardokument == that.produserbardokument
            && mottaker == that.mottaker
            && Objects.equals(orgNr, that.orgNr)
            && Objects.equals(institusjonId, that.institusjonId)
            && Objects.equals(orgnrEtater, that.orgnrEtater)
            && Objects.equals(innledningFritekst, that.innledningFritekst)
            && Objects.equals(manglerFritekst, that.manglerFritekst)
            && Objects.equals(begrunnelseFritekst, that.begrunnelseFritekst)
            && Objects.equals(ektefelleFritekst, that.ektefelleFritekst)
            && Objects.equals(barnFritekst, that.barnFritekst)
            && Objects.equals(kontaktpersonNavn, that.kontaktpersonNavn)
            && Objects.equals(kopiMottakere, that.kopiMottakere)
            && Objects.equals(distribusjonstype, that.distribusjonstype)
            && Objects.equals(fritekstTittel, that.fritekstTittel)
            && Objects.equals(fritekst, that.fritekst)
            && Objects.equals(begrunnelseKode, that.begrunnelseKode)
            && Objects.equals(ytterligereInformasjon, that.ytterligereInformasjon)
            && Objects.equals(nyVurderingBakgrunn, that.nyVurderingBakgrunn)
            && Objects.equals(saksvedlegg, that.saksvedlegg)
            && Objects.equals(dokumentTittel, that.dokumentTittel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(produserbardokument, mottaker, orgNr, institusjonId, orgnrEtater, innledningFritekst,
            manglerFritekst,
            begrunnelseFritekst, ektefelleFritekst, barnFritekst, kontaktpersonNavn, kopiMottakere, distribusjonstype, fritekstTittel,
            fritekst, kontaktopplysninger, begrunnelseKode, ytterligereInformasjon, nyVurderingBakgrunn, saksvedlegg, dokumentTittel);
    }

    @Override
    public String toString() {
        return "BrevbestillingDto{" +
            "produserbardokument=" + produserbardokument +
            ", mottaker=" + mottaker +
            ", orgNr='" + orgNr + '\'' +
            ", institusjonId='" + institusjonId + '\'' +
            ", orgnrEtater='" + orgnrEtater + '\'' +
            ", innledningFritekst='" + innledningFritekst + '\'' +
            ", manglerFritekst='" + manglerFritekst + '\'' +
            ", begrunnelseFritekst='" + begrunnelseFritekst + '\'' +
            ", ektefelleFritekst='" + ektefelleFritekst + '\'' +
            ", barnFritekst='" + barnFritekst + '\'' +
            ", kontaktpersonNavn='" + kontaktpersonNavn + '\'' +
            ", kopiMottakere=" + kopiMottakere +
            ", distribusjonstype='" + distribusjonstype + '\'' +
            ", fritekstTittel='" + fritekstTittel + '\'' +
            ", fritekst='" + fritekst + '\'' +
            ", kontaktopplysninger=" + kontaktopplysninger +
            ", begrunnelseKode='" + begrunnelseKode + '\'' +
            ", ytterligereInformasjon='" + ytterligereInformasjon + '\'' +
            ", nyVurderingBakgrunn='" + nyVurderingBakgrunn + '\'' +
            ", saksvedlegg='" + saksvedlegg + '\'' +
            ", dokumentTittel='" + dokumentTittel + '\'' +
            '}';
    }
}
