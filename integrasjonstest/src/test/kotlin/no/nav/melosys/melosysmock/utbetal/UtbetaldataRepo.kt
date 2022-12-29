package no.nav.melosys.melosysmock.utbetal

import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingRequest
import no.nav.melosys.integrasjon.utbetaling.Utbetaling
import no.nav.melosys.integrasjon.utbetaling.Ytelse
import no.nav.melosys.integrasjon.utbetaling.Ytelsesperiode
import java.time.LocalDate

typealias UtbetaldataRepository = MutableList<Utbetaling>

object UtbetaldataRepo {
    val repo: UtbetaldataRepository = mutableListOf()

    init {
        leggTilUtbetaling(
            Utbetaling(
                ytelseListe = mutableListOf(
                    Ytelse(
                        ytelsesperiode = Ytelsesperiode(
                            LocalDate.now().minusMonths(10), LocalDate.now().minusMonths(9)
                        ),
                        ytelsestype = "BARNETRYGD"
                    ), Ytelse(
                        ytelsesperiode = Ytelsesperiode(
                            LocalDate.now().minusMonths(8), LocalDate.now().minusMonths(7)
                        ),
                        ytelsestype = "BARNETRYGD"
                    ),
                    Ytelse(
                        ytelsesperiode = Ytelsesperiode(
                            LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1)
                        ),
                        ytelsestype = "IKKE_BARNETRYGD"
                    )
                )
            )
        )
        Utbetaling(
            ytelseListe = mutableListOf(
                Ytelse(
                    ytelsesperiode = Ytelsesperiode(
                        LocalDate.now().minusMonths(20), LocalDate.now().minusMonths(10)
                    ),
                    ytelsestype = "IKKE_BARNETRYGD"
                )
            )
        )
        Utbetaling(
            ytelseListe = mutableListOf(
                Ytelse(
                    ytelsesperiode = Ytelsesperiode(
                        LocalDate.now().minusMonths(50), LocalDate.now().minusMonths(40)
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
