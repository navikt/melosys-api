package no.nav.melosys.integrasjon.hendelser

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import java.time.LocalDate

data class MelosysHendelse(
    val melding: HendelseMelding
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = HendelseMelding::class, name = "HendelseMelding"),
    JsonSubTypes.Type(value = VedtakHendelseMelding::class, name = "VedtakHendelseMelding"),
    JsonSubTypes.Type(value = PensjonsopptjeningHendelse::class, name = "PensjonsopptjeningHendelse")
)
@JsonIgnoreProperties(ignoreUnknown = true)
open class HendelseMelding

data class VedtakHendelseMelding(
    val folkeregisterIdent: String,
    val sakstype: Sakstyper,
    val sakstema: Sakstemaer = Sakstemaer.TRYGDEAVGIFT,
    val behandligsresultatType: Behandlingsresultattyper,
    val vedtakstype: Vedtakstyper? = null,
    val medlemskapsperioder: List<Periode>,
    val lovvalgsperioder: List<Periode>
) : HendelseMelding()

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
    val innvilgelsesResultat : InnvilgelsesResultat
)
