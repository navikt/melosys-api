package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.service.kodeverk.Kodeverk;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.KodeDto;
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
        medlemsperiodeDto.periode = getPeriode(periode);
        medlemsperiodeDto.land = getKodeverdi(Kodeverk.LANDKODER, medlemsperiode.land);
        medlemsperiodeDto.grunnlagstype = getKodeverdi(Kodeverk.GRUNNLAG_MEDL, medlemsperiode.grunnlagstype);
        medlemsperiodeDto.grunnlagstype = getKodeverdi(Kodeverk.GRUNNLAG_MEDL, medlemsperiode.land);
        medlemsperiodeDto.kilde = getKodeverdi(Kodeverk.KILDESYSTEM_MEDL, medlemsperiode.kilde);
        medlemsperiodeDto.kildedokumenttype = getKodeverdi(Kodeverk.KILDEDOKUMENT_MEDL, medlemsperiode.kildedokumenttype);
        medlemsperiodeDto.lovvalg = getKodeverdi(Kodeverk.LOVVALG_MEDL, medlemsperiode.lovvalg);
        medlemsperiodeDto.status = getKodeverdi(Kodeverk.PERIODESTATUS_MEDL, medlemsperiode.status);
        medlemsperiodeDto.trygdedekning = getKodeverdi(Kodeverk.DEKNING_MEDL, medlemsperiode.trygdedekning);

        generator.writeObject(medlemsperiodeDto);
    }

    private PeriodeDto getPeriode(Periode periode) {
        if (periode == null) {
            return null;
        }
        return new PeriodeDto(periode.getFom(), periode.getTom());
    }

    private KodeDto getKodeverdi(Kodeverk kodeverk, String kode) {
        if (kode == null) {
            return null;
        }
        return new KodeDto(kode, kodeverkService.dekod(kodeverk, kode, LocalDate.now()));
    }
}
