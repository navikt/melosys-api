package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.melosys.domain.brev.VarselbrevManglendeInnbetalingBrevbestilling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class VarselbrevManglendeInnbetaling extends DokgenDto {

    @JsonFormat(shape = STRING)
    private final LocalDate datoMottatt;

    @JsonFormat(shape = STRING)
    private final LocalDate datoFrist;

    private final String fakturanummer;

    private final String betalingsstatus;

    protected VarselbrevManglendeInnbetaling(VarselbrevManglendeInnbetalingBrevbestilling brevbestilling, LocalDate datoFrist) {
        super(brevbestilling, Mottakerroller.FULLMEKTIG); // TODO i MELOSYS-5738.

        this.datoMottatt = instantTilLocalDate(brevbestilling.getForsendelseMottatt());
        this.datoFrist = datoFrist;
        this.fakturanummer = brevbestilling.getFakturanummer();
        this.betalingsstatus = brevbestilling.getBetalingsstatus().name();

    }

    public static VarselbrevManglendeInnbetaling av(VarselbrevManglendeInnbetalingBrevbestilling brevbestilling, LocalDate datoFrist) {
        return new VarselbrevManglendeInnbetaling(brevbestilling, datoFrist);
    }
}
