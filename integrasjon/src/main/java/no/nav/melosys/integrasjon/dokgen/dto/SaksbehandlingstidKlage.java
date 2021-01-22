package no.nav.melosys.integrasjon.dokgen.dto;


import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.exception.TekniskException;

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

    public SaksbehandlingstidKlage(DokgenBrevbestilling brevbestilling) throws TekniskException {
        super(brevbestilling);
        this.datoMottatt = brevbestilling.getForsendelseMottatt();
        this.datoBehandlingstid = brevbestilling.getForsendelseMottatt().plus(SAKSBEHANDLINGSTID_DAGER, ChronoUnit.DAYS);
        this.datoVedtak = hentVedtaksdato(brevbestilling.getBehandlingsresultat());
    }

    public static SaksbehandlingstidKlage av(DokgenBrevbestilling brevbestilling) throws TekniskException {
        return new SaksbehandlingstidKlage(brevbestilling);
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

}
