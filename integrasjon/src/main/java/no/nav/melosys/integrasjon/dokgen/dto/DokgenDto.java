package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Saksinfo;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.mapMottaker;

@JsonInclude(Include.NON_EMPTY)
public abstract class DokgenDto {
    private final Saksinfo saksinfo;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant dagensDato;

    private final String saksbehandlerNavn;
    private Mottaker mottaker;
    private final boolean toggleEnabled;

    protected DokgenDto(DokgenBrevbestilling brevbestilling, Aktoersroller mottakerType, Saksinfo saksinfo, boolean toggleEnabled) {
        this.saksinfo = saksinfo;
        this.dagensDato = Instant.now();
        this.saksbehandlerNavn = brevbestilling.getSaksbehandlerNavn();
        this.mottaker = mapMottaker(brevbestilling, mottakerType);
        this.toggleEnabled = toggleEnabled;
    }

    protected DokgenDto(DokgenBrevbestilling brevbestilling, Aktoersroller mottakerType, Saksinfo saksinfo) {
        this(brevbestilling, mottakerType, saksinfo, false);
    }

    protected DokgenDto(DokgenBrevbestilling brevbestilling, Aktoersroller mottakerType, boolean toggleEnabled) {
        this(brevbestilling, mottakerType, SaksinfoBruker.av(brevbestilling), toggleEnabled);
    }

    protected DokgenDto(DokgenBrevbestilling brevbestilling, Aktoersroller mottakerType) {
        this(brevbestilling, mottakerType, SaksinfoBruker.av(brevbestilling));
    }

    public Saksinfo getSaksinfo() {
        return saksinfo;
    }

    public Instant getDagensDato() {
        return dagensDato;
    }

    public String getSaksbehandlerNavn() {
        return saksbehandlerNavn;
    }

    public Mottaker getMottaker() {
        return mottaker;
    }

    public void setMottaker(Mottaker mottaker) {
        this.mottaker = mottaker;
    }

    protected LocalDate instantTilLocalDate(Instant datoOgTid) {
        return datoOgTid != null ? LocalDate.ofInstant(datoOgTid, ZoneId.systemDefault()) : null;
    }

    public boolean isToggleEnabled() {
        return toggleEnabled;
    }
}
