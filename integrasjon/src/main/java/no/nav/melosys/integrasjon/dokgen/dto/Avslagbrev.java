package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.melosys.domain.brev.AvslagBrevbestilling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class Avslagbrev extends DokgenDto {

    private final String fritekst;

    @JsonFormat(shape = STRING)
    private final LocalDate datoMottatt;

    private final String sakstype;

    private final String behandlingstype;

    private Avslagbrev(AvslagBrevbestilling brevbestilling,
                       Mottakerroller mottakerType) {
        super(brevbestilling, mottakerType);

        this.fritekst = brevbestilling.getAvslagFritekst();
        this.datoMottatt = instantTilLocalDate(brevbestilling.getForsendelseMottatt());
        this.sakstype = brevbestilling.getBehandling().getFagsak().getType().getKode();
        this.behandlingstype = brevbestilling.getBehandling().getType().getKode();
    }

    public String getFritekst() {
        return fritekst;
    }

    public static Avslagbrev av(AvslagBrevbestilling brevbestilling) {
        return new Avslagbrev(brevbestilling, Mottakerroller.BRUKER);
    }

    public String getSakstype() {
        return sakstype;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public LocalDate getDatoMottatt() {
        return datoMottatt;
    }

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }
}
