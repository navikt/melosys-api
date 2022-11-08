package no.nav.melosys.domain.mottatteopplysninger.data;

import java.math.BigDecimal;

public class LoennOgGodtgjoerelse {
    public Boolean norskArbgUtbetalerLoenn;
    public Boolean erArbeidstakerAnsattHelePerioden;
    public Boolean utlArbgUtbetalerLoenn;
    public Boolean utlArbTilhoererSammeKonsern;
    public BigDecimal bruttoLoennPerMnd;
    public BigDecimal bruttoLoennUtlandPerMnd;
    public Boolean mottarNaturalytelser;
    public BigDecimal samletVerdiNaturalytelser;
    public Boolean erArbeidsgiveravgiftHelePerioden;
    public Boolean erTrukketTrygdeavgift;

    public LoennOgGodtgjoerelse() {
    }

    public LoennOgGodtgjoerelse(Boolean norskArbgUtbetalerLoenn, Boolean erArbeidstakerAnsattHelePerioden,
                                Boolean utlArbgUtbetalerLoenn, Boolean utlArbTilhoererSammeKonsern,
                                BigDecimal bruttoLoennPerMnd, BigDecimal bruttoLoennUtlandPerMnd,
                                Boolean mottarNaturalytelser, BigDecimal samletVerdiNaturalytelser,
                                Boolean erArbeidsgiveravgiftHelePerioden, Boolean erTrukketTrygdeavgift) {
        this.norskArbgUtbetalerLoenn = norskArbgUtbetalerLoenn;
        this.erArbeidstakerAnsattHelePerioden = erArbeidstakerAnsattHelePerioden;
        this.utlArbgUtbetalerLoenn = utlArbgUtbetalerLoenn;
        this.utlArbTilhoererSammeKonsern = utlArbTilhoererSammeKonsern;
        this.bruttoLoennPerMnd = bruttoLoennPerMnd;
        this.bruttoLoennUtlandPerMnd = bruttoLoennUtlandPerMnd;
        this.mottarNaturalytelser = mottarNaturalytelser;
        this.samletVerdiNaturalytelser = samletVerdiNaturalytelser;
        this.erArbeidsgiveravgiftHelePerioden = erArbeidsgiveravgiftHelePerioden;
        this.erTrukketTrygdeavgift = erTrukketTrygdeavgift;
    }
}
