package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.kodeverk.KodeDto;
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
        medlemsperiodeDto.periode = getPeriode(periode);
        medlemsperiodeDto.land = getKodeverdiIgnorerFeil(FellesKodeverk.LANDKODER, medlemsperiode.land);
        medlemsperiodeDto.grunnlagstype = getKodeverdiIgnorerFeil(FellesKodeverk.GRUNNLAG_MEDL, medlemsperiode.grunnlagstype);
        medlemsperiodeDto.kilde = getKodeverdiIgnorerFeil(FellesKodeverk.KILDESYSTEM_MEDL, medlemsperiode.kilde);
        medlemsperiodeDto.kildedokumenttype = getKodeverdiIgnorerFeil(FellesKodeverk.KILDEDOKUMENT_MEDL, medlemsperiode.kildedokumenttype);
        medlemsperiodeDto.lovvalg = getKodeverdiIgnorerFeil(FellesKodeverk.LOVVALG_MEDL, medlemsperiode.lovvalg);
        medlemsperiodeDto.status = getKodeverdiIgnorerFeil(FellesKodeverk.PERIODESTATUS_MEDL, medlemsperiode.status);
        medlemsperiodeDto.trygdedekning = getKodeverdiIgnorerFeil(FellesKodeverk.DEKNING_MEDL, medlemsperiode.trygdedekning);

        generator.writeObject(medlemsperiodeDto);
    }

    private PeriodeDto getPeriode(Periode periode) {
        if (periode == null) {
            return null;
        }
        return new PeriodeDto(periode.getFom(), periode.getTom());
    }
    
    
    private KodeDto getKodeverdiIgnorerFeil(FellesKodeverk kodeverk, String kode) {
        try {
            return kodeverkService.getKodeverdi(kodeverk, kode);
        } catch (TekniskException e) {
            return new KodeDto(kode, "TEKNISK FEIL");
        }
    }
    
}
