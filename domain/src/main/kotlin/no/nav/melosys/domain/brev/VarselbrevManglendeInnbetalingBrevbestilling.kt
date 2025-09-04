package no.nav.melosys.domain.brev

import java.time.LocalDate
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus

class VarselbrevManglendeInnbetalingBrevbestilling : DokgenBrevbestilling {
    var fakturanummer: String? = null
    var betalingsstatus: Betalingsstatus? = null
    var fullmektigForBetaling: String? = null
    var betalingsfrist: LocalDate? = null
    var erEøsPensjonist: Boolean? = null


    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.fakturanummer = builder.fakturanummer
        this.betalingsstatus = builder.betalingsstatus
        this.fullmektigForBetaling = builder.fullmektigForBetaling
        this.betalingsfrist = builder.betalingsfrist
        this.erEøsPensjonist = builder.erEøsPensjonist
    }

    override fun toBuilder(): Builder = Builder(this)

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var fakturanummer: String? = null
        internal var betalingsstatus: Betalingsstatus? = null
        internal var fullmektigForBetaling: String? = null
        internal var betalingsfrist: LocalDate? = null
        internal var erEøsPensjonist: Boolean? = null

        constructor()

        constructor(varselbrevManglendeInnbetalingBrevbestilling: VarselbrevManglendeInnbetalingBrevbestilling) : super(varselbrevManglendeInnbetalingBrevbestilling) {
            this.betalingsstatus = varselbrevManglendeInnbetalingBrevbestilling.betalingsstatus
            this.fakturanummer = varselbrevManglendeInnbetalingBrevbestilling.fakturanummer
            this.fullmektigForBetaling = varselbrevManglendeInnbetalingBrevbestilling.fullmektigForBetaling
            this.betalingsfrist = varselbrevManglendeInnbetalingBrevbestilling.betalingsfrist
            this.erEøsPensjonist = varselbrevManglendeInnbetalingBrevbestilling.erEøsPensjonist
        }

        fun medFakturanummer(fakturanummer: String?) = apply { this.fakturanummer = fakturanummer }

        fun medBetalingsstatus(betalingsstatus: Betalingsstatus?) = apply { this.betalingsstatus = betalingsstatus }

        fun medFullmektigForBetaling(fullmektigForBetaling: String?) = apply { this.fullmektigForBetaling = fullmektigForBetaling }

        fun medErEøsPensjonist(erEøsPensjonist: Boolean) = apply { this.erEøsPensjonist = erEøsPensjonist }

        fun medBetalingsfrist(betalingsfrist: LocalDate?) = apply { this.betalingsfrist = betalingsfrist }

        override fun build(): VarselbrevManglendeInnbetalingBrevbestilling = VarselbrevManglendeInnbetalingBrevbestilling(this)
    }
}
