package no.nav.melosys.tjenester.gui.dto;

import static no.nav.melosys.tjenester.gui.dto.DtoUtils.tilLocalDate;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsavtale;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidstidsordninger;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Avloenningstyper;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Fartsomraader;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.MaritimArbeidsavtale;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Skipsregistre;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Skipstyper;

public class ArbeidsavtaleDto {

    private String arbeidstidsordning;

    private String avloenningstype;

    private String yrke;

    private Float avtaltArbeidstimerPerUke;

    private Float stillingsprosent;

    private LocalDate sisteLoennsendringsdato;

    private Float beregnetAntallTimerPrUke;

    private LocalDate endringsdatoStillingsprosent;

    @JsonProperty("fartsomraade")
    private String fartsområde;

    private String skipsregister;

    private String skipstype;

    public ArbeidsavtaleDto() {
    }

    public static ArbeidsavtaleDto tilDto(Arbeidsavtale avtale) {

        ArbeidsavtaleDto arbeidsavtaleDto = new ArbeidsavtaleDto();

        // For å kunne vurdere hvilke gruppen bruker faller under og hvilke artiklene skal vurderes
        Arbeidstidsordninger arbeidstidsordning = avtale.getArbeidstidsordning();
        // TODO Kodeverk
        arbeidsavtaleDto.setArbeidstidsordning(arbeidstidsordning != null ? arbeidstidsordning.getValue() : null);

        Avloenningstyper avloenningstype = avtale.getAvloenningstype();
        arbeidsavtaleDto.setAvloenningstype(avloenningstype != null ? avloenningstype.getValue() : null);
        arbeidsavtaleDto.setSisteLoennsendringsdato(tilLocalDate(avtale.getSisteLoennsendringsdato()));

        // Yrkesbetegnelse. Nødvendig for statistikk til EU
        arbeidsavtaleDto.setYrke(avtale.getYrke().getValue());

        BigDecimal avtaltArbeidstimerPerUke = avtale.getAvtaltArbeidstimerPerUke();
        arbeidsavtaleDto.setAvtaltArbeidstimerPerUke(avtaltArbeidstimerPerUke != null ? avtaltArbeidstimerPerUke.floatValue() : null);

        BigDecimal beregnetAntallTimerPrUke = avtale.getBeregnetAntallTimerPrUke();
        arbeidsavtaleDto.setBeregnetAntallTimerPrUke(beregnetAntallTimerPrUke != null ? beregnetAntallTimerPrUke.floatValue() : null);

        // Stillingsprosent
        // Både når en jobber i utlandet, og den periode før. Dette for å avdekke reell utsending
        BigDecimal stillingsprosent = avtale.getStillingsprosent();
        arbeidsavtaleDto.setStillingsprosent(stillingsprosent != null ? stillingsprosent.floatValue() : null);
        arbeidsavtaleDto.setEndringsdatoStillingsprosent(tilLocalDate(avtale.getEndringsdatoStillingsprosent()));

        // Maritimt arbeidsavtale
        if (avtale instanceof MaritimArbeidsavtale) {
            MaritimArbeidsavtale maritimArbeidsavtale = (MaritimArbeidsavtale) avtale;

            Fartsomraader fartsomraadeXml = maritimArbeidsavtale.getFartsomraade();
            String fartsomraade = (fartsomraadeXml == null) ? null : fartsomraadeXml.getValue();
            arbeidsavtaleDto.setFartsområde(fartsomraade);

            Skipsregistre skipsregisterXml = maritimArbeidsavtale.getSkipsregister();
            String skipregister = (skipsregisterXml == null) ? null : skipsregisterXml.getValue();
            arbeidsavtaleDto.setSkipsregister(skipregister);

            Skipstyper skipstypeXml = maritimArbeidsavtale.getSkipstype();
            String skipstype = (skipstypeXml == null) ? null : skipstypeXml.getValue();
            arbeidsavtaleDto.setSkipstype(skipstype);
        }

        return arbeidsavtaleDto;
    }

    public String getArbeidstidsordning() {
        return arbeidstidsordning;
    }

    public void setArbeidstidsordning(String arbeidstidsordning) {
        this.arbeidstidsordning = arbeidstidsordning;
    }

    public String getAvloenningstype() {
        return avloenningstype;
    }

    public void setAvloenningstype(String avloenningstype) {
        this.avloenningstype = avloenningstype;
    }

    public String getYrke() {
        return yrke;
    }

    public void setYrke(String yrke) {
        this.yrke = yrke;
    }

    public Float getAvtaltArbeidstimerPerUke() {
        return avtaltArbeidstimerPerUke;
    }

    public void setAvtaltArbeidstimerPerUke(Float avtaltArbeidstimerPerUke) {
        this.avtaltArbeidstimerPerUke = avtaltArbeidstimerPerUke;
    }

    public Float getStillingsprosent() {
        return stillingsprosent;
    }

    public void setStillingsprosent(Float stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }

    public LocalDate getSisteLoennsendringsdato() {
        return sisteLoennsendringsdato;
    }

    public void setSisteLoennsendringsdato(LocalDate sisteLoennsendringsdato) {
        this.sisteLoennsendringsdato = sisteLoennsendringsdato;
    }

    public Float getBeregnetAntallTimerPrUke() {
        return beregnetAntallTimerPrUke;
    }

    public void setBeregnetAntallTimerPrUke(Float beregnetAntallTimerPrUke) {
        this.beregnetAntallTimerPrUke = beregnetAntallTimerPrUke;
    }

    public LocalDate getEndringsdatoStillingsprosent() {
        return endringsdatoStillingsprosent;
    }

    public void setEndringsdatoStillingsprosent(LocalDate endringsdatoStillingsprosent) {
        this.endringsdatoStillingsprosent = endringsdatoStillingsprosent;
    }

    public void setFartsområde(String fartsområde) {
        this.fartsområde = fartsområde;
    }

    public String getFartsområde() {
        return fartsområde;
    }

    public void setSkipsregister(String skipsregister) {
        this.skipsregister = skipsregister;
    }

    public String getSkipsregister() {
        return skipsregister;
    }

    public void setSkipstype(String skipstype) {
        this.skipstype = skipstype;
    }

    public String getSkipstype() {
        return skipstype;
    }
}