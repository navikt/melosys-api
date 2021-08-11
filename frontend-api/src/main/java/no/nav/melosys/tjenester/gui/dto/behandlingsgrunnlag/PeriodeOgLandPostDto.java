package no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag;

import java.time.LocalDate;
import java.util.Collection;

public record PeriodeOgLandPostDto(LocalDate fom, LocalDate tom, Collection<String> land) {}
