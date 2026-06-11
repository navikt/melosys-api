package no.nav.melosys.saksflyt.steg.brev

import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Sender brevet «Innhenting av inntektsopplysninger» automatisk når en årsavregningsbehandling
 * opprettes automatisk (MELOSYS-8122). Steget kjører som siste steg i prosessflyten
 * [no.nav.melosys.saksflytapi.domain.ProsessType.OPPRETT_NY_BEHANDLING_AARSAVREGNING], som deles av
 * skattepliktige (skattemelding) og ikke-skattepliktige (batch). Mottaker- og fullmektig-logikk
 * (FULLMEKTIG_SØKNAD) håndteres automatisk nedstrøms i brev-tjenesten, så vi sender alltid med
 * [Mottakerroller.BRUKER].
 *
 * Steget gates av prosessdata-flagget [ProsessDataKey.SEND_INNHENTINGSBREV], som settes kun av de to
 * in-scope automatiske flytene. Saksbehandlingsflyt-konteksten (OppretteÅrsavregningVedEndring,
 * «Under avklaring med Nav M&A») setter ikke flagget og sender derfor ikke brev. Når og om de to
 * flytene i det hele tatt oppretter årsavregninger styres allerede oppstrøms (skattepliktige bak
 * MELOSYS_SKATTEHENDELSE_CONSUMER, ikke-skattepliktige via manuell admin-jobb), så et eget
 * brev-toggle er ikke nødvendig.
 */
@Component
class SendInnhentingAvInntektsopplysningerBrev(
    private val dokumentServiceFasade: DokumentServiceFasade,
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.SEND_INNHENTINGSBREV_AARSAVREGNING

    override fun utfør(prosessinstans: Prosessinstans) {
        if (!prosessinstans.finnData(ProsessDataKey.SEND_INNHENTINGSBREV, false)) {
            log.debug { "SEND_INNHENTINGSBREV er ikke satt for prosessinstans ${prosessinstans.id} — sender ikke innhentingsbrev" }
            return
        }

        val behandling = prosessinstans.hentBehandling
        log.info { "Sender innhentingsbrev (innhenting_av_inntektsopplysninger) for årsavregningsbehandling ${behandling.id}" }

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = Produserbaredokumenter.INNHENTING_AV_INNTEKTSOPPLYSNINGER
            mottaker = Mottakerroller.BRUKER
        }

        dokumentServiceFasade.produserDokument(behandling.id, brevbestillingDto)
    }
}
