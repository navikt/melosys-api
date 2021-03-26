package no.nav.melosys.integrasjon.dokgen.dto;


import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.TekniskException;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.REPRESENTANT;

public class SaksbehandlingstidSoknad extends DokgenDto {
    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoMottatt;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoBehandlingstid;

    private final Sakstyper typeSoknad;
    private final Aktoersroller avsenderTypeSoknad;
    private final String avsenderSoknad;
    private final String avsenderLand;

    public SaksbehandlingstidSoknad(DokgenBrevbestilling brevbestilling) throws TekniskException {
        super(brevbestilling);

        Behandling behandling = brevbestilling.getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        PersonDokument personDokument = behandling.hentPersonDokument();

        this.datoMottatt = brevbestilling.getForsendelseMottatt();
        this.datoBehandlingstid = brevbestilling.getForsendelseMottatt().plus(SAKSBEHANDLINGSTID_DAGER, ChronoUnit.DAYS);
        this.typeSoknad = fagsak.getType();
        this.avsenderTypeSoknad = (personDokument.fnr.equals(brevbestilling.getAvsenderId()) ? BRUKER : REPRESENTANT);
        this.avsenderSoknad = brevbestilling.getAvsenderNavn();
        this.avsenderLand = null; //NOTE Mangler inntil vi kan avgjøre om avsender == MYNDIGHET
    }

    public static SaksbehandlingstidSoknad av(DokgenBrevbestilling brevbestilling) throws TekniskException {
        return new SaksbehandlingstidSoknad(brevbestilling);
    }

    public Instant getDatoMottatt() {
        return datoMottatt;
    }

    public Instant getDatoBehandlingstid() {
        return datoBehandlingstid;
    }

    public Sakstyper getTypeSoknad() {
        return typeSoknad;
    }

    public Aktoersroller getAvsenderTypeSoknad() {
        return avsenderTypeSoknad;
    }

    public String getAvsenderSoknad() {
        return avsenderSoknad;
    }

    public String getAvsenderLand() {
        return avsenderLand;
    }
}
