package no.nav.melosys.integrasjon.utbetaling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class Utbetaling(
    var forfallsdato: String? = null,
    var posteringsdato: String? = null,
    var utbetalingNettobeloep: Double? = null,
    var utbetalingsdato: String? = null,
    var utbetalingsmelding: String? = null,
    var utbetalingsmetode: String? = null,
    var utbetalingsstatus: String? = null,
    var utbetaltTil: UtbetaltTil? = null,
    var utbetaltTilKonto: UtbetaltTilKonto? = null,
    var ytelseListe: MutableList<Ytelse> = mutableListOf()
)

data class UtbetaltTil(
    var aktoertype: String? = null,
    var ident: String? = null,
    var navn: String? = null
)

data class UtbetaltTilKonto(
    var kontonummer: String? = null,
    var kontotype: String? = null
)

data class Ytelse(
    var bilagsnummer: String? = null,
    var refundertForOrg: RefundertForOrg? = null,
    var rettighetshaver: Rettighetshaver? = null,
    var skattListe: List<Skatt>? = null,
    var skattsum: Double? = null,
    var trekkListe: List<TrekkListe>? = null,
    var trekksum: Int? = null,
    var ytelseNettobeloep: Int? = null,
    var ytelseskomponentListe: List<YtelseskomponentListe>? = null,
    var ytelseskomponentersum: Double? = null,
    var ytelsesperiode: Ytelsesperiode? = null,
    var ytelsestype: String = ""
)

data class RefundertForOrg(
    var aktoertype: String? = null,
    var ident: String? = null,
    var navn: String? = null
)

data class Rettighetshaver(
    var aktoertype: String? = null,
    var ident: String? = null,
    var navn: String? = null
)

data class Skatt(
    var skattebeloep: Double? = null
)

data class TrekkListe(
    var kreditor: String? = null,
    var trekkbeloep: Int? = null,
    var trekktype: String? = null
)

data class YtelseskomponentListe(
    var satsantall: Double? = null,
    var satsbeloep: Int? = null,
    var satstype: String? = null,
    var ytelseskomponentbeloep: Int? = null,
    var ytelseskomponenttype: String? = null
)

data class Ytelsesperiode(
    var fom: LocalDate? = null,
    var tom: LocalDate? = null
)
