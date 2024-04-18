package no.nav.melosys.saksflyt.steg.melding

import mu.KotlinLogging
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import kotlin.jvm.optionals.getOrNull

private val log = KotlinLogging.logger { }

class SendMeldingOmVedtak(
    @Qualifier("meldingOmVedtak") private val melosysHendelseKafkaTemplate: KafkaTemplate<String, MelosysHendelse>,
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

        melosysHendelseKafkaTemplate.send(
            "meldingOmVedtak",
            MelosysHendelse(
                melding = VedtakHendelseMelding(
                    folkeregisterIdent = folkeregisterIdent, // TODO: må krypteres før sending over kafka
                    sakstype = behandling.fagsak.type,
                    sakstema = behandling.fagsak.tema
                )
            )
        )
    }
}
