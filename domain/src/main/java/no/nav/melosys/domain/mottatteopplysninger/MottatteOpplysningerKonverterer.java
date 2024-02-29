package no.nav.melosys.domain.mottatteopplysninger;

import java.util.EnumMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper;

public final class MottatteOpplysningerKonverterer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final EnumMap<Mottatteopplysningertyper, Class<? extends MottatteOpplysningerData>> mapper = new EnumMap<>(Mottatteopplysningertyper.class);

    static {
        mapper.put(Mottatteopplysningertyper.SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS, SøknadNorgeEllerUtenforEØS.class);
        mapper.put(Mottatteopplysningertyper.SØKNAD_IKKE_YRKESAKTIV, SøknadIkkeYrkesaktiv.class);
        mapper.put(Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS, Soeknad.class);
        mapper.put(Mottatteopplysningertyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS, Soeknad.class);
        mapper.put(Mottatteopplysningertyper.SED, SedGrunnlag.class);
        mapper.put(Mottatteopplysningertyper.ANMODNING_ELLER_ATTEST, AnmodningEllerAttest.class);
        objectMapper.registerModules(new JavaTimeModule(), new KotlinModule.Builder().build());
    }

    private MottatteOpplysningerKonverterer() {
    }

    public static void oppdaterMottatteOpplysninger(MottatteOpplysninger mottatteOpplysninger) {
        if (mottatteOpplysninger.getMottatteOpplysningerData() != null) {
            try {
                mottatteOpplysninger.setJsonData(lagJsonFraType(mottatteOpplysninger.getMottatteOpplysningerData()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Kan ikke lage json fra datagrunnlag. MottatteOpplysninger id: " + mottatteOpplysninger.getId());
            }
        }
    }

    public static void lastMottatteOpplysninger(MottatteOpplysninger mottatteOpplysninger) {
        try {
            mottatteOpplysninger.setMottatteOpplysningerData(
                lagDatagrunnlagFraType(mottatteOpplysninger.getJsonData(), klasseForType(mottatteOpplysninger.getType()))
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kan ikke laste datagrunnlag med id " + mottatteOpplysninger.getId(), e);
        }
    }

    public static Class<? extends MottatteOpplysningerData> klasseForType(Mottatteopplysningertyper type) {
        return mapper.get(type);
    }

    private static MottatteOpplysningerData lagDatagrunnlagFraType(String json, Class<? extends MottatteOpplysningerData> clazz) throws JsonProcessingException {
        return objectMapper.readValue(json, clazz);
    }

    private static String lagJsonFraType(MottatteOpplysningerData mottatteOpplysningerData) throws JsonProcessingException {
        return objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(mottatteOpplysningerData);
    }
}
