package no.nav.melosys.domain.behandlingsgrunnlag;

import java.util.EnumMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

public final class BehandlingsgrunnlagKonverterer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final EnumMap<BehandlingsGrunnlagType, Class<? extends BehandlingsgrunnlagData>> mapper = new EnumMap<>(BehandlingsGrunnlagType.class);

    static {
        mapper.put(BehandlingsGrunnlagType.GENERELT, BehandlingsgrunnlagData.class);
        mapper.put(BehandlingsGrunnlagType.SØKNAD, SoeknadDokument.class);
        objectMapper.registerModule(new JavaTimeModule());
    }

    public static void oppdaterBehandlingsgrunnlag(Behandlingsgrunnlag behandlingsgrunnlag) {
        if (behandlingsgrunnlag.getBehandlingsgrunnlagdata() != null) {
            try {
                behandlingsgrunnlag.setJsonData(lagJsonFraType(behandlingsgrunnlag.getBehandlingsgrunnlagdata()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Kan ikke lage json fra datagrunnlag. Behandlingsgrunnlag id: " + behandlingsgrunnlag.getId());
            }
        }
    }

    public static void lastBehandlingsgrunnlag(Behandlingsgrunnlag behandlingsgrunnlag) {
        try {
            behandlingsgrunnlag.setBehandlingsgrunnlagdata(
                lagDatagrunnlagFraType(behandlingsgrunnlag.getJsonData(), klasseForType(behandlingsgrunnlag.getType()))
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kan ikke laste datagrunnlag med id " + behandlingsgrunnlag.getId(), e);
        }
    }

    public static Class<? extends BehandlingsgrunnlagData> klasseForType(BehandlingsGrunnlagType type) {
        return mapper.get(type);
    }

    private static BehandlingsgrunnlagData lagDatagrunnlagFraType(String json, Class<? extends BehandlingsgrunnlagData> clazz) throws JsonProcessingException {
        return objectMapper.readValue(json, clazz);
    }

    private static String lagJsonFraType(BehandlingsgrunnlagData behandlingsgrunnlagdata) throws JsonProcessingException {
        return objectMapper.writeValueAsString(behandlingsgrunnlagdata);
    }
}
