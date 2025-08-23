package no.nav.melosys.domain.brev.trygdeavtale

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import java.time.LocalDate

@JvmRecord
data class Utsendelse(
    val artikkel: LovvalgBestemmelse?,
    val oppholdsadresse: MutableList<String?>?,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?
)
