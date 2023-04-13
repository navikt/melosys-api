package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException

class OppgaveBehandlingstemaGammelUtleder : OppgaveBehandlingstemaUtleder {

    override fun utledOppgaveBehandlingstema(
        sakstype: Sakstyper, sakstema: Sakstemaer, behandlingstema: Behandlingstema, behandlingstype: Behandlingstyper?
    ): OppgaveBehandlingstema {
        if (skalBrukeMelosysBehandlingstemaForOppgaveBehandlingstema(
                sakstype,
                sakstema,
                behandlingstema,
                behandlingstype
            )
        ) {
            return when (behandlingstema) {
                Behandlingstema.PENSJONIST -> OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET
                Behandlingstema.YRKESAKTIV -> OppgaveBehandlingstema.YRKESAKTIV
                Behandlingstema.REGISTRERING_UNNTAK, Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND -> OppgaveBehandlingstema.REGISTRERING_UNNTAK
                Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL -> OppgaveBehandlingstema.ANMODNING_UNNTAK
                else -> throw FunksjonellException("Mangler mapping av behandlingstema $behandlingstema")
            }
        }

        return when (sakstype) {
            Sakstyper.EU_EOS -> OppgaveBehandlingstema.EU_EOS_LAND
            Sakstyper.TRYGDEAVTALE -> OppgaveBehandlingstema.AVTALELAND
            Sakstyper.FTRL -> OppgaveBehandlingstema.UTENFOR_AVTALELAND
        }
    }

    private fun skalBrukeMelosysBehandlingstemaForOppgaveBehandlingstema(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper?
    ): Boolean {
        if (behandlingstype == null) return false

        when (behandlingstema) {
            Behandlingstema.PENSJONIST -> return erPensjonist(sakstema, behandlingstype)
            Behandlingstema.YRKESAKTIV -> return erYrkesaktiv(sakstema, behandlingstype)
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL -> return erAnmodningOmUntakHovedregel(
                sakstype, sakstema, behandlingstype
            )

            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND ->
                return sakstype == Sakstyper.EU_EOS && sakstema == Sakstemaer.UNNTAK && behandlingstype in listOf(
                    Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING
                )

            Behandlingstema.REGISTRERING_UNNTAK ->
                return sakstype == Sakstyper.TRYGDEAVTALE && sakstema == Sakstemaer.UNNTAK && behandlingstype in listOf(
                    Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE
                )

            else -> return false
        }
    }

    private fun erPensjonist(sakstema: Sakstemaer, behandlingstype: Behandlingstyper?) =
        when (sakstema) {
            Sakstemaer.MEDLEMSKAP_LOVVALG -> behandlingstype in listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.KLAGE
            )

            Sakstemaer.TRYGDEAVGIFT -> behandlingstype in listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.KLAGE,
                Behandlingstyper.HENVENDELSE
            )

            Sakstemaer.UNNTAK -> false
        }

    private fun erYrkesaktiv(sakstema: Sakstemaer, behandlingstype: Behandlingstyper?): Boolean =
        sakstema == Sakstemaer.TRYGDEAVGIFT && behandlingstype in listOf(
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.KLAGE,
            Behandlingstyper.HENVENDELSE
        )

    private fun erAnmodningOmUntakHovedregel(
        sakstype: Sakstyper, sakstema: Sakstemaer, behandlingstype: Behandlingstyper?
    ): Boolean =
        when (sakstype) {
            Sakstyper.EU_EOS -> sakstema == Sakstemaer.UNNTAK && behandlingstype in listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING
            )

            Sakstyper.TRYGDEAVTALE -> sakstema == Sakstemaer.UNNTAK && behandlingstype in listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.HENVENDELSE
            )

            else -> false
        }
}
