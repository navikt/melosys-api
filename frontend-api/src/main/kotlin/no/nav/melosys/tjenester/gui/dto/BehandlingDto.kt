package no.nav.melosys.tjenester.gui.dto

class BehandlingDto {
    @JvmField
    var behandlingID: Long? = null

    @JvmField
    var oppsummering: BehandlingOppsummeringDto? = null

    @JvmField
    var saksopplysninger: SaksopplysningerDto? = null

    var isRedigerbart: Boolean = false
}
