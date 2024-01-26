package no.nav.melosys.tjenester.gui.dto

@JvmRecord
data class IkkeGodkjennUnntaksperiodeDto(@JvmField val ikkeGodkjentBegrunnelseKoder: Set<String>, @JvmField val begrunnelseFritekst: String)

