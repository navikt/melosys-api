# JaCoCo Coverage Reports

This project uses JaCoCo for code coverage analysis. Each module generates its own coverage report.

## Quick Start

### Generate Coverage Reports

Run tests with coverage:
```bash
make coverage
```

Or using Maven directly:
```bash
mvn clean test
```

### View Summary

Show a simple one-line-per-module summary:
```bash
make coverage-summary
```

Or directly:
```bash
./scripts/coverage/summary.sh
```

### View Detailed Reports

Each module generates detailed HTML reports at:
```
<module-name>/target/site/jacoco/index.html
```

For example:
- `domain/target/site/jacoco/index.html`
- `service/target/site/jacoco/index.html`
- `integrasjon/target/site/jacoco/index.html`

## Example Output

```
===================================================================
JaCoCo Coverage Summary
===================================================================
Module                         Lines   Branches    Methods
-------------------------------------------------------------------
config                         10.0%      15.0%      10.1%
domain                         36.4%      13.7%      36.6%
frontend-api                   70.5%      55.5%      67.4%
integrasjon                    60.7%      45.0%      50.9%
saksflyt                       80.1%      67.7%      64.0%
service                        83.6%      67.2%      77.7%
-------------------------------------------------------------------
TOTAL                          66.2%      54.7%      49.9%
===================================================================
```

## Understanding the Metrics

- **Lines**: Percentage of executable code lines covered by tests
- **Branches**: Percentage of decision branches (if/else, switch, etc.) covered
- **Methods**: Percentage of methods executed by tests

## CI/CD Integration

The coverage reports are lightweight and suitable for GitHub Actions:

```yaml
- name: Run tests with coverage
  run: mvn clean test

- name: Show coverage summary
  run: ./scripts/coverage/summary.sh

- name: Upload coverage reports
  uses: actions/upload-artifact@v4
  with:
    name: coverage-reports
    path: |
      */target/site/jacoco/
```

## Adding Coverage Thresholds

To fail builds when coverage drops below a threshold, add to the parent `pom.xml`:

```xml
<execution>
    <id>check</id>
    <goals>
        <goal>check</goal>
    </goals>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.60</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

## Excluding Code from Coverage

To exclude classes or packages from coverage analysis, add to individual module `pom.xml`:

```xml
<configuration>
    <excludes>
        <exclude>**/generated/**</exclude>
        <exclude>**/config/**</exclude>
    </excludes>
</configuration>
```
