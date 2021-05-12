package no.nav.melosys.service.vedtak.publisering.dto;

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;

public record AvklarteFakta(Avklartefaktatyper type, String fakta, String begrunnelse, String begrunnelseFritekst) {
}
