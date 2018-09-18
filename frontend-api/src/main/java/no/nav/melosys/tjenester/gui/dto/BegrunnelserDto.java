package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

import no.nav.melosys.service.kodeverk.KodeDto;

public class BegrunnelserDto {
    public List<KodeDto> vesentligVirksomhet;
    public List<KodeDto> ikkeSkip;
    public List<KodeDto> opphold;
}
