package no.nav.melosys.saksflyt.steg.fakturering

import no.nav.melosys.domain.Behandling

internal const val BESKRIVELSE_OPPHØR_MEDLEMSKAP = "Opphør av medlemskap"
internal const val BESKRIVELSE_ANNULLERING_TRYGDEAVGIFT = "Annullering av fakturert trygdeavgift"

internal fun utledKanselleringsbeskrivelse(behandling: Behandling): String =
    if (behandling.erEøsPensjonist()) BESKRIVELSE_ANNULLERING_TRYGDEAVGIFT
    else BESKRIVELSE_OPPHØR_MEDLEMSKAP
