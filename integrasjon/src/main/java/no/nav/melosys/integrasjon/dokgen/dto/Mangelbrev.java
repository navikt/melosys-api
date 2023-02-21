package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class Mangelbrev extends DokgenDto {

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoMottatt;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoVedtatt;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoInnsendingsfrist;

    private final String sakstype;
    private final String sakstema;
    private final String behandlingstype;
    private final String manglerInfoFritekst;
    private final String innledningFritekst;

    protected Mangelbrev(MangelbrevBrevbestilling brevbestilling, Instant datoInnsendingsfrist) {
        super(brevbestilling, Mottakerroller.BRUKER); // TODO i MELOSYS-5738
        var fagsak = brevbestilling.getBehandling().getFagsak();

        this.datoMottatt = brevbestilling.getForsendelseMottatt();
        this.datoVedtatt = brevbestilling.getVedtaksdato();
        this.datoInnsendingsfrist = datoInnsendingsfrist;
        this.sakstype = fagsak.getType().getKode();
        this.sakstema = fagsak.getTema().getKode();
        this.behandlingstype = fagsak.hentSistOppdatertBehandling().getType().getKode();
        this.manglerInfoFritekst = brevbestilling.getManglerInfoFritekst();
        this.innledningFritekst = brevbestilling.getInnledningFritekst();
    }

    public Instant getDatoMottatt() {
        return datoMottatt;
    }

    public Instant getDatoVedtatt() {
        return datoVedtatt;
    }

    public Instant getDatoInnsendingsfrist() {
        return datoInnsendingsfrist;
    }

    public String getSakstype() {
        return sakstype;
    }

    public String getSakstema() {
        return sakstema;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public String getManglerInfoFritekst() {
        return manglerInfoFritekst;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }
}
