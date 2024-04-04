package no.nav.melosys.domain.brev

import no.nav.melosys.domain.kodeverk.Mottakerroller

data class Mottakerliste(
    val hovedMottaker: Mottakerroller,
    val brevkopiRegler: Collection<BrevkopiRegel> = mutableListOf(),
    val kopiMottakere: Collection<Mottakerroller> = mutableListOf(),
    val fasteMottakere: Collection<NorskMyndighet> = mutableListOf(),
) {
    constructor(hovedMottaker: Mottakerroller) : this(hovedMottaker, mutableListOf(), mutableListOf(), mutableListOf())

    fun kanHaKopier(): Boolean = brevkopiRegler.isNotEmpty()
}
