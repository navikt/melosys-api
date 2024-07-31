package no.nav.melosys.integrasjon.kodeverk

import java.time.LocalDate

class Kode(@JvmField val kode: String, @JvmField val navn: String, val gyldigFom: LocalDate, val gyldigTom: LocalDate)
