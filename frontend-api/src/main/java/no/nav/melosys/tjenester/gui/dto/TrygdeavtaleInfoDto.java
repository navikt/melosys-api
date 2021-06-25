package no.nav.melosys.tjenester.gui.dto;


import no.nav.melosys.service.kodeverk.KodeDto;

import java.util.List;

public record TrygdeavtaleInfoDto (List<OrgIdNavnDto> virksomheter, List<KodeDto> barn, KodeDto ektefelle) {}
