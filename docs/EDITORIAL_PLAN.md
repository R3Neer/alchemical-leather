# Discovery-first documentation plan

## Plan before editing — 2026-09-09

- Audit the public branch and existing documentation; use an isolated checkout so
  concurrent local implementation is not accidentally committed or documented as released.
- Keep a short README: purpose, one first step, truthful experiments, installation,
  essential caveats and an explicit link to the complete guide.
- Preserve existing detailed information in docs/GUIDE.md, with contents/navigation
  and repaired relative links. Do not silently remove technical guidance.
- Retain potion/armor exclusions and splash/lingering gesture; move the full slot table, duration rules and compatibility matrix to a spoiler-rich guide. Suggest Speed with leggings as a reliable first experiment.
- Review against these criteria, correct omissions or justify changes, then review
  again. Check relative links and Markdown diff. Documentation-only commit/push;
  no version bump, gameplay mutation, release or binary installation.

## Review

### First review and adjustment

CauldronService, Infusions and EquipmentInfusions confirm the first experiment, one-effect rule, enchanting exclusion, Crouch pouring and water removal. Kept these constraints visible; moved exact slot/duration tables without changing them. Replaced the old latest-release shortcut with the releases list so alpha/prerelease downloads remain discoverable.

### Repeat review

- The short README offers a concrete first step and optional experiments without
  exposing every rule. Essential requirements and operational caveats remain.
- GUIDE.md preserves the original detailed reference, with contents and a return
  link. All local links and heading anchors in both pages resolve.
- Reviewed attribution, claims and loss of information against the public source.
  No gameplay files, versions, dependencies or local concurrent edits changed.
- Documentation-only validation: link/anchor checks and git diff whitespace checks;
  no new runtime/gameplay validation is claimed by this editorial commit.
