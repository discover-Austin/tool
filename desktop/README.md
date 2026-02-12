# TradeSketch Desktop

Compose Desktop target for TradeSketch Estimator.

## What It Includes

- Offline project management (create from templates, rename, delete)
- Space editor (wall, room, slab, circle, L-shape, openings)
- Takeoff calculations (drywall, concrete, gravel/mulch, paint)
- Costing rollups (material, labor, markup, tax)
- Export preview and copy (summary, full report, CSV)
- Local JSON persistence in:
  - `~/.tradesketch-estimator/projects.json`
  - `~/.tradesketch-estimator/settings.json`

## Run

From repo root:

```bash
./gradlew :desktop:run
```

On Windows PowerShell:

```powershell
.\gradlew.bat :desktop:run
```

## Package Installers

```bash
./gradlew :desktop:packageDistributionForCurrentOS
```

This uses Compose Desktop native distributions configured in `desktop/build.gradle.kts`.
