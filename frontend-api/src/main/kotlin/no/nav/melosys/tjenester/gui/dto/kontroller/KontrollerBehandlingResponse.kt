package no.nav.melosys.tjenester.gui.dto.kontroller

import no.nav.melosys.exception.validering.KontrollfeilDto

data class KontrollerBehandlingResponse(val kontrollfeilList: List<KontrollfeilDto>)
