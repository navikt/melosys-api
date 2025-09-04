package no.nav.melosys.service.dokument.brev;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.brev.StandardvedleggType;
import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus;

public final class BrevbestillingDto {
    private Produserbaredokumenter produserbardokument;
    private Mottakerroller mottaker;
    private String orgnr;
    private List<String> orgnrNorskMyndighet;
    private String institusjonID;
    private String innledningFritekst;
    private String manglerFritekst;
    private String begrunnelseFritekst;
    private String ektefelleFritekst;
    private String barnFritekst;
    private String trygdeavgiftFritekst;
    private String kontaktpersonNavn;
    private List<KopiMottakerDto> kopiMottakere;
    private String bestillersId;
    private String fritekstTittel;
    private String fritekst;
    private Distribusjonstype distribusjonstype;
    private boolean skalViseStandardTekstOmkontaktopplysninger;
    private String nyVurderingBakgrunn;
    private List<SaksvedleggDto> saksVedlegg;
    private StandardvedleggType standardvedleggType;
    private List<FritekstvedleggDto> fritekstvedlegg;
    private String dokumentTittel;
    private String saksbehandlerNrToIdent;
    private boolean skalViseStandardTekstOmOpplysninger;
    private String begrunnelseKode;
    private String ytterligereInformasjon;
    private String fakturanummer;
    private Betalingsstatus betalingsstatus;
    private String fullmektigForBetaling;
    private LocalDate betalingsfrist;
    private String annenPersonMottakerIdent;
    private LocalDate opphørtDato;
    private boolean erInnvilgelse;
    private boolean erEøsPensjonist;

    public BrevbestillingDto() {
    }

    public BrevbestillingDto(
        Produserbaredokumenter produserbardokument,
        Mottakerroller mottaker,
        String orgnr,
        List<String> orgnrNorskMyndighet,
        String institusjonID,
        String innledningFritekst,
        String manglerFritekst,
        String begrunnelseFritekst,
        String ektefelleFritekst,
        String barnFritekst,
        String trygdeavgiftFritekst,
        String kontaktpersonNavn,
        List<KopiMottakerDto> kopiMottakere,
        String bestillersId,
        String fritekstTittel,
        String fritekst,
        Distribusjonstype distribusjonstype,
        boolean skalViseStandardTekstOmkontaktopplysninger,
        String nyVurderingBakgrunn,
        List<SaksvedleggDto> saksVedlegg,
        StandardvedleggType standardvedleggType,
        List<FritekstvedleggDto> fritekstvedlegg,
        String dokumentTittel,
        String saksbehandlerNrToIdent,
        boolean skalViseStandardTekstOmOpplysninger,
        String begrunnelseKode,
        String ytterligereInformasjon,
        String fakturanummer,
        Betalingsstatus betalingsstatus,
        String fullmektigForBetaling,
        LocalDate betalingsfrist,
        String annenPersonMottakerIdent,
        LocalDate opphørtDato,
        boolean erInnvilgelse,
        boolean erEøsPensjonist) {

        this.produserbardokument = produserbardokument;
        this.mottaker = mottaker;
        this.orgnr = orgnr;
        this.orgnrNorskMyndighet = orgnrNorskMyndighet;
        this.institusjonID = institusjonID;
        this.innledningFritekst = innledningFritekst;
        this.manglerFritekst = manglerFritekst;
        this.begrunnelseFritekst = begrunnelseFritekst;
        this.ektefelleFritekst = ektefelleFritekst;
        this.barnFritekst = barnFritekst;
        this.trygdeavgiftFritekst = trygdeavgiftFritekst;
        this.kontaktpersonNavn = kontaktpersonNavn;
        this.kopiMottakere = kopiMottakere;
        this.bestillersId = bestillersId;
        this.fritekstTittel = fritekstTittel;
        this.fritekst = fritekst;
        this.distribusjonstype = distribusjonstype;
        this.skalViseStandardTekstOmkontaktopplysninger = skalViseStandardTekstOmkontaktopplysninger;
        this.nyVurderingBakgrunn = nyVurderingBakgrunn;
        this.saksVedlegg = saksVedlegg;
        this.standardvedleggType = standardvedleggType;
        this.fritekstvedlegg = fritekstvedlegg;
        this.dokumentTittel = dokumentTittel;
        this.saksbehandlerNrToIdent = saksbehandlerNrToIdent;
        this.skalViseStandardTekstOmOpplysninger = skalViseStandardTekstOmOpplysninger;
        this.begrunnelseKode = begrunnelseKode;
        this.ytterligereInformasjon = ytterligereInformasjon;
        this.fakturanummer = fakturanummer;
        this.betalingsstatus = betalingsstatus;
        this.fullmektigForBetaling = fullmektigForBetaling;
        this.betalingsfrist = betalingsfrist;
        this.annenPersonMottakerIdent = annenPersonMottakerIdent;
        this.opphørtDato = opphørtDato;
        this.erInnvilgelse = erInnvilgelse;
        this.erEøsPensjonist = erEøsPensjonist;
    }


