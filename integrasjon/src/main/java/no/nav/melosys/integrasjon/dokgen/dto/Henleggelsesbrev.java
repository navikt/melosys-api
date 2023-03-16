package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.melosys.domain.brev.HenleggelseBrevbestilling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class Henleggelsesbrev extends DokgenDto {
    private final String fritekst;
    private final String begrunnelseKode;
    private final String behandlingstype;

    @JsonFormat(shape = STRING)
    private final LocalDate datoMottatt;

    private Henleggelsesbrev(HenleggelseBrevbestilling brevbestilling, Mottakerroller mottakerType) {
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

    public static Henleggelsesbrev av(HenleggelseBrevbestilling brevbestilling) {
        return new Henleggelsesbrev(brevbestilling, Mottakerroller.BRUKER);
    }

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }
}
