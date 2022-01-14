package no.nav.melosys.integrasjon.dokgen.dto;


import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avsendertyper;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;

public class SaksbehandlingstidSoknad extends DokgenDto {
    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoMottatt;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoBehandlingstid;

    private final String typeSoknad;
    private final Aktoersroller avsenderTypeSoknad;
    private final String avsenderSoknad;
    private final String avsenderLand;

    public SaksbehandlingstidSoknad(DokgenBrevbestilling brevbestilling) {
        super(brevbestilling, Aktoersroller.BRUKER);
        this.datoMottatt = brevbestilling.getForsendelseMottatt();
        this.datoBehandlingstid = brevbestilling.getForsendelseMottatt().plus(SAKSBEHANDLINGSTID_DAGER, ChronoUnit.DAYS);
        this.typeSoknad = brevbestilling.getBehandling().getFagsak().getType().getKode();
        this.avsenderTypeSoknad = utledAvsendertype(brevbestilling.getAvsendertype());
        this.avsenderSoknad = brevbestilling.getAvsenderNavn();
        this.avsenderLand = brevbestilling.getAvsenderLand();
    }

    public static SaksbehandlingstidSoknad av(DokgenBrevbestilling brevbestilling) {
        return new SaksbehandlingstidSoknad(brevbestilling);
    }

    public Instant getDatoMottatt() {
        return datoMottatt;
    }

    public Instant getDatoBehandlingstid() {
        return datoBehandlingstid;
    }

    public String getTypeSoknad() {
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

    private Aktoersroller utledAvsendertype(Avsendertyper avsendertype) {
        if (avsendertype == null) {
            return BRUKER;
        }
        return switch (avsendertype) {
            case PERSON -> BRUKER;
            case ORGANISASJON -> REPRESENTANT;
            case UTENLANDSK_TRYGDEMYNDIGHET -> MYNDIGHET;
        };
    }
}
