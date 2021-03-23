#Melosys dokumenter
Melosys er i ferd med å ta over ansvar for egne brevmaler gjennom [melosys-dokgen](https://github.com/navikt/melosys-dokgen). 

Dette betyr at det er flere ansvarsområder som må håndteres av melosys-api for nye brevmaler:
 - Bestilling av produsering
 - Journalføring
 - Distribuering
 - Holde på informasjon som i dag ligger i dokkat hos Team Dokument - dette er malnavn og journalføringsinfo (tittel og kategori)

## Registere ny brevmal i melosys-api
Når en ny brevmal skal tas i bruk må følgende på plass:
 - Ny mal i melosys-dokgen med tilhørende JSON-schema
 - Eventuelt nytt ProduserbareDokumenter i melosys-kodeverk der term er tekst som vises for saksbehandler
 - Oppretting av DTO mot melosys-dokgen basert på JSON-schema for malen. Se [DokgenDto](https://github.com/navikt/melosys-api/blob/master/integrasjon/src/main/java/no/nav/melosys/integrasjon/dokgen/dto/DokgenDto.java)
 - Mapping av [DokumentproduksjonsInfo](DokumentproduksjonsInfoMapper.java) 
    - Dokumentert på [Confluence](https://confluence.adeo.no/display/TEESSI/Kodeverk+i+Melosys#KodeverkiMelosys-DokumenteriMelosys)
 - Mapping av mottakere med tilhørende regler i [BrevmottakerMapper](BrevmottakerMapper.java) 
    - Reglene for mottakere er dokumentert på [Confluence](https://confluence.adeo.no/pages/viewpage.action?pageId=395304999)
    
Dersom det er aktuelt å se på det så er adresseoppsett dokumentert [her](https://confluence.adeo.no/pages/viewpage.action?pageId=405772174)