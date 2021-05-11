package no.nav.melosys.service.vedtak.dto;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.JuridiskArbeidsgiverNorge;
import no.nav.melosys.domain.behandlingsgrunnlag.data.LoennOgGodtgjoerelse;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public record Soknad(Trygdedekninger dekning, LoennOgGodtgjoerelse lonnOgGodtgjorelse, JuridiskArbeidsgiverNorge arbeidsgiver, List<ForetakUtland> arbeidssteder, LocalDate mottaksDato, Periode periode) {
}
