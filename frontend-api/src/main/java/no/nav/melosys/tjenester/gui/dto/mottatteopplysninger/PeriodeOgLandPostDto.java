package no.nav.melosys.tjenester.gui.dto.mottatteopplysninger;

import java.time.LocalDate;
import java.util.List;

public record PeriodeOgLandPostDto(LocalDate fom, LocalDate tom, List<String> land) {}
