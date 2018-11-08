package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.MedlemsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.PeriodeDto;

public class MedlemsperiodeSerializer extends StdSerializer<Medlemsperiode> {

    private KodeverkService kodeverkService;

    public MedlemsperiodeSerializer(KodeverkService kodeverkService) {
        super(Medlemsperiode.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(Medlemsperiode medlemsperiode, JsonGenerator generator, SerializerProvider provider) throws IOException {
        MedlemsperiodeDto medlemsperiodeDto = new MedlemsperiodeDto();
        Periode periode = medlemsperiode.getPeriode();

        medlemsperiodeDto.periodetype = medlemsperiode.getType();
        medlemsperiodeDto.periode = getPeriode(periode);
        medlemsperiodeDto.land = kodeverkService.getKodeverdi(FellesKodeverk.LANDKODER, medlemsperiode.getLand());
        // Feltet er required, men vi mapper til en Melosys-enum, og vil dermed ikke ha grunnlagstype for andre medlemsperioder
        if (medlemsperiode.getGrunnlagstype() != null) {
            medlemsperiodeDto.grunnlagstype = kodeverkService.getKodeverdi(FellesKodeverk.GRUNNLAG_MEDL, medlemsperiode.getGrunnlagstype().getKode());
        }
        medlemsperiodeDto.kilde = kodeverkService.getKodeverdi(FellesKodeverk.KILDESYSTEM_MEDL, medlemsperiode.getKilde());
        medlemsperiodeDto.kildedokumenttype = kodeverkService.getKodeverdi(FellesKodeverk.KILDEDOKUMENT_MEDL, medlemsperiode.getKildedokumenttype());
        medlemsperiodeDto.lovvalg = kodeverkService.getKodeverdi(FellesKodeverk.LOVVALG_MEDL, medlemsperiode.getLovvalg());
        medlemsperiodeDto.status = kodeverkService.getKodeverdi(FellesKodeverk.PERIODESTATUS_MEDL, medlemsperiode.getStatus());
        if (medlemsperiode.getTrygdedekning() != null) {
            medlemsperiodeDto.trygdedekning = kodeverkService.getKodeverdi(FellesKodeverk.DEKNING_MEDL, medlemsperiode.getTrygdedekning().getKode());
        }

        generator.writeObject(medlemsperiodeDto);
    }

    private PeriodeDto getPeriode(Periode periode) {
        if (periode == null) {
            return null;
        }
        return new PeriodeDto(periode.getFom(), periode.getTom());
    }
    
}
