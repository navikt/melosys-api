package no.nav.melosys.domain.eessi.sed;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.nav.melosys.domain.eessi.SedType;

public class SedGrunnlagTypeResolver extends TypeIdResolverBase {

    private static final Class<? extends SedGrunnlagDto> DEFAULT_CLASS = SedGrunnlagDto.class;
    private static final List<String> SED_TYPES_STRING = Arrays.stream(SedType.values()).map(SedType::name).collect(Collectors.toList());
    private static final Map<SedType, Class<? extends SedGrunnlagDto>> SED_GRUNNLAG_TYPER =
        Maps.immutableEnumMap(ImmutableMap.<SedType, Class<? extends SedGrunnlagDto>>builder()
            .put(SedType.A003, SedGrunnlagA003Dto.class)
            .build());

    private JavaType sedType;

    @Override
    public void init(JavaType javaType) {
        this.sedType = javaType;
    }

    @Override
    public String idFromValue(Object o) {
        return null;
    }

    @Override
    public String idFromValueAndType(Object o, Class<?> aClass) {
        return null;
    }

    @Override
    public String idFromBaseType() {
        return null;
    }

    @Override
    public JavaType typeFromId(DatabindContext databindContext, String s) {
        Class<?> type;
        if (SED_TYPES_STRING.contains(s) && SED_GRUNNLAG_TYPER.containsKey(SedType.valueOf(s))) {
            type = SED_GRUNNLAG_TYPER.get(SedType.valueOf(s));
        } else {
            type = DEFAULT_CLASS;
        }

        return databindContext.constructSpecializedType(sedType, type);
    }

    @Override
    public String getDescForKnownTypeIds() {
        return null;
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.NAME;
    }
}
