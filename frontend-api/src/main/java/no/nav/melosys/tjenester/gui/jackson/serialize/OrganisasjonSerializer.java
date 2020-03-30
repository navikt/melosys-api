package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Organisasjon;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.AdresseDto;
import no.nav.melosys.tjenester.gui.dto.GateadresseDto;
import no.nav.melosys.tjenester.gui.dto.OrganisasjonDto;
import org.apache.commons.lang3.StringUtils;

public class OrganisasjonSerializer extends StdSerializer<Organisasjon> {

    private final transient KodeverkService kodeverkService;

    public OrganisasjonSerializer(KodeverkService kodeverkService) {
        super(Organisasjon.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(Organisasjon organisasjon, JsonGenerator generator, SerializerProvider provider) throws IOException {
        OrganisasjonDto organisasjonDto = new OrganisasjonDto();

        organisasjonDto.setOrgnr(organisasjon.getOrgnummer());
        organisasjonDto.setNavn(organisasjon.getNavn());
        organisasjonDto.setOppstartdato(organisasjon.getOppstartsdato());
        if (StringUtils.isNotEmpty(organisasjon.getEnhetstype())) {
            organisasjonDto.setOrganisasjonsform(kodeverkService.dekod(FellesKodeverk.ENHETSTYPER_JURIDISK_ENHET, organisasjon.getEnhetstype(), LocalDate.now()));
        }

        organisasjonDto.setForretningsadresse(tilAdresseDto(organisasjon.getForretningsadresse()));
        organisasjonDto.setPostadresse(tilAdresseDto(organisasjon.getPostadresse()));

        generator.writeObject(organisasjonDto);
    }

    private AdresseDto tilAdresseDto(StrukturertAdresse adresse) {
        AdresseDto dto = new AdresseDto();
        GateadresseDto gateadresse = new GateadresseDto();
        dto.setGateadresse(gateadresse);

        if (adresse == null) {
            // Tomt objekt til frontend (ikke null)
            return dto;
        }

        gateadresse.setGatenavn(adresse.gatenavn);

        dto.setPostnr(adresse.postnummer);
        String poststed = StringUtils.isNotEmpty(adresse.poststed) ? adresse.poststed : kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.postnummer, LocalDate.now());
        dto.setPoststed(poststed);
        dto.setLand(kodeverkService.dekod(FellesKodeverk.LANDKODERISO2, adresse.landkode, LocalDate.now()));

        return dto;
    }
}
