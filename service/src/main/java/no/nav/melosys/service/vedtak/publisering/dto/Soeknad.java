package no.nav.melosys.service.vedtak.publisering.dto;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland;
import no.nav.melosys.domain.mottatteopplysninger.data.JuridiskArbeidsgiverNorge;
import no.nav.melosys.domain.mottatteopplysninger.data.LoennOgGodtgjoerelse;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public record Soeknad(Trygdedekninger dekning, LoennOgGodtgjoerelse loennOgGodtgjoerelse, JuridiskArbeidsgiverNorge arbeidsgiver, List<ForetakUtland> arbeidssteder, LocalDate mottaksDato, Periode periode) {
}
