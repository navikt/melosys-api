package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.time.Period;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.brev.AvslagBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class Avslagbrev extends DokgenDto {

    private final String fritekst;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoInnsendingsfrist;

    @JsonInclude
    @JsonFormat(shape = STRING)
    private final List<Instant> mangelbrevDatoer;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoMottatt;

    private final String sakstype;

    private Avslagbrev(AvslagBrevbestilling brevbestilling, Aktoersroller mottakerType, List<Instant> mangelbrevDatoer) {
        super(brevbestilling, mottakerType);
        this.fritekst = brevbestilling.getFritekst();
        var fagsak = brevbestilling.getBehandling().getFagsak();

        this.sakstype = fagsak.getType().getKode();
        this.mangelbrevDatoer = mangelbrevDatoer;
        this.datoInnsendingsfrist = mangelbrevDatoer.size() > 0
            ? Collections.max(mangelbrevDatoer).plus(Period.ofWeeks(DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV))
            : null;
        this.datoMottatt = brevbestilling.getForsendelseMottatt();
    }

    public String getFritekst() {
        return fritekst;
    }

    public static Avslagbrev av(AvslagBrevbestilling brevbestilling, List<Instant> mangelbrevDatoer) {
        return new Avslagbrev(brevbestilling, Aktoersroller.BRUKER, mangelbrevDatoer);
    }

    public String getSakstype() {
        return sakstype;
    }

    public Instant getDatoInnsendingsfrist() {
        return datoInnsendingsfrist;
    }

    public List<Instant> getMangelbrevDatoer() {
        return mangelbrevDatoer;
    }

    public Instant getDatoMottatt() {
        return datoMottatt;
    }
}
