package no.nav.melosys.domain.eessi.melding

data class UtpekingAvvis(
    var begrunnelse: String? = null,
    var etterspørInformasjon: Boolean? = null,
    var nyttLovvalgsland: String? = null,
    var fritekst: String? = null
)
