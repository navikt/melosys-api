package no.nav.melosys.saksflyt.steg.soknad

import mu.KotlinLogging
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.springframework.stereotype.Component
import java.time.ZoneId

private val log = KotlinLogging.logger { }

internal val OSLO_ZONE: ZoneId = ZoneId.of("Europe/Oslo")

/**
 * Saga-steg som håndterer mottak av digital søknad på eksisterende sak.
 *
 * Brukes i MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD-flyten. Selve attach-logikken ligger i den
 * delte [DigitalSøknadEksisterendeSakHåndterer], som også gjenbrukes av NY-flyten når den under
 * DB-låsen oppdager at saken likevel finnes.
 */
@Component
class HåndterEksisterendeSakDigitalSøknad(
    private val eksisterendeSakHåndterer: DigitalSøknadEksisterendeSakHåndterer
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.HÅNDTER_EKSISTERENDE_SAK_DIGITAL_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) {
        val søknadsdata = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(ProsessDataKey.DIGITAL_SØKNADSDATA)
        val saksnummer = prosessinstans.hentData(ProsessDataKey.SAKSNUMMER)

        prosessinstans.behandling = eksisterendeSakHåndterer.håndter(saksnummer, søknadsdata)
    }
}
