package no.nav.melosys.domain.brev.trygdeavtale

import no.nav.melosys.domain.brev.Person

@JvmRecord
data class MedfolgendeFamiliemedlemmer(val ektefelle: Person?, val barn: MutableSet<Person?>?)
