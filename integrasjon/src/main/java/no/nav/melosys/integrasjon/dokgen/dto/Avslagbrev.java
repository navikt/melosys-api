package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import no.nav.melosys.domain.brev.AvslagBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class Avslagbrev extends DokgenDto {

    private final String fritekst;

    @JsonInclude
    @JsonFormat(shape = STRING)
    private final List<LocalDate> mangelbrevDatoer;

    @JsonFormat(shape = STRING)
    private final LocalDate datoMottatt;

    private final String sakstype;

    private final String behandlingstype;

    private Avslagbrev(AvslagBrevbestilling brevbestilling,
                       Aktoersroller mottakerType,
                       List<Instant> mangelbrevDatoer) {
        super(brevbestilling, mottakerType);

        this.fritekst = brevbestilling.getAvslagFritekst();
        this.mangelbrevDatoer = mangelbrevDatoer.stream().map(this::instantTilLocalDate).collect(Collectors.toList());
        this.datoMottatt = instantTilLocalDate(brevbestilling.getForsendelseMottatt());
        this.sakstype = brevbestilling.getBehandling().getFagsak().getType().getKode();
        this.behandlingstype = brevbestilling.getBehandling().getType().getKode();
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

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public List<LocalDate> getMangelbrevDatoer() {
        return mangelbrevDatoer;
    }

    public LocalDate getDatoMottatt() {
        return datoMottatt;
    }
}
