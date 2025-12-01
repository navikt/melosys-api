package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.melosys.domain.brev.VarselbrevManglendeInnbetalingBrevbestilling;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Mottakerroller;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class VarselbrevManglendeInnbetaling extends DokgenDto {
    private final String fullmektigForBetaling;
    @JsonFormat(shape = STRING)
    private final LocalDate betalingsfrist;
    private final String fakturanummer;
    private final String betalingsstatus;
    private final String medlemskapstype;
    private final String fullmektigForSoeknad;
    private final Boolean erEøsPensjonist;
    private final Boolean erEøsLovvalg;

    public VarselbrevManglendeInnbetaling(VarselbrevManglendeInnbetalingBrevbestilling brevbestilling, Medlemskapstyper medlemskapstype, String fullmektigForSoeknad) {
        super(brevbestilling, Mottakerroller.BRUKER);

        this.fullmektigForBetaling = brevbestilling.getFullmektigForBetaling();
        this.betalingsfrist = brevbestilling.getBetalingsfrist();
        this.fakturanummer = brevbestilling.getFakturanummer();
        this.betalingsstatus = brevbestilling.getBetalingsstatus().name();
        this.medlemskapstype = medlemskapstype.name();
        this.fullmektigForSoeknad = fullmektigForSoeknad;
        this.erEøsPensjonist = brevbestilling.getErEøsPensjonist();
        this.erEøsLovvalg = brevbestilling.getErEøsLovvalg();
    }

    public String getFullmektigForBetaling() {
        return fullmektigForBetaling;
    }

    public String getFakturanummer() {
        return fakturanummer;
    }

    public String getBetalingsstatus() {
        return betalingsstatus;
    }

    public LocalDate getBetalingsfrist() {
        return betalingsfrist;
    }

    public String getMedlemskapstype() {
        return medlemskapstype;
    }

    public String getFullmektigForSoeknad() {
        return fullmektigForSoeknad;
    }

    public Boolean isErEøsPensjonist() {
        return erEøsPensjonist;
    }

    public Boolean isErEøsLovvalg() {
        return erEøsPensjonist;
    }
}
