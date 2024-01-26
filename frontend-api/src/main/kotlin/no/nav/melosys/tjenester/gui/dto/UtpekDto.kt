package no.nav.melosys.tjenester.gui.dto

@JvmRecord
data class UtpekDto(@JvmField val mottakerinstitusjoner: Set<String>, @JvmField val fritekstSed: String, @JvmField val fritekstBrev: String)
