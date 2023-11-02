package no.nav.melosys.domain.brev;

import no.nav.melosys.domain.ftrl.Betalingsstatus;

import java.time.LocalDate;

public class VarselbrevManglendeInnbetalingBrevbestilling extends DokgenBrevbestilling {
    private LocalDate fakturanummer;
    private Betalingsstatus betalingsstatus;

    public VarselbrevManglendeInnbetalingBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private VarselbrevManglendeInnbetalingBrevbestilling(VarselbrevManglendeInnbetalingBrevbestilling.Builder builder) {
        super(builder);
    }

    public LocalDate getFakturanummer() {
        return fakturanummer;
    }

    public Betalingsstatus getBetalingsstatus() {
        return betalingsstatus;
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private LocalDate fakturanummer;
        private Betalingsstatus betalingsstatus;

        public Builder() {
        }

        public Builder(VarselbrevManglendeInnbetalingBrevbestilling varselbrevManglendeInnbetalingBrevbestilling) {
            super(varselbrevManglendeInnbetalingBrevbestilling);
            this.betalingsstatus = varselbrevManglendeInnbetalingBrevbestilling.getBetalingsstatus();
            this.fakturanummer = varselbrevManglendeInnbetalingBrevbestilling.getFakturanummer();
        }

        public Builder medfakturanummer(LocalDate fakturanummer) {
            this.fakturanummer = fakturanummer;
            return this;
        }

        public Builder medBetalingsstatus(Betalingsstatus betalingsstatus) {
            this.betalingsstatus = betalingsstatus;
            return this;
        }

        @Override
        public VarselbrevManglendeInnbetalingBrevbestilling build() {
            return new VarselbrevManglendeInnbetalingBrevbestilling(this);
        }
    }
}
