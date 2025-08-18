package no.nav.melosys.saksflytapi.domain

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.arkiv.DokumentReferanse
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import org.apache.commons.lang3.StringUtils
import org.springframework.util.CollectionUtils
import java.time.LocalDateTime

class ProsessinstansBuilder(
    private var type: ProsessType? = null,
    private var behandling: Behandling? = null,
    private var begrunnelseFritekst: String? = null,
    private var vedleggTilSed: Set<DokumentReferanse?>? = null,
    private var ytterligereInformasjonTilSed: String? = null,
    private var eessiMottakere: Set<String?>? = null,
    private var eessiMelding: MelosysEessiMelding? = null
) {
    fun medType(type: ProsessType?): ProsessinstansBuilder = apply { this.type = type }
    fun medBehandling(behandling: Behandling?): ProsessinstansBuilder = apply { this.behandling = behandling }
    fun medBegrunnelseFritekst(fritekst: String?): ProsessinstansBuilder = apply { this.begrunnelseFritekst = fritekst }
    fun medVedleggTilSed(vedlegg: Set<DokumentReferanse?>?): ProsessinstansBuilder = apply { this.vedleggTilSed = vedlegg }
    fun medYtterligereinformasjonSed(ytterligereInformasjonSed: String?): ProsessinstansBuilder =
        apply { this.ytterligereInformasjonTilSed = ytterligereInformasjonSed }

    fun medEessiMottakere(eessiMottakere: Set<String?>?): ProsessinstansBuilder = apply { this.eessiMottakere = eessiMottakere }
    fun medEessiMelding(eessiMelding: MelosysEessiMelding?): ProsessinstansBuilder = apply { this.eessiMelding = eessiMelding }

    fun build(): Prosessinstans = Prosessinstans(
        type = type ?: error("Prosessinstans må ha en type"),
        status = ProsessStatus.KLAR,
        behandling = behandling,
        registrertDato = LocalDateTime.now(),
        endretDato = LocalDateTime.now(),
    ).apply {
        behandling = this@ProsessinstansBuilder.behandling
        type = this@ProsessinstansBuilder.type!!

        if (StringUtils.isNotEmpty(begrunnelseFritekst))  {
            setData(ProsessDataKey.BEGRUNNELSE_FRITEKST, begrunnelseFritekst)
        }
        if (!CollectionUtils.isEmpty(vedleggTilSed)) {
            setData(ProsessDataKey.VEDLEGG_SED, vedleggTilSed)
        }
        if (StringUtils.isNotEmpty(ytterligereInformasjonTilSed)) {
            setData(ProsessDataKey.YTTERLIGERE_INFO_SED, ytterligereInformasjonTilSed)
        }
        if (!CollectionUtils.isEmpty(eessiMottakere)) {
            setData(ProsessDataKey.EESSI_MOTTAKERE, eessiMottakere)
        }
        eessiMelding?.let {
            setData(ProsessDataKey.EESSI_MELDING, it)
            låsReferanse = it.lagUnikIdentifikator()
        }
    }
}
