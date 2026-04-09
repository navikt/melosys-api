package no.nav.melosys.saksflyt.steg.soknad

import mu.KotlinLogging
import no.nav.melosys.integrasjon.melosysskjema.MelosysSkjemaApiClient
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey.SØKNADSDATA
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Sender saksnummer tilbake til melosys-skjema-api via M2M-kall.
 * Brukes som siste steg i begge SAGA-flytene.
 */
@Component
class SendSaksnummerTilSkjema(
    private val melosysSkjemaApiClient: MelosysSkjemaApiClient
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.SEND_SAKSNUMMER_TIL_SKJEMA

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = requireNotNull(prosessinstans.behandling) {
            "Behandling må være satt for å sende saksnummer"
        }
        val saksnummer = behandling.fagsak.saksnummer
        val søknadsdata = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(SØKNADSDATA)
        val skjemaId = søknadsdata.skjema.id

        log.info { "Sender saksnummer $saksnummer til melosys-skjema-api for skjema $skjemaId" }
        melosysSkjemaApiClient.registrerSaksnummer(skjemaId, saksnummer)
        log.info { "Saksnummer $saksnummer sendt for skjema $skjemaId" }
    }
}
