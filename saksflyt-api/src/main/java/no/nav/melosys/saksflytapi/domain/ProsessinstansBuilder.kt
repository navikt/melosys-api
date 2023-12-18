package no.nav.melosys.saksflytapi.domain

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.arkiv.DokumentReferanse
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import org.apache.commons.lang3.StringUtils
import org.springframework.util.CollectionUtils

class ProsessinstansBuilder {
    private var type: ProsessType? = null
    private var behandling: Behandling? = null
    private var begrunnelseFritekst: String? = null
    private var vedleggTilSed: Set<DokumentReferanse?>? = null
    private var ytterligereInformasjonTilSed: String? = null
    private var eessiMottakere: Set<String?>? = null
    private var eessiMelding: MelosysEessiMelding? = null

    fun medType(type: ProsessType?): ProsessinstansBuilder {
        this.type = type
        return this
    }

    fun medBehandling(behandling: Behandling?): ProsessinstansBuilder {
        this.behandling = behandling
        return this
    }

    fun medBegrunnelseFritekst(fritekst: String?): ProsessinstansBuilder {
        this.begrunnelseFritekst = fritekst
        return this
    }

    fun medVedleggTilSed(vedlegg: Set<DokumentReferanse?>?): ProsessinstansBuilder {
        this.vedleggTilSed = vedlegg
        return this
    }

    fun medYtterligereinformasjonSed(ytterligereInformasjonSed: String?): ProsessinstansBuilder {
        this.ytterligereInformasjonTilSed = ytterligereInformasjonSed
        return this
    }

    fun medEessiMottakere(eessiMottakere: Set<String?>?): ProsessinstansBuilder {
        this.eessiMottakere = eessiMottakere
        return this
    }

    fun medEessiMelding(eessiMelding: MelosysEessiMelding?): ProsessinstansBuilder {
        this.eessiMelding = eessiMelding
        return this
    }

    fun build(): Prosessinstans {
        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = behandling
        prosessinstans.type = type

        if (StringUtils.isNotEmpty(begrunnelseFritekst)) {
            prosessinstans.setData(ProsessDataKey.BEGRUNNELSE_FRITEKST, begrunnelseFritekst)
        }
        if (!CollectionUtils.isEmpty(vedleggTilSed)) {
            prosessinstans.setData(ProsessDataKey.VEDLEGG_SED, vedleggTilSed)
        }
        if (StringUtils.isNotEmpty(ytterligereInformasjonTilSed)) {
            prosessinstans.setData(ProsessDataKey.YTTERLIGERE_INFO_SED, ytterligereInformasjonTilSed)
        }
        if (!CollectionUtils.isEmpty(eessiMottakere)) {
            prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, eessiMottakere)
        }
        if (eessiMelding != null) {
            prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding)
            prosessinstans.låsReferanse = eessiMelding!!.lagUnikIdentifikator()
        }
        return prosessinstans
    }
}
