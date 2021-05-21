package no.nav.melosys.service.vedtak.publisering.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;

public record Saksopplysninger(Person person, LocalDate hentetDato, Periode hentetPeriode) {
}
