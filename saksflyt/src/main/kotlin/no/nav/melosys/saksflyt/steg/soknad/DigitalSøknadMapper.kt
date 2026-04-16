package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto

internal fun mapPeriode(periodeDto: PeriodeDto?): Periode =
    periodeDto?.let { Periode(it.fraDato, it.tilDato) } ?: Periode()

internal fun mapSoeknadsland(landkode: LandKode?): Soeknadsland =
    Soeknadsland(landkode?.let { listOf(it.name) } ?: emptyList(), false)

/**
 * Forenklet mapper som kun mapper periode og land fra skjemadata til [Soeknad].
 *
 * Full skjemadata lagres som original_data (JSON) i skjema_sak_mapping,
 * og saksbehandler bruker PDF for å se detaljene.
 */
object DigitalSøknadMapper {

    fun tilSoeknad(dto: UtsendtArbeidstakerSkjemaM2MDto): Soeknad {
        val (periode, land) = hentPeriodeOgLand(dto)
        return Soeknad().apply {
            this.periode = periode
            this.soeknadsland = land
            //TODO: Sett flere felter - MELOSYS-8016
        }
    }

    fun hentPeriodeOgLand(dto: UtsendtArbeidstakerSkjemaM2MDto): Pair<Periode, Soeknadsland> {
        val data = dto.skjema.data
        return mapPeriode(data.utsendingsperiodeOgLand?.utsendelsePeriode) to
            mapSoeknadsland(data.utsendingsperiodeOgLand?.utsendelseLand)
    }
}
