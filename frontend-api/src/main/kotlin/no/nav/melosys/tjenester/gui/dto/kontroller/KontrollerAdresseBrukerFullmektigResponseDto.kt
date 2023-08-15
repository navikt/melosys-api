package no.nav.melosys.tjenester.gui.dto.kontroller

import no.nav.melosys.domain.kodeverk.Aktoersroller

data class KontrollerAdresseBrukerFullmektigResponseDto(
    val rolle: Aktoersroller,
    val harRegistrertAdresse: Boolean
)
