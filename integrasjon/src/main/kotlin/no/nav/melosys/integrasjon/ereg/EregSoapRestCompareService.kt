package no.nav.melosys.integrasjon.ereg

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.featuretoggle.ToggleName
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger { }

@Service
@Primary
// @Primary Flyttet fra EregService - Vi burde se om vi kan fjerne den helt
// når vi rydder bort toggle melosys.ereg.organisasjon
class EregSoapRestCompareService(
    private var unleash: Unleash,
    private val eregService: EregService,
    private val eregRestService: EregRestService,
) : EregFasade {

    override fun hentOrganisasjon(orgnr: String): Saksopplysning {
        val organisasjonRest = runAndLogErrors(orgnr) {
            eregRestService.hentOrganisasjon(orgnr)
        }
        val organisasjonSoap = eregService.hentOrganisasjon(orgnr)

        if (unleash.isEnabled(ToggleName.MELOSYS_EREG_ORGANISASJON) && organisasjonRest != null) {
            return organisasjonRest
        }
        return organisasjonSoap
    }

    override fun finnOrganisasjon(orgnr: String): Optional<Saksopplysning> {
        val organisasjonRest = runAndLogErrors(orgnr) {
            eregRestService.finnOrganisasjon(orgnr).orElse(null)
        }

        val organisasjonSoap = eregService.finnOrganisasjon(orgnr)

        if (organisasjonSoap.isEmpty && organisasjonRest != null) {
            log.warn("Ereg: organisasjonSoap er tom men rest gir svar for ${filterOrgnummerSomErFnr(orgnr)}")
        }

        if (unleash.isEnabled(ToggleName.MELOSYS_EREG_ORGANISASJON)) {
            return Optional.ofNullable(organisasjonRest)
        }

        return organisasjonSoap
    }

    fun filterOrgnummerSomErFnr(orgnummer: String): String =
        if (orgnummer.length == 11) "***********" else orgnummer

    override fun hentOrganisasjonNavn(orgnummer: String): String {
        val organisasjonNavnRest = runAndLogErrors(orgnummer) {
            eregRestService.hentOrganisasjonNavn(orgnummer)
        }
        val organisasjonNavnSoap = eregService.hentOrganisasjonNavn(orgnummer)

        if (unleash.isEnabled(ToggleName.MELOSYS_EREG_ORGANISASJON) && organisasjonNavnRest != null) {
            return organisasjonNavnRest
        }

        return organisasjonNavnSoap
    }

    private fun <T> runAndLogErrors(orgnr: String, action: () -> T?): T? {
        return try {
            action()
        } catch (e: Exception) {
            log.warn("Ereg: Kall mot rest endepunkt feilet for ${filterOrgnummerSomErFnr(orgnr)}", e)
            null
        }
    }
}
