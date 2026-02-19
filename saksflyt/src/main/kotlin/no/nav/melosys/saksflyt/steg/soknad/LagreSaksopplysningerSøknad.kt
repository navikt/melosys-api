package no.nav.melosys.saksflyt.steg.soknad

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey.SØKNADSDATA
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Saga-steg som mapper søknadsdata til saksopplysninger (MottatteOpplysninger) på behandlingen.
 *
 * Henter søknadsdata fra prosessinstansen (lagret av HENT_SØKNADSDATA), mapper til
 * UtsendtArbeidstakerSøknad domenemodell via UtsendtArbeidstakerSøknadMapper, og lagrer som
 * MottatteOpplysninger via MottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs().
 *
 * Forutsetninger:
 * - HENT_SØKNADSDATA har kjørt og lagret søknadsdata på prosessinstansen
 * - OPPRETT_SAK_OG_BEHANDLING_SØKNAD har kjørt og satt behandling på prosessinstansen
 */
@Component
class LagreSaksopplysningerSøknad(
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val objectMapper: ObjectMapper
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.LAGRE_SAKSOPPLYSNINGER_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = requireNotNull(prosessinstans.behandling) {
            "Behandling må være opprettet før saksopplysninger kan lagres"
        }

        val søknadsdata = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(SØKNADSDATA)
        val referanseId = søknadsdata.referanseId

        log.info { "Mapper og lagrer saksopplysninger fra digital søknad, referanseId=$referanseId, behandlingId=${behandling.id}" }

        val søknad = UtsendtArbeidstakerSøknadMapper.tilUtsendtArbeidstakerSøknad(søknadsdata)
        val originalData = objectMapper.writeValueAsString(søknadsdata)

        mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(
            behandling.id,
            originalData,
            søknad,
            referanseId
        )

        log.info { "Lagret saksopplysninger for digital søknad referanseId=$referanseId" }
    }
}
