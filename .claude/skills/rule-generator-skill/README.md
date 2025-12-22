# Rule Generator Skill for Claude Code

A Claude Code skill that helps you create concise, effective rules and guidelines for LLMs and agents, based on Anthropic's latest best practices.

## Installation

### Option 1: User-level (Available across all projects)
```bash
mkdir -p ~/.claude/agents
cp rule-generator.md ~/.claude/agents/
```

### Option 2: Project-level (Only in current project)
```bash
mkdir -p .claude/agents
cp rule-generator.md .claude/agents/
```

## Usage

Once installed, Claude Code will automatically detect and use this skill when appropriate.

### Automatic Invocation
Simply describe what you need:
```
I need to create guidelines for my Kotlin test DSL
```

### Explicit Invocation
Request the skill directly:
```
Use the rule-generator subagent to create rules for my API testing framework
```

## What This Skill Does

The rule-generator skill helps you create:
- System prompts for Claude Code subagents
- Guidelines for complex DSLs
- Documentation for custom tools
- Instructions for specific workflows

## Key Features

✅ **Concise by Design** - Follows the "less is more" principle
✅ **Example-Driven** - Shows patterns, not just descriptions
✅ **Context-Aware** - Explains the "why" behind rules
✅ **Kotlin DSL Ready** - Includes patterns for test DSLs
✅ **Based on Anthropic Best Practices** - Uses proven techniques from Claude 4 documentation

## What Makes Good Rules?

This skill follows these principles from Anthropic:

1. **Be Clear & Direct** - Claude is smart, don't over-specify
2. **Show, Don't Tell** - Use concrete examples
3. **Provide Context** - Explain why rules exist
4. **Trust Intelligence** - Guidelines, not rigid scripts
5. **Keep It Short** - The shorter, the better

## Structure of Generated Rules

The skill generates rules with this structure:

```markdown
# [Name] Guidelines

## Purpose
Brief description of what this is for

## Core Principles
- 3-5 key behaviors (not more!)

## Usage Patterns
Concrete examples with code

## What to Avoid
Anti-patterns with explanations

## Success Criteria
Measurable, testable outcomes
```

## Example Use Cases

### Creating a Test DSL Guide
```
Use rule-generator to create guidelines for my Kotlin BDD test DSL
that uses infix functions and builder patterns
```

### Creating a Subagent
```
Use rule-generator to create a code review subagent that focuses
on Kotlin idioms and Spring Boot best practices
```

### Creating API Documentation
```
Use rule-generator to create rules for documenting REST APIs
with OpenAPI specifications
```

## Tips for Best Results

1. **Be Specific**: Describe your DSL/tool's key features
2. **Provide Examples**: Share existing code if available
3. **Mention Constraints**: Note any specific requirements
4. **Iterate**: Start simple, refine based on results

## Verification

After installing, verify the skill is available:

```bash
# In Claude Code
/agents

# You should see "rule-generator" in the list
```

## Customization

Feel free to modify the skill for your specific needs:
- Edit the `tools` field to limit access
- Adjust the system prompt for your domain
- Add domain-specific patterns

## Based On

This skill incorporates best practices from:
- Anthropic's Claude 4 Prompt Engineering Guide
- Anthropic's Effective Context Engineering article
- Claude Code Best Practices documentation
- Real-world usage patterns from Anthropic engineers

## License

Free to use, modify, and share.

## Support

For issues or questions about Claude Code skills:
- Visit: https://docs.claude.com/en/docs/claude-code/sub-agents
- Community: https://www.anthropic.com

---

Created for use with Claude Code - An agentic coding assistant by Anthropic
