# TradeSketch UX Benchmark (February 12, 2026)

## Scope
This note captures UX patterns from successful estimating/takeoff products and maps them to TradeSketch refinements implemented in this sprint.

## Primary references
- Joist: estimate templates, payments, signatures  
  https://www.joist.com/features/estimates/
- Housecall Pro: proposal options (Good/Better/Best)  
  https://www.housecallpro.com/features/estimates/
- ServiceTitan: estimate templates and configurable proposals  
  https://www.servicetitan.com/features/estimates
- ServiceTitan: estimate follow-up and reminder workflows  
  https://www.servicetitan.com/features/estimate-follow-up
- Procore: centralized preconstruction/estimating workflows  
  https://www.procore.com/preconstruction
- STACK: takeoff + estimating workflow positioning  
  https://www.stackct.com/
- PlanSwift: digital takeoff speed and quantity workflows  
  https://www.planswift.com/

## Blueprint-specific references
- magicplan Help Center: room-creation methods on Android (square room, free form, import-and-draw, filler room).  
  https://help.magicplan.app/create-a-plan-from-scratch
- magicplan Help Center: room scan availability note (iOS only; Android uses manual methods).  
  https://help.magicplan.app/room-scan-ios
- AutoCAD Mobile app: touch-first drafting/editing workflow reference for mobile CAD controls.  
  https://www.autodesk.com/products/autocad/mobile-app
- Planner 5D: touch/drag floor-plan editor + fullscreen plan editing experience reference.  
  https://planner5d.com/use/floor-plan-creator
- Houzz Pro Help: creating an estimate from takeoff quantities.  
  https://help.houzz.com/en_US/takeoffs-and-estimating-how-tos/create-estimate-from-takeoff

## Benchmark patterns
- Fast-start flows outperform dense first screens.
- Multi-option proposals increase close flexibility.
- Role-based communication output (client, crew, purchasing) reduces rework.
- Follow-up templates improve conversion consistency.
- Progressive disclosure keeps novice flows simple while preserving pro depth.
- Clear readiness status lowers user confusion in multi-step workflows.
- Blueprint tools perform best when primary flow is obvious and advanced controls are secondary.
- Auto-fix style cleanup actions reduce friction for non-technical users.
- Field apps benefit from direct artifact export (download/share) from the active workflow screen.
- Tablet/large-phone adaptability improves comprehension for complex plan-editing controls.
- Professional drafting workflows benefit from explicit revision controls (undo/redo visibility).
- Client handoff improves with multi-format blueprint output (PNG for chat/workflow, PDF for formal sharing).
- Mobile blueprint editors perform best with direct touch drawing on canvas (tap/drag wall creation) instead of form-only entry.
- Compact collapsible side rails preserve screen real estate while keeping pro controls immediately reachable.
- Pro floor-plan tools improve speed with guided drawing assists (chain drawing, ortho constraints, angle snapping, endpoint anchoring).

## Implemented in TradeSketch
- Simplified home has one-tap creation and one-tap resume for latest estimate.
- Project detail now includes intelligence scoring, strengths/risks, and next-best-action CTA.
- Quick inserts added in project workspace: wall, room, slab.
- Takeoff now supports playbooks: Fast Bid, Balanced, Safety First.
- Takeoff adds smart checks and bid intelligence (unit-rate and cost-share diagnostics).
- Pricing inputs in takeoff now use progressive disclosure (summary first, full controls on demand).
- Export now includes:
  - Proposal Options (Good/Better/Best) with payment schedule suggestions.
  - Audience Brief generator (Client/Crew/Purchasing).
  - Follow-up templates (Same Day/Next Day/Final Nudge).
- Blueprint now includes:
  - command-center readiness score + layout checks,
  - one-row primary flow (`Auto Fix Layout` then `Continue to Takeoff`),
  - dedicated `Pro Tools` grouping for advanced controls,
  - overlap-aware optimization (iterative conflict separation with fallback repack),
  - direct `Download PNG` and `Share PNG` actions for current layer exports,
  - `Download PDF` and `Share PDF` actions for formal deliverables,
  - visible `Undo/Redo` controls with stack counts in command center,
  - direct touch wall drawing in fullscreen canvas (tap/tap and drag/release),
  - pro draw-assist controls (chain mode, ortho lock, angle snap cycle, endpoint anchor snap cycle),
  - collapsible right-side mini-icon rail with rapid pro controls,
  - dimension-intelligent exports (overall envelope dimensions + per-space footprint annotations),
  - responsive two-pane layout on wider devices with compact controls on smaller devices.

## Recommended next wave
- Persist proposal status per project (Draft, Sent, Follow-up, Approved).
- Add signature-ready export variants (client acceptance block).
- Add optional line-item groups for alternate scopes.
- Add calendar-linked follow-up reminders.
