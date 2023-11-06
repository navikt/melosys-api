package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.melosys.domain.brev.VarselbrevManglendeInnbetalingBrevbestilling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class VarselbrevManglendeInnbetaling extends DokgenDto {

    private final String fullmektigForBetaling;

    @JsonFormat(shape = STRING)
    private final LocalDate betalingsfrist;

    private final String fakturanummer;

    private final String betalingsstatus;

    public VarselbrevManglendeInnbetaling(VarselbrevManglendeInnbetalingBrevbestilling brevbestilling) {
        super(brevbestilling, Mottakerroller.BRUKER);

        this.fullmektigForBetaling = brevbestilling.getFullmektigForBetaling();
        this.betalingsfrist = brevbestilling.getBetalingsfrist();
        this.fakturanummer = brevbestilling.getFakturanummer();
        this.betalingsstatus = brevbestilling.getBetalingsstatus().name();

    }
}
