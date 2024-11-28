package no.nav.melosys.tjenester.gui.config.jackson.serialize;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikkeyrkesaktivsituasjontype;
import no.nav.melosys.tjenester.gui.kodeverk.KodeDto;

/**
 * Alle klasser som implementerer {@code Kodeverk} serialiseres enten som kode og term, eller som string,
 */
public class KodeSerializer extends StdSerializer<Kodeverk> {
    private static final Collection<Class<? extends Kodeverk>> IKKE_MAPPES_TIL_KODE_DTO = Set.of(
        Avsendertyper.class, Mottatteopplysningertyper.class, Fartsomrader.class, Flyvningstyper.class,
        Folketrygdloven_kap2_bestemmelser.class, Innretningstyper.class, InnvilgelsesResultat.class,
        Medlemskapstyper.class, Tema.class, Trygdedekninger.class, Vilkaar.class, Mottakerroller.class,
        Aktoersroller.class, Trygdeavtale_myndighetsland.class, Land_iso2.class, Skatteplikttype.class,
        Inntektskildetype.class, Ikkeyrkesaktivsituasjontype.class, Fullmaktstype.class, Vertslandsavtale_bestemmelser.class
    );

    public KodeSerializer() {
        super(Kodeverk.class);
    }

    @Override
    public void serialize(Kodeverk value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        if (skalMappesTilKodeDto(value)) {
            KodeDto kodeDto = new KodeDto(value.getKode(), value.getBeskrivelse());
            generator.writeObject(kodeDto);
        } else {
            generator.writeString(value.getKode());
        }
    }

    private boolean skalMappesTilKodeDto(Kodeverk kodeverkObjekt) {
        return IKKE_MAPPES_TIL_KODE_DTO.stream().noneMatch(clazz -> clazz.isInstance(kodeverkObjekt));
    }
}
