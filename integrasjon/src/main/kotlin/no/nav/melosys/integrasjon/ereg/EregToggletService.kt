package no.nav.melosys.integrasjon.ereg

import io.getunleash.Unleash
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.featuretoggle.ToggleName.MELOSYS_EREG_ORGANISASJON
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.*

@Service
@Primary // Flyttet fra EregService - Vi burde se om vi kan fjerne @Primary helt når vi rydder bort toggle
class EregToggletService(
    private var unleash: Unleash,
    private val eregService: EregService,
    private val eregRestService: EregRestService
) : EregFasade {
    override fun hentOrganisasjon(orgnr: String): Saksopplysning {
        if (unleash.isEnabled(MELOSYS_EREG_ORGANISASJON)) {
            return eregRestService.hentOrganisasjon(orgnr)
        }
        return eregService.hentOrganisasjon(orgnr)
    }

    override fun finnOrganisasjon(orgnr: String): Optional<Saksopplysning> {
        if (unleash.isEnabled(MELOSYS_EREG_ORGANISASJON)) {
            return eregRestService.finnOrganisasjon(orgnr)
        }
        return eregService.finnOrganisasjon(orgnr)
    }

    override fun hentOrganisasjonNavn(orgnummer: String): String {
        if (unleash.isEnabled(MELOSYS_EREG_ORGANISASJON)) {
            return eregRestService.hentOrganisasjonNavn(orgnummer)
        }
        return eregService.hentOrganisasjonNavn(orgnummer)
    }
}
