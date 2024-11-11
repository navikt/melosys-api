package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FaktureringIntervall;
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
    private String trygdeavgiftFritekst;
    private List<KopiMottakerDto> kopiMottakere;
    private FaktureringIntervall betalingsintervall;
    private Boolean kopiTilArbeidsgiver;
    private LocalDate opphoerDato;

    public FaktureringIntervall getBetalingsintervall() {
        return betalingsintervall;
    }

    public void setBetalingsintervall(FaktureringIntervall betalingsintervall) {
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

    public String getTrygdeavgiftFritekst() {
        return trygdeavgiftFritekst;
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

    public void setTrygdeavgiftFritekst(String trygdeavgiftFritekst) {
        this.trygdeavgiftFritekst = trygdeavgiftFritekst;
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

    public LocalDate getOpphoerDato() {
        return opphoerDato;
    }

    public void setOpphoerDato(LocalDate opphoerDato) {
        this.opphoerDato = opphoerDato;
    }
}
