package no.nav.melosys.saksflyt.steg.melding

import mu.KotlinLogging
import no.nav.melosys.integrasjon.hendelser.KafkaMelosysHendelseProducer
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.integrasjon.hendelser.MelosysHendelse
import no.nav.melosys.integrasjon.hendelser.VedtakHendelseMelding
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

private val log = KotlinLogging.logger { }

@Component
class SendMeldingOmVedtak(
    private val kafkaMelosysHendelseProducer : KafkaMelosysHendelseProducer,
    private val persondataService: PersondataService
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.SEND_MELDING_OM_VEDTAK
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.behandling

        val brukersAktørID = behandling.fagsak.hentBrukersAktørID()
        val folkeregisterIdent = persondataService.finnFolkeregisterident(brukersAktørID)
            .getOrNull()

        if (folkeregisterIdent == null) {
            log.error("Fant ikke folkeregisterident for bruker med aktørID $brukersAktørID")
            return
        }

        kafkaMelosysHendelseProducer.produserBestillingsmelding(
            MelosysHendelse(
                melding = VedtakHendelseMelding(
                    folkeregisterIdent = folkeregisterIdent,
                    sakstype = behandling.fagsak.type,
                    sakstema = behandling.fagsak.tema
                )
            )
        )
    }
}
