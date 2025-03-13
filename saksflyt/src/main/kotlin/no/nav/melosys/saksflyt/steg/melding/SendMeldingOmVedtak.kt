package no.nav.melosys.saksflyt.steg.melding

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.hendelser.KafkaMelosysHendelseProducer
import no.nav.melosys.integrasjon.hendelser.MelosysHendelse
import no.nav.melosys.integrasjon.hendelser.Periode
import no.nav.melosys.integrasjon.hendelser.VedtakHendelseMelding
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

private val log = KotlinLogging.logger { }

@Component
@Profile("!local-q1 & !local-q2")
class SendMeldingOmVedtak(
    private val kafkaMelosysHendelseProducer: KafkaMelosysHendelseProducer,
    private val persondataService: PersondataService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val unleash: Unleash
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.SEND_MELDING_OM_VEDTAK
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        if (!unleash.isEnabled(ToggleName.MELOSYS_SEND_MELDING_OM_VEDTAK)) {
            return
        }
        val behandling = prosessinstans.behandling

        val fagsak = behandling.fagsak
        val brukersAktørID = fagsak.hentBrukersAktørID()
        val folkeregisterIdent = persondataService.finnFolkeregisterident(brukersAktørID).getOrNull()

        if (folkeregisterIdent == null) {
            log.warn("Fant ikke folkeregisterident for sak: ${fagsak.saksnummer} med behandling: ${behandling.id}")
            return
        }

        val behandlingsresultat: Behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
        val behandligsresultatType: Behandlingsresultattyper = behandlingsresultat.type
        val vedtakstype = behandlingsresultat.vedtakMetadata?.vedtakstype

        kafkaMelosysHendelseProducer.produserBestillingsmelding(
            MelosysHendelse(
                melding = VedtakHendelseMelding(
                    folkeregisterIdent = folkeregisterIdent,
                    sakstype = fagsak.type,
                    sakstema = fagsak.tema,
                    behandligsresultatType = behandligsresultatType,
                    vedtakstype = vedtakstype,
                    medlemskapsperioder = behandlingsresultat.medlemskapsperioder
                        .mapNotNull { Periode(it.fom, it.tom, it.innvilgelsesresultat) },
                    lovvalgsperioder = behandlingsresultat.lovvalgsperioder.mapNotNull {
                        Periode(it.fom, it.tom, it.innvilgelsesresultat)
                    }
                )
            )
        )
    }
}
