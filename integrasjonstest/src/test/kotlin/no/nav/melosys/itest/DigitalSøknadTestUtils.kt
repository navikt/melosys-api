package no.nav.melosys.itest

import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import java.util.UUID

/** Låsreferansen for digital søknad er `{gruppe}_{skjemaId}`, så skjemaets prosess finnes på slutten av referansen. */
internal fun ProsessinstansRepository.finnDigitalSøknadProsess(skjemaId: UUID): Prosessinstans? =
    findAll().singleOrNull { it.type.erDigitalSøknadMottak() && it.låsReferanse?.endsWith(skjemaId.toString()) == true }
