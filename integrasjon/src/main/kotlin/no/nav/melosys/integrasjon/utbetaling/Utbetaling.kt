package no.nav.melosys.integrasjon.utbetaling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Utbetaling(
    var forfallsdato: String?,
    var posteringsdato: String,
    var utbetalingNettobeloep: Double,
    var utbetalingsdato: String?,
    var utbetalingsmelding: String?,
    var utbetalingsmetode: String,
    var utbetalingsstatus: String,
    var utbetaltTil: UtbetaltTil,
    var utbetaltTilKonto: UtbetaltTilKonto?,
    var ytelseListe: List<Ytelse>
)

data class UtbetaltTil(
    var aktoertype: String,
    var ident: String,
    var navn: String
)

data class UtbetaltTilKonto(
    var kontonummer: String,
    var kontotype: String
)

data class Ytelse(
    var bilagsnummer: String?,
    var refundertForOrg: RefundertForOrg?,
    var rettighetshaver: Rettighetshaver,
    var skattListe: List<Skatt>,
    var skattsum: Double,
    var trekkListe: List<TrekkListe>?,
    var trekksum: Int,
    var ytelseNettobeloep: Int,
    var ytelseskomponentListe: List<YtelseskomponentListe>?,
    var ytelseskomponentersum: Double,
    var ytelsesperiode: Ytelsesperiode,
    var ytelsestype: String?
)

data class RefundertForOrg(
    var aktoertype: String,
    var ident: String,
    var navn: String
)

data class Rettighetshaver(
    var aktoertype: String,
    var ident: String,
    var navn: String
)

data class Skatt(
    var skattebeloep: Double
)

data class TrekkListe(
    var kreditor: String,
    var trekkbeloep: Int,
    var trekktype: String
)

data class YtelseskomponentListe(
    var satsantall: Double,
    var satsbeloep: Int,
    var satstype: String,
    var ytelseskomponentbeloep: Int,
    var ytelseskomponenttype: String
)

data class Ytelsesperiode(
    var fom: String,
    var tom: String
)
