package no.nav.melosys.melosysmock.utbetal

import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingRequest
import no.nav.melosys.integrasjon.utbetaling.Utbetaling
import no.nav.melosys.integrasjon.utbetaling.Ytelse

typealias UtbetaldataRepository = MutableList<Utbetaling>

object UtbetaldataRepo {
    val repo: UtbetaldataRepository = mutableListOf()

    init {
        leggTilUtbetaling(
            Utbetaling(
                ytelseListe = mutableListOf(
                    Ytelse(
                        ytelsestype = "BARNETRYGD"
                    ), Ytelse(
                        ytelsestype = "BARNETRYGD"
                    ),
                    Ytelse(
                        ytelsestype = "IKKE_BARNETRYGD"
                    )
                )
            )
        )
        Utbetaling(
            ytelseListe = mutableListOf(
                Ytelse(
                    ytelsestype = "IKKE_BARNETRYGD"
                )
            )
        )
        Utbetaling(
            ytelseListe = mutableListOf(
                Ytelse(
                    ytelsestype = "BARNETRYGD"
                )
            )
        )
    }

    fun leggTilUtbetaling(utbetaling: Utbetaling) {
        repo.add(utbetaling)
    }

    fun finnUtbetalinger(utbetalingRequest: UtbetalingRequest): List<Utbetaling> = repo
}
