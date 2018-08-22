package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.AdresseDto;
import no.nav.melosys.tjenester.gui.dto.GateadresseDto;
import no.nav.melosys.tjenester.gui.dto.OrganisasjonDto;

public class OrganisasjonSerializer extends StdSerializer<OrganisasjonDokument> {

    private final KodeverkService kodeverkService;

    public OrganisasjonSerializer(KodeverkService kodeverkService) {
        super(OrganisasjonDokument.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(OrganisasjonDokument organisasjon, JsonGenerator generator, SerializerProvider provider) throws IOException {
        OrganisasjonDto organisasjonDto = new OrganisasjonDto();

        organisasjonDto.setOrgnr(organisasjon.getOrgnummer());
        organisasjonDto.setNavn(getNavn(organisasjon));
        organisasjonDto.setOppstartdato(organisasjon.getOppstartsdato());
        organisasjonDto.setOrganisasjonsform(kodeverkService.dekod(FellesKodeverk.ENHETSTYPER_JURIDISK_ENHET, organisasjon.getEnhetstype(), LocalDate.now()));

        if (organisasjon.getOrganisasjonDetaljer() == null) {
            return;
        }

        // OrganisasjonDetaljer
        List<GeografiskAdresse> forretningsadresser = organisasjon.getOrganisasjonDetaljer().getForretningsadresse();
        if (forretningsadresser != null) {
            organisasjonDto.setForretningsadresse(tilAdresseDto(forretningsadresser));
        }

        List<GeografiskAdresse> postadresser = organisasjon.getOrganisasjonDetaljer().getPostadresse();
        if (postadresser != null) {
            organisasjonDto.setPostadresse(tilAdresseDto(postadresser));
        }

        generator.writeObject(organisasjonDto);
    }

    private AdresseDto tilAdresseDto(List<GeografiskAdresse> adresser) {
        GeografiskAdresse adresse = null;

        for (GeografiskAdresse a : adresser) {
            Periode gyldighetsperiode = a.getGyldighetsperiode();
            if (gyldighetsperiode.erGyldig()) { // TODO hvis det finnes flere gyldige adresser?
                adresse = a;
                break;
            }
        }

        if (adresse instanceof SemistrukturertAdresse) {
            SemistrukturertAdresse sAdresse = (SemistrukturertAdresse) adresse;

            AdresseDto dto = new AdresseDto();
            GateadresseDto gateadresse = new GateadresseDto();
            dto.setGateadresse(gateadresse);

            // FIXME Hvordan formaterer vi adresselinjer?
            StringBuilder stringBuilder = new StringBuilder();

            String linje1 = sAdresse.getAdresselinje1();
            stringBuilder.append(linje1 == null ? "" : linje1);
            String linje2 = sAdresse.getAdresselinje2();
            stringBuilder.append(linje2 == null ? "" : linje2);
            String linje3 = sAdresse.getAdresselinje3();
            stringBuilder.append(linje3 == null ? "" : linje3);

            String adresseLinje = stringBuilder.toString();
            adresseLinje.replaceAll("\\s+", " ");
            
            gateadresse.setGatenavn(adresseLinje);


            String postNummer = sAdresse.getPostnr();

            dto.setPostnr(postNummer);
            dto.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, postNummer, LocalDate.now()));

            dto.setLand(kodeverkService.dekod(FellesKodeverk.LANDKODERISO2, sAdresse.getLandkode(), LocalDate.now()));

            return  dto;
        }

        return null;
    }

    // Hvis man ikke har bruk for historikk på navn så er det best å bruke navn på nivå organisasjon.
    private String getNavn(OrganisasjonDokument organisasjon) {
        return organisasjon.getNavn() == null ? "UKJENT" : String.join(" ", organisasjon.getNavn());
    }

}
