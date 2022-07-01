package no.nav.melosys.integrasjon.dokgen.dto;


import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class SaksbehandlingstidKlage extends DokgenDto {
    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoMottatt;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoBehandlingstid;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoVedtak;

    public SaksbehandlingstidKlage(DokgenBrevbestilling brevbestilling, Instant datoBehandlingstid) {
        super(brevbestilling, Aktoersroller.BRUKER);
        this.datoMottatt = brevbestilling.getForsendelseMottatt();
        this.datoBehandlingstid = datoBehandlingstid;
        this.datoVedtak = brevbestilling.getVedtaksdato();
    }

    public static SaksbehandlingstidKlage av(DokgenBrevbestilling brevbestilling, Instant datoBehandlingstid) {
        return new SaksbehandlingstidKlage(brevbestilling, datoBehandlingstid);
    }

    public Instant getDatoMottatt() {
        return datoMottatt;
    }

    public Instant getDatoBehandlingstid() {
        return datoBehandlingstid;
    }

    public Instant getDatoVedtak() {
        return datoVedtak;
    }

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }
}
