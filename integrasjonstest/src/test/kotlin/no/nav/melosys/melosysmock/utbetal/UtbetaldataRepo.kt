package no.nav.melosys.melosysmock.utbetal

import no.nav.melosys.integrasjon.utbetaling.*
import java.time.LocalDate

typealias UtbetaldataRepository = MutableList<Utbetaling>

object UtbetaldataRepo {
    val repo: UtbetaldataRepository = mutableListOf()

    init {
        leggTilUtbetaling(
            Utbetaling(
                ytelseListe = mutableListOf(
                    Ytelse(
                        ytelsesperiode = Periode(
                            LocalDate.now().minusMonths(10).toString(), LocalDate.now().minusMonths(9).toString()
                        ),
                        ytelsestype = "BARNETRYGD"
                    ), Ytelse(
                        ytelsesperiode = Periode(
                            LocalDate.now().minusMonths(8).toString(), LocalDate.now().minusMonths(7).toString()
                        ),
                        ytelsestype = "BARNETRYGD"
                    ),
                    Ytelse(
                        ytelsesperiode = Periode(
                            LocalDate.now().minusMonths(2).toString(), LocalDate.now().minusMonths(1).toString()
                        ),
                        ytelsestype = "IKKE_BARNETRYGD"
                    )
                )
            )
        )
        Utbetaling(
            ytelseListe = mutableListOf(
                Ytelse(
                    ytelsesperiode = Periode(
                        LocalDate.now().minusMonths(20).toString(), LocalDate.now().minusMonths(10).toString()
                    ),
                    ytelsestype = "IKKE_BARNETRYGD"
                )
            )
        )
        Utbetaling(
            ytelseListe = mutableListOf(
                Ytelse(
                    ytelsesperiode = Periode(
                        LocalDate.now().minusMonths(50).toString(), LocalDate.now().minusMonths(40).toString()
                    ),
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
