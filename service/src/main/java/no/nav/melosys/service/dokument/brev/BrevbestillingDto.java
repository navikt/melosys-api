package no.nav.melosys.service.dokument.brev;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public final class BrevbestillingDto {
    private Produserbaredokumenter produserbardokument;
    private Mottakerroller mottaker;
    private String orgnr;
    private List<String> orgnrNorskMyndighet;
    private String institusjonId;
    private String innledningFritekst;
    private String manglerFritekst;
    private String begrunnelseFritekst;
    private String ektefelleFritekst;
    private String barnFritekst;
    private String kontaktpersonNavn;
    private List<KopiMottakerDto> kopiMottakere;
    private String bestillersId;
    private String fritekstTittel;
    private String fritekst;
    private Distribusjonstype distribusjonstype;
    private boolean kontaktopplysninger;
    private String nyVurderingBakgrunn;
    private List<SaksvedleggDto> saksVedlegg;
    private List<FritekstvedleggDto> fritekstvedlegg;
    private String dokumentTittel;
    @Deprecated(since = "Benyttes i doksys, kommer til å bli erstattet av dokgen-variabel")
    private String begrunnelseKode;
    @Deprecated(since = "Benyttes i doksys, kommer til å bli erstattet av dokgen-variabel")
    private String ytterligereInformasjon;

    public BrevbestillingDto() {
    }

    public BrevbestillingDto(
        Produserbaredokumenter produserbardokument,
        Mottakerroller mottaker,
        String orgnr,
        List<String> orgnrNorskMyndighet,
        String institusjonId,
        String innledningFritekst,
        String manglerFritekst,
        String begrunnelseFritekst,
        String ektefelleFritekst,
        String barnFritekst,
        String kontaktpersonNavn,
        List<KopiMottakerDto> kopiMottakere,
        String bestillersId,
        String fritekstTittel,
        String fritekst,
        Distribusjonstype distribusjonstype,
        boolean kontaktopplysninger,
        String nyVurderingBakgrunn,
        List<SaksvedleggDto> saksVedlegg,
        List<FritekstvedleggDto> fritekstvedlegg,
        String dokumentTittel,
        @Deprecated(since = "Benyttes i doksys, kommer til å bli erstattet av dokgen-variabel")
            String begrunnelseKode,
        @Deprecated(since = "Benyttes i doksys, kommer til å bli erstattet av dokgen-variabel")
            String ytterligereInformasjon) {

        this.produserbardokument = produserbardokument;
        this.mottaker = mottaker;
        this.orgnr = orgnr;
        this.orgnrNorskMyndighet = orgnrNorskMyndighet;
        this.institusjonId = institusjonId;
        this.innledningFritekst = innledningFritekst;
        this.manglerFritekst = manglerFritekst;
        this.begrunnelseFritekst = begrunnelseFritekst;
        this.ektefelleFritekst = ektefelleFritekst;
        this.barnFritekst = barnFritekst;
        this.kontaktpersonNavn = kontaktpersonNavn;
        this.kopiMottakere = kopiMottakere;
        this.bestillersId = bestillersId;
        this.fritekstTittel = fritekstTittel;
        this.fritekst = fritekst;
        this.distribusjonstype = distribusjonstype;
        this.kontaktopplysninger = kontaktopplysninger;
        this.nyVurderingBakgrunn = nyVurderingBakgrunn;
        this.saksVedlegg = saksVedlegg;
        this.fritekstvedlegg = fritekstvedlegg;
        this.dokumentTittel = dokumentTittel;
        this.begrunnelseKode = begrunnelseKode;
        this.ytterligereInformasjon = ytterligereInformasjon;
    }


    public static BrevbestillingDto av(BrevbestillingUtkast brevbestillingUtkast) {
        String institusjonId = null;
        String bestillersId = null;
        String deprecatedBegrunnelseKode = null;
        String deprecatedYtterligereInformasjon = null;

        return new BrevbestillingDto(
            brevbestillingUtkast.produserbardokument(),
            brevbestillingUtkast.mottaker(),
            brevbestillingUtkast.orgnr(),
            brevbestillingUtkast.orgnrNorskMyndighet(),
            institusjonId,
            brevbestillingUtkast.innledningFritekst(),
            brevbestillingUtkast.manglerFritekst(),
            brevbestillingUtkast.begrunnelseFritekst(),
            brevbestillingUtkast.ektefelleFritekst(),
            brevbestillingUtkast.barnFritekst(),
            brevbestillingUtkast.kontaktpersonNavn(),
            brevbestillingUtkast.kopiMottakere().stream().map(KopiMottakerDto::av).toList(),
            bestillersId,
            brevbestillingUtkast.fritekstTittel(),
            brevbestillingUtkast.fritekst(),
            brevbestillingUtkast.distribusjonstype(),
            brevbestillingUtkast.kontaktopplysninger(),
            brevbestillingUtkast.nyVurderingBakgrunn(),
            brevbestillingUtkast.saksVedlegg().stream().map(SaksvedleggDto::av).toList(),
            brevbestillingUtkast.fritekstVedlegg().stream().map(FritekstvedleggDto::av).toList(),
            brevbestillingUtkast.dokumentTittel(),
            deprecatedBegrunnelseKode,
            deprecatedYtterligereInformasjon
        );
    }

    public void setProduserbardokument(Produserbaredokumenter produserbardokument) {
        this.produserbardokument = produserbardokument;
    }

    public void setMottaker(Mottakerroller mottaker) {
        this.mottaker = mottaker;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public void setOrgnrNorskMyndighet(List<String> orgnrNorskMyndighet) {
        this.orgnrNorskMyndighet = orgnrNorskMyndighet;
    }

    public void setInstitusjonId(String institusjonId) {
        this.institusjonId = institusjonId;
    }

    public void setInnledningFritekst(String innledningFritekst) {
        this.innledningFritekst = innledningFritekst;
    }

    public void setManglerFritekst(String manglerFritekst) {
        this.manglerFritekst = manglerFritekst;
    }

    public void setBegrunnelseFritekst(String begrunnelseFritekst) {
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public void setEktefelleFritekst(String ektefelleFritekst) {
        this.ektefelleFritekst = ektefelleFritekst;
    }

    public void setBarnFritekst(String barnFritekst) {
        this.barnFritekst = barnFritekst;
    }

    public void setKontaktpersonNavn(String kontaktpersonNavn) {
        this.kontaktpersonNavn = kontaktpersonNavn;
    }

    public void setKopiMottakere(List<KopiMottakerDto> kopiMottakere) {
        this.kopiMottakere = kopiMottakere;
    }

    public void setBestillersId(String bestillersId) {
        this.bestillersId = bestillersId;
    }

    public void setFritekstTittel(String fritekstTittel) {
        this.fritekstTittel = fritekstTittel;
    }

    public void setFritekst(String fritekst) {
        this.fritekst = fritekst;
    }

    public void setDistribusjonstype(Distribusjonstype distribusjonstype) {
        this.distribusjonstype = distribusjonstype;
    }

    public void setKontaktopplysninger(boolean kontaktopplysninger) {
        this.kontaktopplysninger = kontaktopplysninger;
    }

    public void setNyVurderingBakgrunn(String nyVurderingBakgrunn) {
        this.nyVurderingBakgrunn = nyVurderingBakgrunn;
    }

    public void setSaksVedlegg(List<SaksvedleggDto> saksVedlegg) {
        this.saksVedlegg = saksVedlegg;
    }

    public void setFritekstvedlegg(List<FritekstvedleggDto> fritekstvedlegg) {
        this.fritekstvedlegg = fritekstvedlegg;
    }

    public void setDokumentTittel(String dokumentTittel) {
        this.dokumentTittel = dokumentTittel;
    }

    public void setBegrunnelseKode(String begrunnelseKode) {
        this.begrunnelseKode = begrunnelseKode;
    }

    public void setYtterligereInformasjon(String ytterligereInformasjon) {
        this.ytterligereInformasjon = ytterligereInformasjon;
    }

    public Produserbaredokumenter getProduserbardokument() {
        return produserbardokument;
    }

    public Mottakerroller getMottaker() {
        return mottaker;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public List<String> getOrgnrNorskMyndighet() {
        if (orgnrNorskMyndighet == null) {
            orgnrNorskMyndighet = new ArrayList<>();
        }
        return orgnrNorskMyndighet;
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

    public List<KopiMottakerDto> getKopiMottakere() {
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

    public Distribusjonstype getDistribusjonstype() {
        return distribusjonstype;
    }

    public boolean getKontaktopplysninger() {
        return kontaktopplysninger;
    }

    public String getNyVurderingBakgrunn() {
        return nyVurderingBakgrunn;
    }

    public List<SaksvedleggDto> getSaksVedlegg() {
        if (saksVedlegg == null) {
            saksVedlegg = new ArrayList<>();
        }
        return saksVedlegg;
    }

    public List<FritekstvedleggDto> getFritekstvedlegg() {
        if (fritekstvedlegg == null) {
            fritekstvedlegg = new ArrayList<>();
        }
        return fritekstvedlegg;
    }

    public String getDokumentTittel() {
        return dokumentTittel;
    }

    @Deprecated(since = "Benyttes i doksys, kommer til å bli erstattet av dokgen-variabel")
    public String getBegrunnelseKode() {
        return begrunnelseKode;
    }

    @Deprecated(since = "Benyttes i doksys, kommer til å bli erstattet av dokgen-variabel")
    public String getYtterligereInformasjon() {
        return ytterligereInformasjon;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BrevbestillingDto) obj;
        return Objects.equals(this.produserbardokument, that.produserbardokument) &&
            Objects.equals(this.mottaker, that.mottaker) &&
            Objects.equals(this.orgnr, that.orgnr) &&
            Objects.equals(this.orgnrNorskMyndighet, that.orgnrNorskMyndighet) &&
            Objects.equals(this.institusjonId, that.institusjonId) &&
            Objects.equals(this.innledningFritekst, that.innledningFritekst) &&
            Objects.equals(this.manglerFritekst, that.manglerFritekst) &&
            Objects.equals(this.begrunnelseFritekst, that.begrunnelseFritekst) &&
            Objects.equals(this.ektefelleFritekst, that.ektefelleFritekst) &&
            Objects.equals(this.barnFritekst, that.barnFritekst) &&
            Objects.equals(this.kontaktpersonNavn, that.kontaktpersonNavn) &&
            Objects.equals(this.kopiMottakere, that.kopiMottakere) &&
            Objects.equals(this.bestillersId, that.bestillersId) &&
            Objects.equals(this.fritekstTittel, that.fritekstTittel) &&
            Objects.equals(this.fritekst, that.fritekst) &&
            Objects.equals(this.distribusjonstype, that.distribusjonstype) &&
            this.kontaktopplysninger == that.kontaktopplysninger &&
            Objects.equals(this.nyVurderingBakgrunn, that.nyVurderingBakgrunn) &&
            Objects.equals(this.saksVedlegg, that.saksVedlegg) &&
            Objects.equals(this.fritekstvedlegg, that.fritekstvedlegg) &&
            Objects.equals(this.dokumentTittel, that.dokumentTittel) &&
            Objects.equals(this.begrunnelseKode, that.begrunnelseKode) &&
            Objects.equals(this.ytterligereInformasjon, that.ytterligereInformasjon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(produserbardokument, mottaker, orgnr, orgnrNorskMyndighet, institusjonId, innledningFritekst, manglerFritekst, begrunnelseFritekst, ektefelleFritekst, barnFritekst, kontaktpersonNavn, kopiMottakere, bestillersId, fritekstTittel, fritekst, distribusjonstype, kontaktopplysninger, nyVurderingBakgrunn, saksVedlegg, fritekstvedlegg, dokumentTittel, begrunnelseKode, ytterligereInformasjon);
    }

    @Override
    public String toString() {
        return "BrevbestillingDto[" +
            "produserbardokument=" + produserbardokument + ", " +
            "mottaker=" + mottaker + ", " +
            "orgnr=" + orgnr + ", " +
            "orgnrNorskMyndighet=" + orgnrNorskMyndighet + ", " +
            "institusjonId=" + institusjonId + ", " +
            "innledningFritekst=" + innledningFritekst + ", " +
            "manglerFritekst=" + manglerFritekst + ", " +
            "begrunnelseFritekst=" + begrunnelseFritekst + ", " +
            "ektefelleFritekst=" + ektefelleFritekst + ", " +
            "barnFritekst=" + barnFritekst + ", " +
            "kontaktpersonNavn=" + kontaktpersonNavn + ", " +
            "kopiMottakere=" + kopiMottakere + ", " +
            "bestillersId=" + bestillersId + ", " +
            "fritekstTittel=" + fritekstTittel + ", " +
            "fritekst=" + fritekst + ", " +
            "distribusjonstype=" + distribusjonstype + ", " +
            "kontaktopplysninger=" + kontaktopplysninger + ", " +
            "nyVurderingBakgrunn=" + nyVurderingBakgrunn + ", " +
            "saksVedlegg=" + saksVedlegg + ", " +
            "fritekstvedlegg=" + fritekstvedlegg + ", " +
            "dokumentTittel=" + dokumentTittel + ", " +
            "begrunnelseKode=" + begrunnelseKode + ", " +
            "ytterligereInformasjon=" + ytterligereInformasjon + ']';
    }

}
