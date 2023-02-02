package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.*;

import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.FritekstvedleggDto;
import no.nav.melosys.service.dokument.brev.KopiMottakerDto;
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
    private List<KopiMottakerDto> kopiMottakere;
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

    public BrevbestillingDto tilBrevbestillingDto(String bestillersId) {
        return new BrevbestillingDto(
            this.produserbardokument,
            this.mottaker,
            this.orgNr,
            this.orgnrEtater,
            this.institusjonId,
            this.innledningFritekst,
            this.manglerFritekst,
            this.begrunnelseFritekst,
            this.ektefelleFritekst,
            this.barnFritekst,
            this.kontaktpersonNavn,
            this.kopiMottakere,
            bestillersId,
            this.fritekstTittel,
            this.fritekst,
            this.distribusjonstype,
            this.kontaktopplysninger,
            this.nyVurderingBakgrunn,
            this.saksvedlegg,
            this.fritekstvedlegg,
            this.dokumentTittel,
            this.begrunnelseKode,
            this.ytterligereInformasjon
        );
    }

    public BrevbestillingDto tilBrevbestillingDto() {
        return tilBrevbestillingDto(SubjectHandler.getInstance().getUserID());
    }

    public void setProduserbardokument(Produserbaredokumenter produserbardokument) {
        this.produserbardokument = produserbardokument;
    }

    public void setMottaker(Aktoersroller mottaker) {
        this.mottaker = mottaker;
    }

    public void setOrgNr(String orgNr) {
        this.orgNr = orgNr;
    }

    public void setInstitusjonId(String institusjonId) {
        this.institusjonId = institusjonId;
    }

    public void setOrgnrEtater(List<String> orgnrEtater) {
        this.orgnrEtater = orgnrEtater;
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

    public void setSaksvedlegg(List<SaksvedleggDto> saksvedlegg) {
        this.saksvedlegg = saksvedlegg;
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

    public List<KopiMottakerDto> getKopiMottakere() {
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

    public BrevbestillingUtkast tilUtkast() {
        return new BrevbestillingUtkast(
            this.getProduserbardokument(),
            this.getMottaker(),
            this.getOrgNr(),
            this.getOrgnrEtater(),
            this.getInstitusjonId(),
            this.getInnledningFritekst(),
            this.getManglerFritekst(),
            this.getBegrunnelseFritekst(),
            this.getEktefelleFritekst(),
            this.getBarnFritekst(),
            this.getKontaktpersonNavn(),
            this.getKopiMottakere().stream().map(KopiMottakerDto::tilUtkast).toList(),
            this.getFritekstTittel(),
            this.getFritekst(),
            this.getDistribusjonstype(),
            this.isKontaktopplysninger(),
            this.getNyVurderingBakgrunn(),
            Optional.ofNullable(this.getSaksvedlegg())
                .orElseGet(Collections::emptyList)
                .stream().map(SaksvedleggDto::tilUtkast).toList(),
            Optional.ofNullable(this.getFritekstvedlegg())
                .orElseGet(Collections::emptyList)
                .stream().map(FritekstvedleggDto::tilUtkast).toList(),
            this.getDokumentTittel()
        );
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
        return "BrevbestillingRequest{" +
            "produserbardokument=" + produserbardokument +
            ", mottaker=" + mottaker +
            ", orgNr='" + orgNr + '\'' +
            ", institusjonId='" + institusjonId + '\'' +
            ", orgnrEtater=" + orgnrEtater +
            ", innledningFritekst='" + innledningFritekst + '\'' +
            ", manglerFritekst='" + manglerFritekst + '\'' +
            ", begrunnelseFritekst='" + begrunnelseFritekst + '\'' +
            ", ektefelleFritekst='" + ektefelleFritekst + '\'' +
            ", barnFritekst='" + barnFritekst + '\'' +
            ", kontaktpersonNavn='" + kontaktpersonNavn + '\'' +
            ", kopiMottakere=" + kopiMottakere +
            ", fritekstTittel='" + fritekstTittel + '\'' +
            ", fritekst='" + fritekst + '\'' +
            ", distribusjonstype=" + distribusjonstype +
            ", kontaktopplysninger=" + kontaktopplysninger +
            ", nyVurderingBakgrunn='" + nyVurderingBakgrunn + '\'' +
            ", saksvedlegg=" + saksvedlegg +
            ", fritekstvedlegg=" + fritekstvedlegg +
            ", dokumentTittel='" + dokumentTittel + '\'' +
            ", begrunnelseKode='" + begrunnelseKode + '\'' +
            ", ytterligereInformasjon='" + ytterligereInformasjon + '\'' +
            '}';
    }
}
