package no.nav.melosys.domain.jpa;

import java.util.EnumMap;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.grunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.grunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.grunnlag.BehandlingsgrunnlagData;

public class BehandlingsgrunnlagListener {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final EnumMap<BehandlingsGrunnlagType, Class<? extends BehandlingsgrunnlagData>> mapper = new EnumMap<>(BehandlingsGrunnlagType.class);

    static {
        mapper.put(BehandlingsGrunnlagType.BEH_GRUNNLAG, BehandlingsgrunnlagData.class);
        mapper.put(BehandlingsGrunnlagType.SOEKNAD_GRUNNLAG, SoeknadDokument.class);
        objectMapper.registerModule(new JavaTimeModule());
    }

    @PrePersist
    public void oppdaterBehandlingsgrunnlag(Behandlingsgrunnlag behandlingsgrunnlag) {
        if (behandlingsgrunnlag.getBehandlingsgrunnlagdata() != null) {
            try {
                behandlingsgrunnlag.setJsonData(lagJsonFraType(behandlingsgrunnlag.getBehandlingsgrunnlagdata()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Kan ikke lage json fra datagrunnlag. Behandlingsgrunnlag id: " + behandlingsgrunnlag.getId());
            }
        }
    }

    @PostLoad
    public void lastBehandlingsgrunnlag(Behandlingsgrunnlag behandlingsgrunnlag) {
        try {
            behandlingsgrunnlag.setBehandlingsgrunnlagdata(
                lagDatagrunnlagFraType(behandlingsgrunnlag.getJsonData(), mapper.get(behandlingsgrunnlag.getType()))
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kan ikke laste datagrunnlag med id " + behandlingsgrunnlag.getId(), e);
        }
    }

    private BehandlingsgrunnlagData lagDatagrunnlagFraType(String json, Class<? extends BehandlingsgrunnlagData> clazz) throws JsonProcessingException {
        return objectMapper.readValue(json, clazz);
    }

    private String lagJsonFraType(BehandlingsgrunnlagData behandlingsgrunnlagdata) throws JsonProcessingException {
        return objectMapper.writeValueAsString(behandlingsgrunnlagdata);
    }
}
