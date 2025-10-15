package no.nav.melosys.domain.dokument.utbetaling

import no.nav.melosys.domain.dokument.SaksopplysningDokument

class UtbetalingDokument : SaksopplysningDokument {

    var utbetalinger: List<Utbetaling> = emptyList()
}
