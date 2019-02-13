package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Henleggelsesgrunner;

;

public class BehandlingsresultatDto {
    public Henleggelsesgrunner henleggelsegrunnKode;
    public String henleggelseFritekst;

    public BehandlingsresultatDto(Behandlingsresultat behandlingsresultat) {
        henleggelsegrunnKode = behandlingsresultat.getHenleggelsesgrunn();
        henleggelseFritekst = behandlingsresultat.getHenleggelseFritekst();
    }
}
