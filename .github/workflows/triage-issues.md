---
name: triage-issues
on:
  issue:
    types: [opened, edited, labeled]
engine: copilot
permissions:
  issues: write
  repository: read
strict: true
---

# Triage Issues Workflow

## Purpose
This workflow automatically triages new and updated issues in the repository.

## Steps
1. Analyze the issue title and body for keywords and context.
2. Apply appropriate labels (e.g., bug, enhancement, question) based on content.
3. Assign the issue to relevant team members if possible.
4. Add a comment with triage results and next steps.

## Instructions
- This workflow is triggered when an issue is opened, edited, or labeled.
- It uses Copilot as the AI engine for analysis and actions.
- Ensure permissions for writing issues and reading repository data are set.

## Example Prompt
Analyze the following issue and determine:
- The correct label(s)
- If assignment is needed, suggest a team member
- Add a comment summarizing the triage

---

**Issue Title:** {{ issue.title }}
**Issue Body:** {{ issue.body }}

---

Respond with:
- Labels to apply
- Assignment suggestion
- Triage summary comment

## Security
- Strict mode enabled for safe outputs.
- No template injection.
- Minimal permissions.

## References
- [GitHub Agentic Workflows Documentation](https://github.com/github/gh-aw)
