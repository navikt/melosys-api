package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Saksopplysninger;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.mapMottaker;

@JsonInclude(Include.NON_EMPTY)
public abstract class DokgenDto {
    private final Saksopplysninger saksopplysninger;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant dagensDato;

    private final String saksbehandlerNavn;
    private Mottaker mottaker;

    // Saksbehandlingstid er 12 uker fra dato for utsendelse av brev, uavhengig av helg, helligdager, osv.
    protected static final int SAKSBEHANDLINGSTID_DAGER = 12 * 7;
    // Svarfrist mangelbrev 4 uker fra dato brevet blir generert.
    protected static final int DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV = 4;

    protected DokgenDto(DokgenBrevbestilling brevbestilling) {
        this.saksopplysninger = Saksopplysninger.av(brevbestilling);
        this.dagensDato = Instant.now();
        this.saksbehandlerNavn = brevbestilling.getSaksbehandlerNavn();
        this.mottaker = mapMottaker(brevbestilling, Aktoersroller.BRUKER);
    }

    protected DokgenDto(DokgenBrevbestilling brevbestilling, Aktoersroller mottakerType) {
        this.saksopplysninger = Saksopplysninger.av(brevbestilling);
        this.dagensDato = Instant.now();
        this.saksbehandlerNavn = brevbestilling.getSaksbehandlerNavn();
        this.mottaker = mapMottaker(brevbestilling, mottakerType);
    }

    public Saksopplysninger getSaksopplysninger() {
        return saksopplysninger;
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
}
