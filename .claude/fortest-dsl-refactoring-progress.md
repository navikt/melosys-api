# forTest DSL Refactoring Progress

## Status: In Progress

## Completed

### Shared Factories (service module)
- [x] `SaksbehandlingDataFactory.kt` - Converted FagsakTestFactory.builder(), Behandlingsresultat().apply, Saksopplysning().apply
- [x] `MottatteOpplysningerStub.kt` - Converted MottatteOpplysninger().apply, Soeknad().apply
- [x] `SaksopplysningStubs.kt` - Converted Saksopplysning().apply, removed mocks
- [x] `DataByggerStubs.kt` - Converted Soeknad().apply, ForetakUtland().apply, PersonDokument().apply

### service/behandling tests
- [x] `AngiBehandlingsresultatServiceTest.kt` - Converted Behandlingsresultat().apply, Medlemskapsperiode().apply
- [x] `ReplikerBehandlingsresultatServiceTest.kt` - Converted all Entity().apply patterns, removed lateinit vars
- [x] `AvsluttArt13BehandlingServiceTest.kt` - Converted Behandlingsresultat().apply, Lovvalgsperiode().apply, removed lateinit vars
- [x] `UtledMottaksdatoTest.kt` - Converted MottatteOpplysninger().apply

### DSL Extensions Added
- `SoeknadTestFactory`: Added `oppholdUtland()`, `maritimtArbeid {}`, `foretakUtland(ForetakUtland)`, `foretakUtlandMedDetaljer {}`, `utenlandskIdent()`, `erSelvstendig`
- `SaksopplysningTestFactory`: Added `kilde {}` for SaksopplysningKilde
- `PersonDokumentTestFactory`: Added `erEgenAnsatt`, `familiemedlem {}`
- `BehandlingsresultatTestFactory`: Added `kontrollresultat {}`
- `BehandlingTestFactory`: Added `behandlingsaarsak {}`
- `MottatteOpplysningerTestFactory`: Added `mottaksdato`

## Remaining (service module)

### service/dokument tests (~25 files)
- [ ] BrevDataServiceTest.kt
- [ ] DokumentServiceTest.kt
- [ ] BrevmottakerServiceTest.kt
- [ ] TilBrevAdresseServiceTest.kt
- [ ] HentMuligeBrevmottakereServiceTest.kt
- [ ] DokgenTestData.kt
- [ ] Various mapper tests (InnvilgelsesbrevMapperTest, etc.)

### service/ftrl tests (~5 files)
- [ ] AvklarteFaktaForBestemmelseTest.kt
- [ ] VilkårForBestemmelseIkkeYrkesaktivTest.kt
- [ ] VilkårForBestemmelsePensjonistTest.kt
- [ ] VilkårForBestemmelseYrkesaktivTest.kt
- [ ] UtledMedlemskapsperiodeTest.kt

### service/other tests (~20 files)
- [ ] OppfriskSaksopplysningerServiceTest.kt
- [ ] UnntaksregistreringServiceTest.kt
- [ ] UnntaksperiodeServiceTest.kt
- [ ] AnmodningsperiodeServiceTest.kt
- [ ] LovligeKombinasjonerSaksbehandlingServiceTest.kt
- [ ] OpprettLovvalgsperiodeServiceTest.kt
- [ ] TrygdeavtaleServiceTest.kt
- [ ] And more...

## Anti-patterns to Convert
- `Entity().apply {}` → `Entity.forTest {}` or `entityForTest {}`
- `FagsakTestFactory.builder()...build()` → `Fagsak.forTest {}`
- `lateinit var entity` → local val in each test
- Post-construction mutation → configure in DSL block

## Test Results
- 1018 service tests passing as of last commit
