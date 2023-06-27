package no.nav.melosys.tjenester.gui.dto.kontroller

import no.nav.melosys.exception.validering.KontrollfeilDto

data class KontrollerFerdigbehandlingDto(var kontrollfeilList: List<KontrollfeilDto>)
