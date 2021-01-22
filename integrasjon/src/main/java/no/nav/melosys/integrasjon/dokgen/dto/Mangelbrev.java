package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.time.Period;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.DokgenMetaKey;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;

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

    protected Mangelbrev(DokgenBrevbestilling brevbestilling) throws TekniskException, IkkeFunnetException {
        super(brevbestilling);
        Fagsak fagsak = brevbestilling.getBehandling().getFagsak();

        this.datoMottatt = brevbestilling.getForsendelseMottatt();
        this.datoVedtatt = hentVedtaksdato(brevbestilling.getBehandlingsresultat());
        this.datoInnsendingsfrist = Instant.now().plus(Period.ofWeeks(DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV));
        this.sakstype = fagsak.getType();
        this.behandlingstype = fagsak.getSistOppdaterteBehandling().getType();
        this.saksbehandlerNavn = fagsak.getEndretAv();
        this.fritekstMangelinfo = brevbestilling.getVariabeltFelt(DokgenMetaKey.FRITEKST_MANGELINFO, String.class);
        this.fritekstMottaksinfo = brevbestilling.getVariabeltFelt(DokgenMetaKey.FRITEKST_MOTTAKSINFO, String.class);
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
}
