package no.nav.melosys.saksflyt.steg.brev

import mu.KotlinLogging
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.dokument.BrevmottakerService
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.oppgave.ÅrsakIkkeSendtInnhentingsbrev
import no.nav.melosys.service.persondata.PersondataFasade
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger { }

/**
 * Sender brevet «Innhenting av inntektsopplysninger» automatisk når en årsavregningsbehandling
 * opprettes automatisk (MELOSYS-8122). Steget kjører som siste steg i prosessflyten
 * [no.nav.melosys.saksflytapi.domain.ProsessType.OPPRETT_NY_BEHANDLING_AARSAVREGNING], som deles av
 * skattepliktige (skattemelding) og ikke-skattepliktige (batch). Mottaker- og fullmektig-logikk
 * (FULLMEKTIG_SØKNAD) håndteres automatisk nedstrøms i brev-tjenesten, så vi sender alltid med
 * [Mottakerroller.BRUKER].
 *
 * Steget gates av prosessdata-flagget [ProsessDataKey.SEND_INNHENTINGSBREV], som settes til `true` kun
 * av de to in-scope automatiske flytene. Den tredje flyten som oppretter årsavregninger
 * (OppretteÅrsavregningVedEndring — et automatisk saga-steg ved behandlingsendring, «Under avklaring
 * med Nav M&A») bruker 3-arg-varianten som setter flagget til `false`, og sender derfor ikke brev. Når
 * og om de to in-scope flytene i det hele tatt oppretter årsavregninger styres allerede oppstrøms
 * (skattepliktige bak MELOSYS_SKATTEHENDELSE_CONSUMER, ikke-skattepliktige via manuell admin-jobb), så
 * et eget brev-toggle er ikke nødvendig.
 *
 * Brevet sendes ikke dersom mottakeren er en person som mangler adresse (dokgen kan ikke lage brevet),
 * eller en virksomhet som er slettet i Enhetsregisteret (dokdist avviser utsendingen).
 * Da oppdateres oppgaven i stedet, slik at saksbehandler følger opp manuelt (MELOSYS-8378).
 */
@Component
class SendInnhentingAvInntektsopplysningerBrev(
    private val dokumentServiceFasade: DokumentServiceFasade,
    private val brevmottakerService: BrevmottakerService,
    private val persondataFasade: PersondataFasade,
    private val eregFasade: EregFasade,
    private val oppgaveService: OppgaveService,
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.SEND_INNHENTINGSBREV_AARSAVREGNING

    override fun utfør(prosessinstans: Prosessinstans) {
        if (!prosessinstans.finnData(ProsessDataKey.SEND_INNHENTINGSBREV, false)) {
            log.debug { "SEND_INNHENTINGSBREV er false eller mangler for prosessinstans ${prosessinstans.id} — sender ikke innhentingsbrev" }
            return
        }

        val behandling = prosessinstans.hentBehandling
        val brevmottaker = brevmottakerService.avklarMottaker(
            Produserbaredokumenter.INNHENTING_AV_INNTEKTSOPPLYSNINGER, Mottaker.medRolle(Mottakerroller.BRUKER), behandling
        )
        val årsakIkkeSendt = finnÅrsakTilAtBrevIkkeKanSendes(brevmottaker)
        if (årsakIkkeSendt != null) {
            log.info { "Sender ikke innhentingsbrev for sak ${behandling.fagsak.saksnummer}, behandling ${behandling.id}: $årsakIkkeSendt" }
            oppgaveService.oppdaterOppgaveVedIkkeSendtInnhentingsbrev(behandling.id, årsakIkkeSendt)
            return
        }

        log.info { "Sender innhentingsbrev (innhenting_av_inntektsopplysninger) for årsavregningsbehandling ${behandling.id}" }

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = Produserbaredokumenter.INNHENTING_AV_INNTEKTSOPPLYSNINGER
            mottaker = Mottakerroller.BRUKER
        }

        dokumentServiceFasade.produserDokument(behandling.id, brevbestillingDto)
    }

    private fun finnÅrsakTilAtBrevIkkeKanSendes(mottaker: Mottaker): ÅrsakIkkeSendtInnhentingsbrev? = when {
        mottaker.erOrganisasjon() ->
            ÅrsakIkkeSendtInnhentingsbrev.VIRKSOMHET_SLETTET.takeIf { erVirksomhetSlettet(mottaker.orgnrNonNull()) }
        else -> finnÅrsakForPerson(mottaker)
    }

    private fun erVirksomhetSlettet(orgnr: String): Boolean {
        val organisasjon = eregFasade.hentOrganisasjon(orgnr).dokument as OrganisasjonDokument
        val opphoersdato = organisasjon.organisasjonDetaljer.opphoersdato ?: return false
        return opphoersdato.isBefore(LocalDate.now())
    }

    private fun finnÅrsakForPerson(mottaker: Mottaker): ÅrsakIkkeSendtInnhentingsbrev? {
        val ident = mottaker.personIdent ?: mottaker.aktørId ?: return null
        val persondata = persondataFasade.hentPerson(ident)
        return when {
            !persondata.manglerGyldigRegistrertAdresse() -> null
            persondata.erPersonDød() -> ÅrsakIkkeSendtInnhentingsbrev.PERSON_DØD_UTEN_ADRESSE
            else -> ÅrsakIkkeSendtInnhentingsbrev.PERSON_UTEN_ADRESSE
        }
    }
}
