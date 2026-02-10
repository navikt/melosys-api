package no.nav.melosys.saksflyt.steg.soknad

import mu.KotlinLogging
import no.nav.melosys.integrasjon.melosysskjema.MelosysSkjemaApiClient
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey.SØKNAD_MOTTATT_MELDING
import no.nav.melosys.saksflytapi.domain.ProsessDataKey.SØKNADSDATA
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Saga-steg som henter søknadsdata fra melosys-skjema-api og lagrer på prosessinstansen.
 *
 * Henter skjemaId fra Kafka-meldingen og bruker MelosysSkjemaApiClient til å hente
 * komplett søknadsdata (UtsendtArbeidstakerM2MSkjemaData) for videre behandling.
 */
@Component
class HentSøknadsdata(
    private val melosysSkjemaApiClient: MelosysSkjemaApiClient
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.HENT_SØKNADSDATA

    override fun utfør(prosessinstans: Prosessinstans) {
        val søknadMottattMelding = prosessinstans.hentData<SkjemaMottattMelding>(SØKNAD_MOTTATT_MELDING)
        val skjemaId = søknadMottattMelding.skjemaId

        log.info { "Henter søknadsdata for skjemaId $skjemaId" }

        val søknadsdata = melosysSkjemaApiClient.hentUtsendtArbeidstakerSkjema(skjemaId)

        prosessinstans.setData(SØKNADSDATA, søknadsdata)

        log.info { "Hentet og lagret søknadsdata for skjemaId $skjemaId" }
    }
}
