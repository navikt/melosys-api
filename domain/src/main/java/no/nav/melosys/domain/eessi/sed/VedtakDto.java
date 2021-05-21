package no.nav.melosys.domain.eessi.sed;

import java.time.LocalDate;

public record VedtakDto(boolean førstegangsvedtak, LocalDate datoforrigevedtak) { }
