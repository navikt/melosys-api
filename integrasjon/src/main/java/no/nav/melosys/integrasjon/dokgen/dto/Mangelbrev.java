package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

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

    private final Sakstyper sakstype;
    private final Behandlingstyper behandlingstype;
    private final String saksbehandlerNavn;
    private final String fritekstMangelinfo;
    private final String fritekstMottaksinfo;

    protected Mangelbrev(String fnr, String saksnummer, Instant dagensDato, String navnBruker, String navnMottaker,
                         List<String> adresselinjer, String postnr, String poststed, String land,
                         Instant datoMottatt, Instant datoVedtatt, Instant datoInnsendingsfrist, Sakstyper sakstype,
                         Behandlingstyper behandlingstype, String saksbehandlerNavn, String fritekstMangelinfo,
                         String fritekstMottaksinfo) {
        super(fnr, saksnummer, dagensDato, navnBruker, navnMottaker, adresselinjer, postnr, poststed, land);
        this.datoMottatt = datoMottatt;
        this.datoVedtatt = datoVedtatt;
        this.datoInnsendingsfrist = datoInnsendingsfrist;
        this.sakstype = sakstype;
        this.behandlingstype = behandlingstype;
        this.saksbehandlerNavn = saksbehandlerNavn;
        this.fritekstMangelinfo = fritekstMangelinfo;
        this.fritekstMottaksinfo = fritekstMottaksinfo;
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

    public Sakstyper getSakstype() {
        return sakstype;
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public String getSaksbehandlerNavn() {
        return saksbehandlerNavn;
    }

    public String getFritekstMangelinfo() {
        return fritekstMangelinfo;
    }

    public String getFritekstMottaksinfo() {
        return fritekstMottaksinfo;
    }

    protected static Instant hentVedtaksdato(Behandlingsresultat behandlingsresultat) {
        return (behandlingsresultat != null && behandlingsresultat.harVedtak()) ?
            behandlingsresultat.getVedtakMetadata().getVedtaksdato() : null;
    }
}
