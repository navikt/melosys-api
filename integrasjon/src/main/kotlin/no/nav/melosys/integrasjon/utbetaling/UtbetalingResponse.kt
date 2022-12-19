package no.nav.melosys.integrasjon.utbetaldata.utbetaling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UtbetalingResponse(
    var utbetalingListe: List<Utbetaling>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Utbetaling(
    val forfallsdato: String?,
    val posteringsdato: String,
    val utbetalingNettobeloep: Double,
    val utbetalingsdato: String?,
    val utbetalingsmelding: String?,
    val utbetalingsmetode: String,
    val utbetalingsstatus: String,
    val utbetaltTil: UtbetaltTil,
    val utbetaltTilKonto: UtbetaltTilKonto?,
    val ytelseListe: List<Ytelse>
)

data class UtbetaltTil(
    val aktoertype: String,
    val ident: String,
    val navn: String
)

data class UtbetaltTilKonto(
    val kontonummer: String,
    val kontotype: String
)

data class Ytelse(
    val bilagsnummer: String?,
    val refundertForOrg: RefundertForOrg?,
    val rettighetshaver: Rettighetshaver,
    val skattListe: List<Skatt>,
    val skattsum: Double,
    val trekkListe: List<TrekkListe>?,
    val trekksum: Int,
    val ytelseNettobeloep: Int,
    val ytelseskomponentListe: List<YtelseskomponentListe>?,
    val ytelseskomponentersum: Double,
    val ytelsesperiode: Ytelsesperiode,
    val ytelsestype: String?
)

data class RefundertForOrg(
    val aktoertype: String,
    val ident: String,
    val navn: String
)

data class Rettighetshaver(
    val aktoertype: String,
    val ident: String,
    val navn: String
)

data class Skatt(
    val skattebeloep: Double
)

data class TrekkListe(
    val kreditor: String,
    val trekkbeloep: Int,
    val trekktype: String
)

data class YtelseskomponentListe(
    val satsantall: Double,
    val satsbeloep: Int,
    val satstype: String,
    val ytelseskomponentbeloep: Int,
    val ytelseskomponenttype: String
)

data class Ytelsesperiode(
    val fom: String,
    val tom: String
)
