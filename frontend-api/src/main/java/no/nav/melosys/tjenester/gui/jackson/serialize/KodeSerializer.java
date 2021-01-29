package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.service.kodeverk.KodeDto;

/**
 * Alle klasser som implementerer {@code Kodeverk} serialiseres enten som kode og term, eller som string,
 * i henhold til https://github.com/navikt/melosys-schema
 */
public class KodeSerializer extends StdSerializer<Kodeverk> {
    private static final Collection<Class<? extends Kodeverk>> IKKE_MAPPES_TIL_KODE_DTO = Set.of(
        Avsendertyper.class, Behandlingsgrunnlagtyper.class, Folketrygdloven_kap2_bestemmelser.class,
        Innretningstyper.class, InnvilgelsesResultat.class, Loenn_forhold.class, Medlemskapstyper.class,
        Saerligeavgiftsgrupper.class, Tema.class, Trygdedekninger.class, Vilkaar.class,
        Vurderingsutfall_trygdeavgift_norsk_inntekt.class, Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.class
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
