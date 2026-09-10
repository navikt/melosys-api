package no.nav.melosys.tjenester.gui.dto


data class BehandlingDto(
    val behandlingID: Long,
    val oppsummering: BehandlingOppsummeringDto,
    val saksopplysninger: SaksopplysningerDto,
    val redigerbart: Boolean = false,
    /** NAV-ident til saksbehandleren som har den åpne behandlingsoppgaven, eller null hvis oppgaven er utildelt. */
    val tilordnetIdent: String? = null,
    /** Fullt navn på [tilordnetIdent], eller identen selv hvis navneoppslaget ikke ga treff. */
    val tilordnetNavn: String? = null,
)

