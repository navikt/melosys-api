package no.nav.melosys.tjenester.gui.config.jackson.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.periode.KodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.MedlemsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;

public class MedlemsperiodeSerializer extends StdSerializer<Medlemsperiode> {

    private final transient KodeverkService kodeverkService;

    public MedlemsperiodeSerializer(KodeverkService kodeverkService) {
        super(Medlemsperiode.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(Medlemsperiode medlemsperiode, JsonGenerator generator, SerializerProvider provider) throws IOException {
        MedlemsperiodeDto medlemsperiodeDto = new MedlemsperiodeDto();
        Periode periode = medlemsperiode.getPeriode();

        medlemsperiodeDto.setPeriodeID(medlemsperiode.getId());
        medlemsperiodeDto.setPeriodetype(getKodeverdi(FellesKodeverk.PERIODETYPE_MEDL, medlemsperiode.getType()));
        medlemsperiodeDto.setPeriode(getPeriode(periode));
        medlemsperiodeDto.setLand(getKodeverdi(FellesKodeverk.LANDKODER, medlemsperiode.getLand()));
        medlemsperiodeDto.setGrunnlagstype(getKodeverdi(FellesKodeverk.GRUNNLAG_MEDL, medlemsperiode.getGrunnlagstype()));
        medlemsperiodeDto.setKilde(new KodeDto(medlemsperiode.getKilde(), medlemsperiode.getKilde()));
        medlemsperiodeDto.setKildedokumenttype(getKodeverdi(FellesKodeverk.KILDEDOKUMENT_MEDL, medlemsperiode.getKildedokumenttype()));
        medlemsperiodeDto.setLovvalg(getKodeverdi(FellesKodeverk.LOVVALG_MEDL, medlemsperiode.getLovvalg()));
        medlemsperiodeDto.setStatus(getKodeverdi(FellesKodeverk.PERIODESTATUS_MEDL, medlemsperiode.getStatus()));
        medlemsperiodeDto.setTrygdedekning(getKodeverdi(FellesKodeverk.DEKNING_MEDL, medlemsperiode.getTrygdedekning()));
        generator.writeObject(medlemsperiodeDto);
    }

    private PeriodeDto getPeriode(Periode periode) {
        if (periode == null) {
            return null;
        }
        return new PeriodeDto(periode.getFom(), periode.getTom());
    }

    private KodeDto getKodeverdi(FellesKodeverk kodeverk, String kode) {
        if (kode == null) {
            return null;
        }
        return new KodeDto(kode, kodeverkService.dekod(kodeverk, kode));
    }

}
