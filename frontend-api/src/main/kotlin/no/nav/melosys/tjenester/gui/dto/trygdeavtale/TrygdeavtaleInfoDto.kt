package no.nav.melosys.tjenester.gui.dto.trygdeavtale

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.domain.kodeverk.Trygdeavtale_myndighetsland
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import java.time.LocalDate
import java.util.function.Function
import java.util.function.Predicate

@JvmRecord
data class TrygdeavtaleInfoDto(
    @JvmField val aktoerId: String?,
    @JvmField val behandlingstema: String?,
    @JvmField val behandlingstype: String?,
    @JvmField val redigerbart: Boolean,
    @JvmField val periodeFom: LocalDate,
    @JvmField val periodeTom: LocalDate,
    @JvmField @field:JsonInclude(JsonInclude.Include.NON_NULL) @param:JsonInclude(JsonInclude.Include.NON_NULL) val soeknadsland: Trygdeavtale_myndighetsland?,
    val virksomheter: List<VirksomhetDto?>, val barn: List<FamilieDto?>,
    val ektefelleSamboer: FamilieDto?,
    @JvmField val innledningFritekst: String?,
    @JvmField val begrunnelseFritekst: String?,
    @JvmField val nyVurderingBakgrunn: String?
) {
    constructor(
        aktoerId: String?,
        behandlingstema: String?,
        behandlingstype: String?,
        redigerbart: Boolean,
        periode: Periode,
        soeknadsland: Trygdeavtale_myndighetsland?,
        virksomheter: Map<String?, String?>,
        familie: List<MedfolgendeFamilie>,
        innledingFritekst: String?,
        begrunnelseFritekst: String?,
        nyVurderingBakgrunn: String?
    ) : this(
        aktoerId,
        behandlingstema,
        behandlingstype,
        redigerbart,
        periode.fom,
        periode.tom,
        soeknadsland,
        mapVirksomheter(virksomheter),
        filtrerOgMapFamilie(familie) { obj: MedfolgendeFamilie -> obj.erBarn() },
        filtrerOgMapFamilie(familie) { obj: MedfolgendeFamilie -> obj.erEktefelleSamboer() }
            .stream().findFirst().orElse(null),
        innledingFritekst,
        begrunnelseFritekst,
        nyVurderingBakgrunn
    )

    companion object {
        fun mapVirksomheter(virksomheter: Map<String?, String?>): List<VirksomhetDto?> {
            return virksomheter.entries.stream()
                .map { a -> VirksomhetDto(a.key, a.value) }
                .toList()
        }

        fun filtrerOgMapFamilie(
            familie: List<MedfolgendeFamilie>,
            filterfunksjon: Predicate<MedfolgendeFamilie>?
        ): List<FamilieDto?> {
            return familie.stream()
                .filter(filterfunksjon)
                .map { familiemedlem: MedfolgendeFamilie ->
                    FamilieDto(
                        familiemedlem.uuid,
                        familiemedlem.fnr,
                        familiemedlem.navn
                    )
                }
                .toList()
        }
    }
}
