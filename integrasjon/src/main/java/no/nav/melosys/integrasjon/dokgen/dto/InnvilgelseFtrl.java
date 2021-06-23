package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.Periode;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.Trygdeavgift;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class InnvilgelseFtrl extends DokgenDto {

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoMottatt;

    private final List<Periode> perioder;
    private final Trygdeavgift trygdeavgift;
    private final boolean erFullstendigInnvilget;
    private final String ftrl_2_8_begrunnelse;
    private final boolean vurderingMedlemskapEktefelle;
    private final boolean vurderingLovvalgBarn;
    private final List<OmfattetFamilie> omfattetFamilie;
    private final List<OmfattetFamilie> ikkeOmfattetBarn;
    private final OmfattetFamilie ikkeOmfattetEktefelle;
    private final String fritekstInnledning;
    private final String fritekstBegrunnelse;
    private final String fritekstEktefelle;
    private final String fritekstBarn;
    private final String saksbehandlerNavn;
    private final String arbeidsgiverNavn;
    private final String arbeidsland;
    private final boolean trygdeavtaleMedArbeidsland;
    private final VurderingTrygdeavgift vurderingTrygdeavgift;
    private final Loennsforhold loennsforhold;
    private final String arbeidsgiverFullmektigNavn;
    private final boolean brukerHarFullmektig;
    private final String avgiftssatsAar;
    private final boolean loennNorgeSkattepliktig;
    private final boolean loennUtlandSkattepliktig;

    private InnvilgelseFtrl(InnvilgelseBrevbestilling brevbestilling, MedlemAvFolketrygden medlemAvFolketrygden) {
        super(brevbestilling);
        perioder = medlemAvFolketrygden.getMedlemskapsperioder().stream().map(Periode::new).collect(Collectors.toList());
    }

    public static InnvilgelseFtrl av(InnvilgelseBrevbestilling brevbestilling, MedlemAvFolketrygden medlemAvFolketrygden) {
        return new InnvilgelseFtrl(brevbestilling, medlemAvFolketrygden);
    }
}
