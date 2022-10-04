package no.nav.melosys.integrasjon.dokgen.dto;


import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;

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

    public SaksbehandlingstidSoknad(DokgenBrevbestilling brevbestilling, Instant datoBehandlingstid) {
        super(brevbestilling, Aktoersroller.BRUKER);
        this.datoMottatt = brevbestilling.getForsendelseMottatt();
        this.datoBehandlingstid = datoBehandlingstid;
        this.typeSoknad = brevbestilling.getBehandling().getFagsak().getType().getKode();
        this.avsenderTypeSoknad = utledAvsendertype(brevbestilling.getAvsendertype());
        this.avsenderSoknad = brevbestilling.getAvsenderID();
        this.avsenderLand = brevbestilling.getAvsenderLand();
    }

    public static SaksbehandlingstidSoknad av(DokgenBrevbestilling brevbestilling, Instant datoBehandlingstid) {
        return new SaksbehandlingstidSoknad(brevbestilling, datoBehandlingstid);
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

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }

    private Aktoersroller utledAvsendertype(Avsendertyper avsendertype) {
        if (avsendertype == null) {
            return BRUKER;
        }
        return switch (avsendertype) {
            case PERSON -> BRUKER;
            case ORGANISASJON -> REPRESENTANT;
            case UTENLANDSK_TRYGDEMYNDIGHET -> TRYGDEMYNDIGHET;
        };
    }
}
