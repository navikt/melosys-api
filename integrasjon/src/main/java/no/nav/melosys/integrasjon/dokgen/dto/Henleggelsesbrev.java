package no.nav.melosys.integrasjon.dokgen.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.melosys.domain.brev.Henleggelsesbrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;

import java.time.LocalDate;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class Henleggelsesbrev extends DokgenDto {
    private final String fritekst;
    private final String begrunnelseKode;
    private final String behandlingstype;

    @JsonFormat(shape = STRING)
    private final LocalDate datoMottatt;

    private Henleggelsesbrev(Henleggelsesbrevbestilling brevbestilling, Aktoersroller mottakerType) {
        super(brevbestilling, mottakerType);
        this.fritekst = brevbestilling.getFritekst();
        this.begrunnelseKode = brevbestilling.getBegrunnelseKode();
        this.behandlingstype = brevbestilling.getBehandling().getType().getKode();
        this.datoMottatt = instantTilLocalDate(brevbestilling.getForsendelseMottatt());
    }

    public String getFritekst() {
        return fritekst;
    }

    public String getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public LocalDate getDatoMottatt() {
        return datoMottatt;
    }

    public static Henleggelsesbrev av(Henleggelsesbrevbestilling brevbestilling) {
        return new Henleggelsesbrev(brevbestilling, Aktoersroller.BRUKER);
    }
}
