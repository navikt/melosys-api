package no.nav.melosys.domain.brev;

import java.time.LocalDate;

import no.nav.melosys.domain.manglendebetaling.Betalingsstatus;

public class VarselbrevManglendeInnbetalingBrevbestilling extends DokgenBrevbestilling {
    private String fakturanummer;
    private Betalingsstatus betalingsstatus;
    private String fullmektigForBetaling;
    private LocalDate betalingsfrist;

    public VarselbrevManglendeInnbetalingBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private VarselbrevManglendeInnbetalingBrevbestilling(VarselbrevManglendeInnbetalingBrevbestilling.Builder builder) {
        super(builder);
    }

    public String getFakturanummer() {
        return fakturanummer;
    }

    public Betalingsstatus getBetalingsstatus() {
        return betalingsstatus;
    }
    public String getFullmektigForBetaling() {
        return fullmektigForBetaling;
    }

    public LocalDate getBetalingsfrist() {
        return betalingsfrist;
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String fakturanummer;
        private Betalingsstatus betalingsstatus;
        private String fullmektigForBetaling;
        private LocalDate betalingsfrist;

        public Builder() {
        }

        public Builder(VarselbrevManglendeInnbetalingBrevbestilling varselbrevManglendeInnbetalingBrevbestilling) {
            super(varselbrevManglendeInnbetalingBrevbestilling);
            this.betalingsstatus = varselbrevManglendeInnbetalingBrevbestilling.getBetalingsstatus();
            this.fakturanummer = varselbrevManglendeInnbetalingBrevbestilling.getFakturanummer();
            this.fullmektigForBetaling = varselbrevManglendeInnbetalingBrevbestilling.getFullmektigForBetaling();
            this.betalingsfrist = varselbrevManglendeInnbetalingBrevbestilling.getBetalingsfrist();
        }

        public Builder medFakturanummer(String fakturanummer) {
            this.fakturanummer = fakturanummer;
            return this;
        }

        public Builder medBetalingsstatus(Betalingsstatus betalingsstatus) {
            this.betalingsstatus = betalingsstatus;
            return this;
        }

        public Builder medFullmektigForBetaling(String fullmektigForBetaling) {
            this.fullmektigForBetaling = fullmektigForBetaling;
            return this;
        }

        public Builder medBetalingsfrist(LocalDate betalingsfrist) {
            this.betalingsfrist = betalingsfrist;
            return this;
        }

        @Override
        public VarselbrevManglendeInnbetalingBrevbestilling build() {
            return new VarselbrevManglendeInnbetalingBrevbestilling(this);
        }
    }
}
