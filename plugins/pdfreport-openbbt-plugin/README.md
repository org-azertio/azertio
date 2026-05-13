# pdfreport-azertio-plugin

An Azertio plugin that generates paginated PDF test reports after each execution.

## Report structure

| Page | Content |
|------|---------|
| Cover | Project title, organization, execution metadata, test suite list, and logo |
| Statistics | Three donut charts (test suites, test features, test cases) with pass/fail/error breakdown |
| Content | One section per suite → feature → scenario, with step details |

The cover page shows:
- **Test Plan Date** — when the test plan was defined
- **Execution Date** — when the execution ran
- **Profile** — execution profile, if set
- **Test cases** — total count
- **Test suites** — list of suite names

Each scenario displays its identifier, tags, duration, and result badge. Steps optionally include inline `Document` (code block) and `DataTable` (grid) arguments.

Page numbers (`n / total`) appear at the bottom-right of every page. An optional footer text is placed at the bottom-left.

## Configuration

All keys are prefixed with `pdfreport.`.

### Output

| Key | Default | Description |
|-----|---------|-------------|
| `outputDir` | `.azertio/reports` | Directory where PDF files are written. Relative paths are resolved from the working directory. |
| `outputFile` | `%Y%m%d-%h%M%s.pdf` | Filename pattern. Supports the tokens below, relative to `outputDir`. |

**Filename tokens**

| Token | Expands to |
|-------|-----------|
| `%Y`  | Year (4 digits) |
| `%m`  | Month (2 digits) |
| `%d`  | Day (2 digits) |
| `%h`  | Hour (2 digits, 24 h) |
| `%M`  | Minute (2 digits) |
| `%s`  | Second (2 digits) |

### Branding

| Key | Default | Description |
|-----|---------|-------------|
| `title` | Project name | Title shown on the cover page. |
| `accentColor` | Dark navy (`#21213D`) | Primary brand color as `#RRGGBB`. Suite headers, feature bars, tag labels, and the cover band are all derived automatically from this single value. |
| `logoPath` | _(none)_ | Path to a PNG or JPEG image embedded in the top-right corner of the cover header. |
| `footer` | _(none)_ | Fixed text shown at the bottom-left of every page (e.g. a confidentiality notice). |

### Layout

| Key | Default | Description |
|-----|---------|-------------|
| `pageBreak` | `none` | Where automatic page breaks are inserted. |

**`pageBreak` values**

| Value | Behaviour |
|-------|-----------|
| `none` | Content flows continuously across pages. |
| `suite` | Each test suite starts on a new page. |
| `feature` | Each feature starts on a new page. |
| `test_case` | Each test case starts on a new page. |

### Content

| Key | Default | Description |
|-----|---------|-------------|
| `includePassedSteps` | `true` | When `false`, only failed and errored steps are rendered, producing a more concise report. |

## Example configuration

```yaml
pdfreport.outputDir: reports
pdfreport.outputFile: "%Y%m%d-%h%M%s.pdf"

pdfreport.title: "REST API Test Report"
pdfreport.accentColor: "#1A4A6A"
pdfreport.logoPath: assets/logo.png
pdfreport.footer: "Confidential — ACME Corporation — Internal use only"

pdfreport.pageBreak: suite
pdfreport.includePassedSteps: false
```