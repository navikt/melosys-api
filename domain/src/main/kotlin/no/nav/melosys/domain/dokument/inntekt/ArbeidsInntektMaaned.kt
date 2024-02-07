package no.nav.melosys.domain.dokument.inntekt

import com.fasterxml.jackson.annotation.JsonView
import no.nav.melosys.domain.dokument.DokumentView
import java.time.YearMonth
import javax.validation.constraints.NotNull

 data class ArbeidsInntektMaaned(
     @JvmField val aarMaaned: YearMonth? = null,
     @JsonView(DokumentView.Database::class) val avvikListe: @NotNull List<Avvik>? = emptyList(),
     @JvmField val arbeidsInntektInformasjon: ArbeidsInntektInformasjon
)
