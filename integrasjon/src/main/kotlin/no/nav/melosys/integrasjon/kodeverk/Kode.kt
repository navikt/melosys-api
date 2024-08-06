package no.nav.melosys.integrasjon.kodeverk

import java.time.LocalDate

data class Kode(val kode: String, val navn: String, val gyldigFom: LocalDate, val gyldigTom: LocalDate)
