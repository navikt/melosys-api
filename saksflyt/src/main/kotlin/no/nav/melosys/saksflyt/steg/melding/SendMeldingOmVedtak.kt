package no.nav.melosys.saksflyt.steg.melding

import mu.KotlinLogging
import no.nav.melosys.integrasjon.hendelser.KafkaMelosysHendelseProducer
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.vedtak.VedtakHendelseMeldingBygger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

private val log = KotlinLogging.logger { }

@Component
@Profile("!local-q1 & !local-q2")
class SendMeldingOmVedtak(
    private val kafkaMelosysHendelseProducer: KafkaMelosysHendelseProducer,
    private val persondataService: PersondataService,
    private val vedtakHendelseMeldingBygger: VedtakHendelseMeldingBygger
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.SEND_MELDING_OM_VEDTAK
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.hentBehandling

        val fagsak = behandling.fagsak
        val brukersAktørID = fagsak.hentBrukersAktørID()
        val folkeregisterIdent = persondataService.finnFolkeregisterident(brukersAktørID).getOrNull()

        if (folkeregisterIdent == null) {
            log.warn("Fant ikke folkeregisterident for sak: ${fagsak.saksnummer} med behandling: ${behandling.id}")
            return
        }

        kafkaMelosysHendelseProducer.produserBestillingsmelding(
            vedtakHendelseMeldingBygger.bygg(behandling, folkeregisterIdent)
        )
        log.info { "Sendte melding om vedtak for sak: ${fagsak.saksnummer} med behandling: ${behandling.id}" }
    }
}
