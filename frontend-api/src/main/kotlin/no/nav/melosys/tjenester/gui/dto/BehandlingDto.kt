package no.nav.melosys.tjenester.gui.dto


data class BehandlingDto(
    val behandlingID: Long,
    val oppsummering: BehandlingOppsummeringDto,
    val saksopplysninger: SaksopplysningerDto,
    val redigerbart: Boolean = false,
    val aarsavregningID: Long?
)

