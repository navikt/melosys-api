package no.nav.melosys.saksflyt.steg.soknad

import mu.KotlinLogging
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.sak.SkjemaSakMappingService
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.springframework.stereotype.Component
import java.util.UUID

private val log = KotlinLogging.logger { }

/**
 * Avgjør om mottatt digital søknad skal gi ny sak eller festes på eksisterende sak.
 *
 * Avgjørelsen tas i sagaen, ikke ved mottak, slik at en relatert søknad som ble mottatt rett før
 * er ferdig behandlet og har lagret skjema-sak-mappingen sin. Det sikres av at relaterte søknader
 * deler låsgruppe og settes PÅ_VENT (se ProsessinstansService.opprettProsessinstansDigitalSøknadMottatt).
 *
 * Resultatet lagres i [ProsessDataKey.DIGITAL_SØKNAD_NY_SAK], som senere steg bruker for å avgjøre
 * om arkivsak og oppgave skal opprettes.
 */
@Component
class AvklarSakOgBehandlingDigitalSøknad(
    private val skjemaSakMappingService: SkjemaSakMappingService,
    private val nySakOppretter: DigitalSøknadNySakOppretter,
    private val eksisterendeSakHåndterer: DigitalSøknadEksisterendeSakHåndterer
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.AVKLAR_SAK_OG_BEHANDLING_DIGITAL_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) {
        val skjemaId = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(ProsessDataKey.DIGITAL_SØKNADSDATA).skjema.id
        val relaterteIder = prosessinstans.finnData<List<UUID>>(ProsessDataKey.DIGITAL_SØKNAD_RELATERTE_SKJEMA_IDER).orEmpty()

        val harEksisterendeSak = skjemaSakMappingService.harMappingMedGyldigSaksnummerForSkjemaId(relaterteIder + skjemaId)

        if (harEksisterendeSak) {
            log.info { "Fant eksisterende sak for skjemaId=$skjemaId (låsreferanse ${prosessinstans.låsReferanse})" }
            eksisterendeSakHåndterer.håndter(prosessinstans)
        } else {
            log.info { "Fant ingen eksisterende sak for skjemaId=$skjemaId (låsreferanse ${prosessinstans.låsReferanse}), oppretter ny" }
            nySakOppretter.opprett(prosessinstans)
        }

        prosessinstans.setData(ProsessDataKey.DIGITAL_SØKNAD_NY_SAK, !harEksisterendeSak)
    }
}

internal fun Prosessinstans.gjelderNySakForDigitalSøknad(): Boolean = hentData<Boolean>(ProsessDataKey.DIGITAL_SØKNAD_NY_SAK)
