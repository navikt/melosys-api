package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto

/**
 * Forenklet mapper som kun mapper periode og land fra skjemadata til [Soeknad].
 *
 * Full skjemadata lagres som original_data (JSON) i skjema_sak_mapping,
 * og saksbehandler bruker PDF for å se detaljene.
 */
object ForenkletSøknadMapper {

    fun tilSoeknad(dto: UtsendtArbeidstakerSkjemaM2MDto): Soeknad {
        val (periode, land) = hentPeriodeOgLand(dto)
        return Soeknad().apply {
            this.periode = periode
            this.soeknadsland = land
        }
    }

    fun hentPeriodeOgLand(dto: UtsendtArbeidstakerSkjemaM2MDto): Pair<Periode, Soeknadsland> {
        return when (val data = dto.skjema.data) {
            is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto ->
                mapPeriode(data.utsendingsperiodeOgLand?.utsendelsePeriode) to
                    mapSoeknadsland(data.utsendingsperiodeOgLand?.utsendelseLand)

            is UtsendtArbeidstakerArbeidstakersSkjemaDataDto ->
                mapPeriode(data.utsendingsperiodeOgLand?.utsendelsePeriode) to
                    mapSoeknadsland(data.utsendingsperiodeOgLand?.utsendelseLand)

            is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto ->
                mapPeriode(data.utsendingsperiodeOgLand?.utsendelsePeriode) to
                    mapSoeknadsland(data.utsendingsperiodeOgLand?.utsendelseLand)
        }
    }

    private fun mapPeriode(periodeDto: PeriodeDto?): Periode =
        periodeDto?.let { Periode(it.fraDato, it.tilDato) } ?: Periode()

    private fun mapSoeknadsland(landkode: LandKode?): Soeknadsland =
        Soeknadsland(landkode?.let { listOf(it.name) } ?: emptyList(), false)
}