    public static BrevbestillingDto av(BrevbestillingUtkast brevbestillingUtkast) {
        String institusjonID = null;
        String bestillersId = null;
        String deprecatedBegrunnelseKode = null;
        String deprecatedYtterligereInformasjon = null;

        return new BrevbestillingDto(
            brevbestillingUtkast.produserbardokument(),
            brevbestillingUtkast.mottaker(),
            brevbestillingUtkast.orgnr(),
            brevbestillingUtkast.orgnrNorskMyndighet(),
            institusjonID,
            brevbestillingUtkast.innledningFritekst(),
            brevbestillingUtkast.manglerFritekst(),
            brevbestillingUtkast.begrunnelseFritekst(),
            brevbestillingUtkast.ektefelleFritekst(),
            brevbestillingUtkast.barnFritekst(),
            brevbestillingUtkast.trygdeavgiftFritekst(),
            brevbestillingUtkast.kontaktpersonNavn(),
            brevbestillingUtkast.kopiMottakere().stream().map(KopiMottakerDto::av).toList(),
            bestillersId,
            brevbestillingUtkast.fritekstTittel(),
            brevbestillingUtkast.fritekst(),
            brevbestillingUtkast.distribusjonstype(),
            brevbestillingUtkast.kontaktopplysninger(),
            brevbestillingUtkast.nyVurderingBakgrunn(),
            brevbestillingUtkast.saksVedlegg().stream().map(SaksvedleggDto::av).toList(),
            brevbestillingUtkast.standardvedleggType(),
            brevbestillingUtkast.fritekstVedlegg().stream().map(FritekstvedleggDto::av).toList(),
            brevbestillingUtkast.dokumentTittel(),
            brevbestillingUtkast.saksbehandlerNrToIdent(),
            brevbestillingUtkast.skalViseStandardTekstOmOpplysninger(),
            deprecatedBegrunnelseKode,
            deprecatedYtterligereInformasjon,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false
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

    public void setInstitusjonID(String institusjonID) {
        this.institusjonID = institusjonID;
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

    public void setTrygdeavtaleFritekst(String trygdeavgiftFritekst) {
        this.trygdeavgiftFritekst = trygdeavgiftFritekst;
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

    public void setSkalViseStandardTekstOmkontaktopplysninger(boolean skalViseStandardTekstOmkontaktopplysninger) {
        this.skalViseStandardTekstOmkontaktopplysninger = skalViseStandardTekstOmkontaktopplysninger;
    }

    public void setNyVurderingBakgrunn(String nyVurderingBakgrunn) {
        this.nyVurderingBakgrunn = nyVurderingBakgrunn;
    }

    public void setSaksVedlegg(List<SaksvedleggDto> saksVedlegg) {
        this.saksVedlegg = saksVedlegg;
    }

    public void setStandardvedleggType(StandardvedleggType standardvedleggType) {
        this.standardvedleggType = standardvedleggType;
    }

    public void setFritekstvedlegg(List<FritekstvedleggDto> fritekstvedlegg) {
        this.fritekstvedlegg = fritekstvedlegg;
    }

    public void setDokumentTittel(String dokumentTittel) {
        this.dokumentTittel = dokumentTittel;
    }

    public void setSaksbehandlerNrToIdent(String saksbehandlerNrToIdent) {
        this.saksbehandlerNrToIdent = saksbehandlerNrToIdent;
    }

    public void setBegrunnelseKode(String begrunnelseKode) {
        this.begrunnelseKode = begrunnelseKode;
    }

    public void setYtterligereInformasjon(String ytterligereInformasjon) {
        this.ytterligereInformasjon = ytterligereInformasjon;
    }

    public boolean isSkalViseStandardTekstOmOpplysninger() {
        return skalViseStandardTekstOmOpplysninger;
    }

    public void setSkalViseStandardTekstOmOpplysninger(boolean skalViseStandardTekstOmOpplysninger) {
        this.skalViseStandardTekstOmOpplysninger = skalViseStandardTekstOmOpplysninger;
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

    public String getInstitusjonID() {
        return institusjonID;
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

    public String getTrygdeavgiftFritekst() {
        return trygdeavgiftFritekst;
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

    public boolean isSkalViseStandardTekstOmKontaktopplysninger() {
        return skalViseStandardTekstOmkontaktopplysninger;
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

    public StandardvedleggType getStandardvedleggType() {
        return standardvedleggType;
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

    public String getSaksbehandlerNrToIdent() {
        return saksbehandlerNrToIdent;
    }

    public String getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public String getYtterligereInformasjon() {
        return ytterligereInformasjon;
    }

    public void setTrygdeavgiftFritekst(String trygdeavgiftFritekst) {
        this.trygdeavgiftFritekst = trygdeavgiftFritekst;
    }

    public LocalDate getOpphørtDato() {
        return opphørtDato;
    }

    public void setOpphørtDato(LocalDate opphørtDato) {
        this.opphørtDato = opphørtDato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrevbestillingDto that = (BrevbestillingDto) o;
        return skalViseStandardTekstOmkontaktopplysninger == that.skalViseStandardTekstOmkontaktopplysninger && produserbardokument == that.produserbardokument && mottaker == that.mottaker &&
            Objects.equals(orgnr, that.orgnr) && Objects.equals(orgnrNorskMyndighet, that.orgnrNorskMyndighet) &&
            Objects.equals(institusjonID, that.institusjonID) && Objects.equals(innledningFritekst, that.innledningFritekst) &&
            Objects.equals(manglerFritekst, that.manglerFritekst) && Objects.equals(begrunnelseFritekst, that.begrunnelseFritekst) &&
            Objects.equals(ektefelleFritekst, that.ektefelleFritekst) && Objects.equals(barnFritekst, that.barnFritekst) &&
            Objects.equals(trygdeavgiftFritekst, that.trygdeavgiftFritekst) && Objects.equals(kontaktpersonNavn, that.kontaktpersonNavn) &&
            Objects.equals(kopiMottakere, that.kopiMottakere) && Objects.equals(bestillersId, that.bestillersId) &&
            Objects.equals(fritekstTittel, that.fritekstTittel) && Objects.equals(fritekst, that.fritekst) &&
            distribusjonstype == that.distribusjonstype && Objects.equals(nyVurderingBakgrunn, that.nyVurderingBakgrunn) &&
            Objects.equals(saksVedlegg, that.saksVedlegg) && Objects.equals(fritekstvedlegg, that.fritekstvedlegg) &&
            Objects.equals(dokumentTittel, that.dokumentTittel) && Objects.equals(saksbehandlerNrToIdent, that.saksbehandlerNrToIdent) &&
            Objects.equals(begrunnelseKode, that.begrunnelseKode) && Objects.equals(ytterligereInformasjon, that.ytterligereInformasjon) &&
            Objects.equals(fakturanummer, that.fakturanummer) && betalingsstatus == that.betalingsstatus &&
            Objects.equals(fullmektigForBetaling, that.fullmektigForBetaling) && Objects.equals(betalingsfrist, that.betalingsfrist) &&
            Objects.equals(annenPersonMottakerIdent, that.annenPersonMottakerIdent) && Objects.equals(opphørtDato, that.opphørtDato) && Objects.equals(erInnvilgelse, that.erInnvilgelse) &&
            Objects.equals(erEøsPensjonist, that.erEøsPensjonist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(produserbardokument, mottaker, orgnr, orgnrNorskMyndighet, institusjonID, innledningFritekst, manglerFritekst,
            begrunnelseFritekst, ektefelleFritekst, barnFritekst, trygdeavgiftFritekst, kontaktpersonNavn, kopiMottakere, bestillersId, fritekstTittel, fritekst, distribusjonstype, skalViseStandardTekstOmkontaktopplysninger, nyVurderingBakgrunn, saksVedlegg, fritekstvedlegg, dokumentTittel, saksbehandlerNrToIdent, begrunnelseKode, ytterligereInformasjon, fakturanummer, betalingsstatus, fullmektigForBetaling, betalingsfrist, annenPersonMottakerIdent, opphørtDato);
    }

    @Override
    public String toString() {
        return "BrevbestillingDto{" +
            "produserbardokument=" + produserbardokument +
            ", mottaker=" + mottaker +
            ", orgnr='" + orgnr + '\'' +
            ", orgnrNorskMyndighet=" + orgnrNorskMyndighet +
            ", institusjonID='" + institusjonID + '\'' +
            ", innledningFritekst='" + innledningFritekst + '\'' +
            ", manglerFritekst='" + manglerFritekst + '\'' +
            ", begrunnelseFritekst='" + begrunnelseFritekst + '\'' +
            ", ektefelleFritekst='" + ektefelleFritekst + '\'' +
            ", barnFritekst='" + barnFritekst + '\'' +
            ", trygdeavgiftFritekst='" + trygdeavgiftFritekst + '\'' +
            ", kontaktpersonNavn='" + kontaktpersonNavn + '\'' +
            ", kopiMottakere=" + kopiMottakere +
            ", bestillersId='" + bestillersId + '\'' +
            ", fritekstTittel='" + fritekstTittel + '\'' +
            ", fritekst='" + fritekst + '\'' +
            ", distribusjonstype=" + distribusjonstype +
            ", kontaktopplysninger=" + skalViseStandardTekstOmkontaktopplysninger +
            ", nyVurderingBakgrunn='" + nyVurderingBakgrunn + '\'' +
            ", saksVedlegg=" + saksVedlegg +
            ", fritekstvedlegg=" + fritekstvedlegg +
            ", dokumentTittel='" + dokumentTittel + '\'' +
            ", saksbehandlerNrToIdent='" + saksbehandlerNrToIdent + '\'' +
            ", begrunnelseKode='" + begrunnelseKode + '\'' +
            ", ytterligereInformasjon='" + ytterligereInformasjon + '\'' +
            ", fakturanummer='" + fakturanummer + '\'' +
            ", betalingsstatus=" + betalingsstatus +
            ", fullmektigForBetaling='" + fullmektigForBetaling + '\'' +
            ", betalingsfrist=" + betalingsfrist +
            ", annenPersonMottakerIdent='" + annenPersonMottakerIdent + '\'' +
            ", opphørtDato='" + opphørtDato + '\'' +
            ", erInnvilgelse='" + erInnvilgelse + '\'' +
            ", erEøsPensjonist='" + erEøsPensjonist + '\'' +
            '}';
    }

    public String getFakturanummer() {
        return fakturanummer;
    }

    public void setFakturanummer(String fakturanummer) {
        this.fakturanummer = fakturanummer;
    }

    public Betalingsstatus getBetalingsstatus() {
        return betalingsstatus;
    }

    public void setBetalingsstatus(Betalingsstatus betalingsstatus) {
        this.betalingsstatus = betalingsstatus;
    }

    public String getFullmektigForBetaling() {
        return fullmektigForBetaling;
    }

    public void setFullmektigForBetaling(String fullmektigForBetaling) {
        this.fullmektigForBetaling = fullmektigForBetaling;
    }

    public LocalDate getBetalingsfrist() {
        return betalingsfrist;
    }

    public void setBetalingsfrist(LocalDate betalingsfrist) {
        this.betalingsfrist = betalingsfrist;
    }

    public String getAnnenPersonMottakerIdent() {
        return annenPersonMottakerIdent;
    }

    public void setAnnenPersonMottakerIdent(String annenPersonMottakerIdent) {
        this.annenPersonMottakerIdent = annenPersonMottakerIdent;
    }

    public boolean isErInnvilgelse() {
        return erInnvilgelse;
    }

    public void setErInnvilgelse(boolean erInnvilgelse) {
        this.erInnvilgelse = erInnvilgelse;
    }

    public boolean isErEøsPensjonist() { return erEøsPensjonist; }

    public void setErEøsPensjonist(boolean erEøsPensjonist) {
        this.erEøsPensjonist = erEøsPensjonist;
    }

}
