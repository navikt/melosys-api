package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.service.kodeverk.KodeDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.periode.MedlemsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;

public class MedlemsperiodeSerializer extends StdSerializer<Medlemsperiode> {

    private transient KodeverkService kodeverkService;

    public MedlemsperiodeSerializer(KodeverkService kodeverkService) {
        super(Medlemsperiode.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(Medlemsperiode medlemsperiode, JsonGenerator generator, SerializerProvider provider) throws IOException {
        MedlemsperiodeDto medlemsperiodeDto = new MedlemsperiodeDto();
        Periode periode = medlemsperiode.getPeriode();

        medlemsperiodeDto.periodeID = medlemsperiode.id;
        medlemsperiodeDto.periodetype = kodeverkService.getKodeverdi(FellesKodeverk.PERIODETYPE_MEDL, medlemsperiode.getType());
        medlemsperiodeDto.periode = getPeriode(periode);
        medlemsperiodeDto.land = kodeverkService.getKodeverdi(FellesKodeverk.LANDKODER, medlemsperiode.getLand());
        medlemsperiodeDto.grunnlagstype = new KodeDto(medlemsperiode.getGrunnlagstype(), medlemsperiode.getGrunnlagstype());
        medlemsperiodeDto.kilde = new KodeDto(medlemsperiode.getKilde(), medlemsperiode.getKilde());
        medlemsperiodeDto.kildedokumenttype = kodeverkService.getKodeverdi(FellesKodeverk.KILDEDOKUMENT_MEDL, medlemsperiode.getKildedokumenttype());
        medlemsperiodeDto.lovvalg = kodeverkService.getKodeverdi(FellesKodeverk.LOVVALG_MEDL, medlemsperiode.getLovvalg());
        medlemsperiodeDto.status = kodeverkService.getKodeverdi(FellesKodeverk.PERIODESTATUS_MEDL, medlemsperiode.getStatus());
        medlemsperiodeDto.trygdedekning = kodeverkService.getKodeverdi(FellesKodeverk.DEKNING_MEDL, medlemsperiode.getTrygdedekning());
        generator.writeObject(medlemsperiodeDto);
    }

    private PeriodeDto getPeriode(Periode periode) {
        if (periode == null) {
            return null;
        }
        return new PeriodeDto(periode.getFom(), periode.getTom());
    }

}
