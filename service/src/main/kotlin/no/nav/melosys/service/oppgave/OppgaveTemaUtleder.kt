package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

class OppgaveTemaUtleder {
    fun utledTema(sakstype: Sakstyper, sakstema: Sakstemaer?, behandlingstema: Behandlingstema, behandlingstype: Behandlingstyper): Tema {
        if (sakstype == Sakstyper.FTRL && behandlingstema == Behandlingstema.UNNTAK_MEDLEMSKAP)
            return Tema.UFM
        if (behandlingstype == Behandlingstyper.ÅRSAVREGNING) {
            return Tema.TRY
        }

        return when (sakstema) {
            Sakstemaer.MEDLEMSKAP_LOVVALG -> Tema.MED
            Sakstemaer.TRYGDEAVGIFT -> Tema.TRY
            Sakstemaer.UNNTAK -> Tema.UFM
            else -> {
                error("ingen mapping for sakstema:$sakstema")
            }
        }
    }
}
