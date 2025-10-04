package no.nav.melosys.domain.dokument.utbetaling;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

public class UtbetalingDokument implements SaksopplysningDokument {

    public List<Utbetaling> utbetalinger = new ArrayList<>();
}
