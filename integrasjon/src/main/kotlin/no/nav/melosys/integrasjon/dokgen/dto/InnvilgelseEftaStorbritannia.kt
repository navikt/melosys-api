package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.InnvilgelseEftaStorbritanniaBrevbestilling
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import java.time.LocalDate

class InnvilgelseEftaStorbritannia(
    brevbestilling: InnvilgelseEftaStorbritanniaBrevbestilling,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val datoMottatt: LocalDate?,
    val navnVirksomhet: String?,
    val behandlingstype: Behandlingstyper,
    val nyVurderingBakgrunn: String?,
    val innvilgelseFritekst: String?,
    val lovvalgsbestemmelse: String?,
    val erUnntakTuristskip: Boolean?,
    val erNorskSkip: Boolean?,
    val lovvalgsperiode: Periode?,
    val innledningFritekst: String?,
    val tilleggsbestemmelse: String?,
    val erArtikkel13_3_a_eller_13_4: Boolean?,
    val erArtikkel14_1_eller_14_2: Boolean?,
    val erArtikkel16_1_eller_16_3: Boolean?,
    val erArtikkel18_1: Boolean?,
    val bosted: String?,
    val anmodningsperiodeSvarType: String?,
    val begrunnelseFritekst: String?,
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {
    constructor(
        brevbestilling: InnvilgelseEftaStorbritanniaBrevbestilling,
        navnVirksomhet: String?,
        behandlingstype: Behandlingstyper,
        nyVurderingBakgrunn: String?,
        innvilgelseFritekst: String?,
        lovvalgsbestemmelse: String?,
        erUnntakTuristskip: Boolean?,
        erNorskSkip: Boolean?,
        lovvalgsperiode: Periode?,
        innledningFritekst: String?,
        tilleggsbestemmelse: String?,
        erArtikkel13_3_a_eller_13_4: Boolean?,
        erArtikkel14_1_eller_14_2: Boolean?,
        erArtikkel16_1_eller_16_3: Boolean?,
        erArtikkel18_1: Boolean?,
        bosted: String?,
        anmodningsperiodeSvarType: String?,
        begrunnelseFritekst: String?,
    ) : this(
        brevbestilling,
        datoMottatt = instantTilLocalDate(brevbestilling.forsendelseMottatt),
        navnVirksomhet,
        behandlingstype,
        nyVurderingBakgrunn,
        innvilgelseFritekst,
        lovvalgsbestemmelse,
        erUnntakTuristskip,
        erNorskSkip,
        lovvalgsperiode,
        innledningFritekst,
        tilleggsbestemmelse,
        erArtikkel13_3_a_eller_13_4,
        erArtikkel14_1_eller_14_2,
        erArtikkel16_1_eller_16_3,
        erArtikkel18_1,
        bosted,
        anmodningsperiodeSvarType,
        begrunnelseFritekst,
    )
}
