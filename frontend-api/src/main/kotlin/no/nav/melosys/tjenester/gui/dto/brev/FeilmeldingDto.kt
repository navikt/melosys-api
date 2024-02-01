package no.nav.melosys.tjenester.gui.dto.brev

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import java.util.List

@JvmRecord
data class FeilmeldingDto(@JvmField val tittel: String, val underpunkter: Collection<FeilmeldingUnderpunkt>) {
    constructor(begrunnelse: Kontroll_begrunnelser, underpunkt: String?) : this(
        begrunnelse.beskrivelse, List.of<FeilmeldingUnderpunkt>(
            FeilmeldingUnderpunkt(
                underpunkt!!
            )
        )
    )

    constructor(begrunnelse: Kontroll_begrunnelser) : this(begrunnelse.beskrivelse, emptyList<FeilmeldingUnderpunkt>())

    constructor(tittel: String?) : this(tittel!!, emptyList<FeilmeldingUnderpunkt>())
}

