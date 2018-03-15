package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.service.kodeverk.Kodeverk;
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

        medlemsperiodeDto.type = medlemsperiode.type;
        if (periode != null) {
            medlemsperiodeDto.periode = new PeriodeDto(periode.getFom(), periode.getTom());
        }
        if (medlemsperiode.land != null) {
            medlemsperiodeDto.land = getKodeverdi(Kodeverk.LANDKODER, medlemsperiode.land);
        }
        if (medlemsperiode.grunnlagstype != null) {
            medlemsperiodeDto.grunnlagstype = getKodeverdi(Kodeverk.GRUNNLAG_MEDL, medlemsperiode.grunnlagstype);
        }
        if (medlemsperiode.kilde != null) {
            medlemsperiodeDto.kilde = getKodeverdi(Kodeverk.KILDESYSTEM_MEDL, medlemsperiode.kilde);
        }
        if (medlemsperiode.kildedokumenttype != null) {
            medlemsperiodeDto.kildedokumenttype = getKodeverdi(Kodeverk.KILDEDOKUMENT_MEDL, medlemsperiode.kildedokumenttype);
        }
        if (medlemsperiode.lovvalg != null) {
            medlemsperiodeDto.lovvalg = getKodeverdi(Kodeverk.LOVVALG_MEDL, medlemsperiode.lovvalg);
        }
        if (medlemsperiode.status != null) {
            medlemsperiodeDto.status = getKodeverdi(Kodeverk.PERIODESTATUS_MEDL, medlemsperiode.status);
        }
        if (medlemsperiode.trygdedekning != null) {
            medlemsperiodeDto.trygdedekning = getKodeverdi(Kodeverk.DEKNING_MEDL, medlemsperiode.trygdedekning);
        }

        generator.writeObject(medlemsperiodeDto);
    }

    private Map<String, String> getKodeverdi(Kodeverk kodeverk, String kode) {
        Map<String, String> kodeverdi = new HashMap<>();
        kodeverdi.put("kode", kode);

        String term = kodeverkService.dekod(kodeverk, kode, LocalDate.now());
        kodeverdi.put("term", term);

        return kodeverdi;
    }
}
