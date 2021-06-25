package no.nav.melosys.tjenester.gui.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record OrgIdNavnDto(String orgId, String navn) {
    public static List<OrgIdNavnDto> av(Map<String, String> map) {
        return map.entrySet().stream().map(x -> new OrgIdNavnDto(x.getKey(), x.getValue())).collect(Collectors.toList());
    }
}
