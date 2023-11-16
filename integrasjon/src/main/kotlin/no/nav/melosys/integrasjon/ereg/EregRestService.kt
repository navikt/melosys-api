package no.nav.melosys.integrasjon.ereg

import mu.KotlinLogging
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningKildesystem
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonRestConsumer
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger { }

@Service
class EregRestService(
    private val organisasjonRestConsumer: OrganisasjonRestConsumer
) : EregFasade {
    private val eregDtoTilSaksopplysningKonverter: EregDtoTilSaksopplysningKonverter = EregDtoTilSaksopplysningKonverter()

    override fun hentOrganisasjon(orgnr: String): Saksopplysning {
        if (erUgyldigOrgnummer(orgnr)) {
            throw TekniskException("orgnr er ikke gyldig")
        }
        val organisasjon = organisasjonRestConsumer.hentOrganisasjon(orgnr)
        return eregDtoTilSaksopplysningKonverter.lagSaksopplysning(organisasjon).apply {
            leggTilKildesystemOgMottattDokument(
                SaksopplysningKildesystem.EREG, organisasjon.tilJsonString()
            )
            type = SaksopplysningType.ORG
            versjon = EREG_REST_VERSJON
        }
    }


    override fun finnOrganisasjon(orgnr: String): Optional<Saksopplysning> {
        if (erUgyldigOrgnummer(orgnr)) {
            throw TekniskException("orgnr er ikke gyldig")
        }
        return try {
            Optional.ofNullable(hentOrganisasjon(orgnr))
        } catch (ex: IkkeFunnetException) {
            log.warn(ex.message)
            Optional.empty()
        }
    }

    override fun hentOrganisasjonNavn(orgnr: String): String? =
        (hentOrganisasjon(orgnr).dokument as OrganisasjonDokument).getSammenslåttNavn()

    private fun erUgyldigOrgnummer(orgnr: String) = orgnr.length == 11

    companion object {
        const val EREG_REST_VERSJON = "REST 2.0"
    }

}
