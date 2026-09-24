package no.nav.melosys.tjenester.gui.dto


data class BehandlingDto(
    val behandlingID: Long,
    val oppsummering: BehandlingOppsummeringDto,
    val saksopplysninger: SaksopplysningerDto,
    val redigerbart: Boolean = false,
    val tilordnetIdent: String? = null,
    val tilordnetNavn: String? = null,
    val tilordnetMeg: Boolean = false,
    val kanTildeles: Boolean = false,
    val tildelingTilgjengelig: Boolean = false,
)

