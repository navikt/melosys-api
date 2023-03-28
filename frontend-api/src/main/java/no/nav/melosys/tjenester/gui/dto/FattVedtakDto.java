package no.nav.melosys.tjenester.gui.dto;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FaktureringsIntervall;
import no.nav.melosys.service.dokument.brev.KopiMottakerDto;

public class FattVedtakDto {
    private Behandlingsresultattyper behandlingsresultatTypeKode;
    private Vedtakstyper vedtakstype;
    private String fritekst;
    private String fritekstSed;
    private Set<String> mottakerinstitusjoner;
    private String nyVurderingBakgrunn;
    private String innledningFritekst;
    private String begrunnelseFritekst;
    private String ektefelleFritekst;
    private String barnFritekst;
    private List<KopiMottakerDto> kopiMottakere;
    private FaktureringsIntervall betalingsintervall;
    private Boolean kopiTilArbeidsgiver;

    public FaktureringsIntervall getBetalingsintervall() {
        return betalingsintervall;
    }

    public void setBetalingsintervall(FaktureringsIntervall betalingsintervall) {
        this.betalingsintervall = betalingsintervall;
    }

    public Behandlingsresultattyper getBehandlingsresultatTypeKode() {
        return behandlingsresultatTypeKode;
    }

    public void setBehandlingsresultatTypeKode(Behandlingsresultattyper behandlingsresultatTypeKode) {
        this.behandlingsresultatTypeKode = behandlingsresultatTypeKode;
    }

    public Boolean getKopiTilArbeidsgiver() {
        return kopiTilArbeidsgiver;
    }

    public void setKopiTilArbeidsgiver(final Boolean kopiTilArbeidsgiver) {
        this.kopiTilArbeidsgiver = kopiTilArbeidsgiver;
    }
    
    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(final String fritekst) {
        this.fritekst = fritekst;
    }

    public String getFritekstSed() {
        return fritekstSed;
    }

    public void setFritekstSed(String fritekstSed) {
        this.fritekstSed = fritekstSed;
    }

    public Set<String> getMottakerinstitusjoner() {
        return mottakerinstitusjoner;
    }

    public void setMottakerinstitusjoner(Set<String> mottakerinstitusjoner) {
        this.mottakerinstitusjoner = mottakerinstitusjoner;
    }

    public void setNyVurderingBakgrunn(String nyVurderingBakgrunn) {
        this.nyVurderingBakgrunn = nyVurderingBakgrunn;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
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

    public List<KopiMottakerDto> getKopiMottakere() {
        return kopiMottakere;
    }

    public String getNyVurderingBakgrunn() {
        return nyVurderingBakgrunn;
    }

    public void setInnledningFritekst(String innledningFritekst) {
        this.innledningFritekst = innledningFritekst;
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

    public void setKopiMottakere(List<KopiMottakerDto> kopiMottakere) {
        this.kopiMottakere = kopiMottakere;
    }

    public Vedtakstyper getVedtakstype() {
        return vedtakstype;
    }

    public void setVedtakstype(Vedtakstyper vedtakstype) {
        this.vedtakstype = vedtakstype;
    }
}
