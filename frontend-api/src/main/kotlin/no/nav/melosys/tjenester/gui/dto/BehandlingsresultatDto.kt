package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse
import no.nav.melosys.domain.Kontrollresultat
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import java.util.stream.Collectors

class BehandlingsresultatDto private constructor(
    behandlingsresultatTypeKode: Behandlingsresultattyper,
    val begrunnelseFritekst: String?,
    val innledningFritekst: String?,
    val trygdeavgiftFritekst: String?,
    val nyVurderingBakgrunn: String?,
    val utfallRegistreringUnntak: String?,
    val utfallUtpeking: String?,
    val vedtakstype: String?,
    val kontrollresultatBegrunnelseKoder: List<String>?,
    val fakturaserieReferanse: String?
) {
    val behandlingsresultatTypeKode: String = behandlingsresultatTypeKode.kode
    @JvmField
    val begrunnelseKoder: MutableList<String> = ArrayList()

    companion object {
        @JvmStatic
        fun av(resultat: Behandlingsresultat): BehandlingsresultatDto {
            val dto = BehandlingsresultatDto(
                resultat.type,
                resultat.begrunnelseFritekst,
                resultat.innledningFritekst,
                resultat.trygdeavgiftFritekst,
                resultat.nyVurderingBakgrunn,
                if (resultat.utfallRegistreringUnntak != null) resultat.utfallRegistreringUnntak.kode else null,
                if (resultat.utfallUtpeking != null) resultat.utfallUtpeking.kode else null,
                if (resultat.vedtakMetadata != null && resultat.vedtakMetadata.vedtakstype != null
                ) resultat.vedtakMetadata.vedtakstype.kode else null,
                resultat.kontrollresultater.stream().map { obj: Kontrollresultat -> obj.begrunnelse }
                    .map { obj: Kontroll_begrunnelser -> obj.kode }.collect(Collectors.toList()),
                resultat.fakturaserieReferanse
            )

            resultat.behandlingsresultatBegrunnelser.stream()
                .map { obj: BehandlingsresultatBegrunnelse -> obj.kode }
                .forEach { e: String -> dto.begrunnelseKoder.add(e) }

            if (resultat.nyVurderingBakgrunn != null) {
                dto.begrunnelseKoder.add(resultat.nyVurderingBakgrunn)
            }
            return dto
        }
    }
}
