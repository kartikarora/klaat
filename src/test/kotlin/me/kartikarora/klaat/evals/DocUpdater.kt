package me.kartikarora.klaat.evals

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DocUpdater {

    fun updateDocs(report: EvalReport, skillsFile: File) {
        skillsFile.parentFile?.mkdirs()
        val html = buildSkillsPageHtml(report)
        skillsFile.writeText(html)
        println("Successfully generated/updated skills benchmark subpage at ${skillsFile.absolutePath}")
    }

    private fun buildSkillsPageHtml(report: EvalReport): String {
        val m = report.metrics
        val formattedDate = try {
            val instant = Instant.parse(report.timestamp)
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm z").withZone(ZoneId.of("UTC")).format(instant)
        } catch (e: Exception) {
            report.timestamp
        }

        val rows = report.results.joinToString("\n") { res ->
            val triggerBadge = if (!res.isPositive) {
                if (!res.withAgent.invokedSkill) {
                    """<span class="status-badge" style="background: rgba(33, 150, 243, 0.15); color: #2196f3; border-color: rgba(33, 150, 243, 0.3);">Ignored (Correct)</span>"""
                } else {
                    """<span class="status-badge required">False Trigger</span>"""
                }
            } else {
                if (res.withAgent.invokedSkill) {
                    """<span class="status-badge" style="background: rgba(76, 175, 80, 0.15); color: #4caf50; border-color: rgba(76, 175, 80, 0.3);">Triggered</span>"""
                } else {
                    """<span class="status-badge required">Missed</span>"""
                }
            }

            val compileBadge = if (res.withAgent.checklist.compiles) {
                """<span class="status-badge" style="background: rgba(76, 175, 80, 0.15); color: #4caf50; border-color: rgba(76, 175, 80, 0.3);">Compiled OK</span>"""
            } else if (!res.isPositive) {
                """<span class="status-badge" style="color: var(--secondary);">-</span>"""
            } else {
                """<span class="status-badge required">Compile Error</span>"""
            }

            val tierBadge = when (res.tier.lowercase()) {
                "easy" -> """<span class="status-badge" style="color: var(--secondary);">Easy</span>"""
                "medium" -> """<span class="status-badge" style="background: rgba(33, 150, 243, 0.15); color: #2196f3;">Medium</span>"""
                "hard" -> """<span class="status-badge" style="background: rgba(255, 152, 0, 0.15); color: #ff9800;">Hard</span>"""
                else -> """<span class="status-badge">${res.tier}</span>"""
            }

            """
            <tr>
                <td><code>${res.id}</code></td>
                <td>$tierBadge</td>
                <td><span style="font-size: 0.85rem; color: var(--secondary);">${res.category}</span></td>
                <td>$triggerBadge</td>
                <td>$compileBadge</td>
                <td style="font-size: 0.85rem; color: var(--secondary);">${escapeHtml(res.prompt.take(75))}...</td>
            </tr>
            """.trimIndent()
        }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Klaat AI Skills Benchmark &amp; Evals</title>
    <meta name="description" content="Automated evaluation benchmarks measuring autonomous skill discovery, false-positive resistance, and compiler pass rates for Klaat AI skills.">
    <meta name="author" content="Kartik Arora">
    
    <!-- Brand Identity CSS -->
    <link href="https://distribute.kartikarora.me/css/kartikarora.1.7.0.css" rel="stylesheet">
    
    <!-- Favicons -->
    <link rel="icon" type="image/png" sizes="32x32" href="https://kartikarora.me/img/icon32.png">
    <link rel="icon" type="image/png" sizes="16x16" href="https://kartikarora.me/img/icon16.png">
    <link rel="apple-touch-icon" sizes="180x180" href="https://kartikarora.me/img/icon180.png">
    <link rel="apple-touch-icon" sizes="192x192" href="https://kartikarora.me/img/icon192.png">

    <!-- Required Fonts & Icons -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500;600&family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    
    <!-- Docs Specific CSS -->
    <link href="style.css" rel="stylesheet">
    
    <!-- Brand CDN JS -->
    <script src="https://distribute.kartikarora.me/js/kartikarora.1.7.0.js" defer></script>
</head>
<body>
    <nav class="nav">
        <div class="nav-container">
            <a href="../index.html" class="nav-brand">Klaat <span class="text-muted" style="font-size: 0.8rem; font-weight: normal; margin-left: 5px;">Skills Benchmark</span></a>
            <button class="nav-toggle" id="nav-toggle" aria-label="Toggle navigation">
                <span class="material-symbols-outlined">menu</span>
            </button>
            <div class="nav-links" id="nav-links">
                <ul style="display: flex; list-style: none; gap: var(--space-md); align-items: center; margin: 0; padding: 0;">
                    <li><a href="../index.html" class="nav-link">Home</a></li>
                    <li><a href="index.html" class="nav-link">Web Man Pages</a></li>
                    <li><a href="format.html" class="nav-link">Format Guide</a></li>
                    <li><a href="skills.html" class="nav-link active">Skills Benchmark</a></li>
                    <li><a href="https://github.com/kartikarora/klaat" class="nav-link" target="_blank">GitHub</a></li>
                </ul>
            </div>
        </div>
    </nav>

    <div class="docs-wrapper">
        <!-- Sidebar Navigation -->
        <aside class="docs-sidebar" id="docs-sidebar">
            <div class="docs-sidebar-nav">
                <!-- Group 1: Benchmark Sections -->
                <div class="docs-sidebar-group">
                    <div class="docs-sidebar-title">Skills Evaluation</div>
                    <ul class="docs-sidebar-links">
                        <li><a href="#overview" class="docs-sidebar-link active">Overview</a></li>
                        <li><a href="#scorecard" class="docs-sidebar-link">Scorecard</a></li>
                        <li><a href="#test-suite" class="docs-sidebar-link">Test Breakdown</a></li>
                        <li><a href="#methodology" class="docs-sidebar-link">Methodology</a></li>
                    </ul>
                </div>

                <!-- Group 2: Quick Links -->
                <div class="docs-sidebar-group">
                    <div class="docs-sidebar-title">Documentation</div>
                    <ul class="docs-sidebar-links">
                        <li><a href="index.html#cmd-install-skills" class="docs-sidebar-link"><span class="material-symbols-outlined" style="font-size: 1rem; vertical-align: middle; margin-right: 4px;">terminal</span>klaat install-skills</a></li>
                        <li><a href="format.html" class="docs-sidebar-link"><span class="material-symbols-outlined" style="font-size: 1rem; vertical-align: middle; margin-right: 4px;">menu_book</span>Format Guide</a></li>
                        <li><a href="index.html" class="docs-sidebar-link"><span class="material-symbols-outlined" style="font-size: 1rem; vertical-align: middle; margin-right: 4px;">description</span>All CLI Commands</a></li>
                    </ul>
                </div>
            </div>
        </aside>

        <!-- Main Content Area -->
        <main class="docs-content">
            <!-- Section 1: Overview -->
            <section id="overview" class="docs-section">
                <h1>AI Agent Skills Benchmark</h1>
                <p>Klaat bundles official AI formatting skills (such as <code>klaat-markdown-author</code>, <code>klaat-cli</code>, and <code>klaat-gdocs-author</code>) that install into 55+ coding agent environments via <code>klaat install-skills</code>. To ensure these guidelines genuinely improve agent output and prevent hallucination, we evaluate them against both live LLM agents and deterministic compiler validation.</p>

                <!-- Metric Summary Cards -->
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: var(--space-md); margin: var(--space-lg) 0;">
                    <div style="background: var(--card); border: 1px solid var(--border); border-radius: var(--border-radius); padding: var(--space-md); text-align: center;">
                        <div style="font-size: 0.8rem; text-transform: uppercase; color: var(--secondary); font-weight: 700; letter-spacing: 0.05em;">Autonomous Trigger</div>
                        <div style="font-size: 2.2rem; font-weight: 800; color: #4caf50; margin: var(--space-xs) 0;">${"%.0f".format(m.triggerRecallPct)}%</div>
                        <div style="font-size: 0.8rem; color: var(--secondary);">Sensitivity on codelab tasks</div>
                    </div>
                    <div style="background: var(--card); border: 1px solid var(--border); border-radius: var(--border-radius); padding: var(--space-md); text-align: center;">
                        <div style="font-size: 0.8rem; text-transform: uppercase; color: var(--secondary); font-weight: 700; letter-spacing: 0.05em;">False Trigger Resistance</div>
                        <div style="font-size: 2.2rem; font-weight: 800; color: #2196f3; margin: var(--space-xs) 0;">${"%.0f".format(100.0 - m.falsePositiveRatePct)}%</div>
                        <div style="font-size: 0.8rem; color: var(--secondary);">Specificity on distractor prompts</div>
                    </div>
                    <div style="background: var(--card); border: 1px solid var(--border); border-radius: var(--border-radius); padding: var(--space-md); text-align: center;">
                        <div style="font-size: 0.8rem; text-transform: uppercase; color: var(--secondary); font-weight: 700; letter-spacing: 0.05em;">Klaat Export Pass Rate</div>
                        <div style="font-size: 2.2rem; font-weight: 800; color: #4caf50; margin: var(--space-xs) 0;">${"%.0f".format(m.agentCompileRatePct)}%</div>
                        <div style="font-size: 0.8rem; color: var(--secondary);">vs. ${"%.0f".format(m.baselineCompileRatePct)}% without skill</div>
                    </div>
                    <div style="background: var(--card); border: 1px solid var(--border); border-radius: var(--border-radius); padding: var(--space-md); text-align: center;">
                        <div style="font-size: 0.8rem; text-transform: uppercase; color: var(--secondary); font-weight: 700; letter-spacing: 0.05em;">Duration Tag Adherence</div>
                        <div style="font-size: 2.2rem; font-weight: 800; color: #4caf50; margin: var(--space-xs) 0;">${"%.0f".format(m.agentDurationCompliancePct)}%</div>
                        <div style="font-size: 0.8rem; color: var(--secondary);">vs. ${"%.0f".format(m.baselineDurationCompliancePct)}% without skill</div>
                    </div>
                </div>
            </section>

            <!-- Section 2: Scorecard -->
            <section id="scorecard" class="docs-section">
                <h2>A/B Benchmark Scorecard</h2>
                <p>We compare an agent with only standard programming knowledge (<strong>Baseline</strong>) against an agent with access to Klaat's skills catalog (<strong>With Klaat Skill</strong>).</p>

                <div class="man-page">
                    <div class="man-section">
                        <div class="man-section-title">EVALUATION SUMMARY</div>
                        <div class="man-content">
                            <p style="margin-bottom: var(--space-sm);">Evaluation Model: <strong>${report.model}</strong> &bull; Run Mode: <strong>${report.mode}</strong> &bull; Last Evaluated: <strong>$formattedDate</strong></p>
                            <table class="options-table">
                                <thead>
                                    <tr>
                                        <th>Evaluation Criteria</th>
                                        <th>Baseline (No Skill)</th>
                                        <th>With Klaat Skill</th>
                                        <th>Net Improvement</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td><code>Klaat Codelab Compilation</code></td>
                                        <td>${"%.0f".format(m.baselineCompileRatePct)}%</td>
                                        <td><strong>${"%.0f".format(m.agentCompileRatePct)}%</strong></td>
                                        <td><span class="status-badge" style="background: rgba(76, 175, 80, 0.15); color: #4caf50;">+${"%.0f".format(m.agentCompileRatePct - m.baselineCompileRatePct)}%</span></td>
                                    </tr>
                                    <tr>
                                        <td><code>Duration Tag Presence</code></td>
                                        <td>${"%.0f".format(m.baselineDurationCompliancePct)}%</td>
                                        <td><strong>${"%.0f".format(m.agentDurationCompliancePct)}%</strong></td>
                                        <td><span class="status-badge" style="background: rgba(76, 175, 80, 0.15); color: #4caf50;">+${"%.0f".format(m.agentDurationCompliancePct - m.baselineDurationCompliancePct)}%</span></td>
                                    </tr>
                                    <tr>
                                        <td><code>Klaat Callout Markers</code></td>
                                        <td>${"%.0f".format(m.baselineCalloutCompliancePct)}%</td>
                                        <td><strong>${"%.0f".format(m.agentCalloutCompliancePct)}%</strong></td>
                                        <td><span class="status-badge" style="background: rgba(76, 175, 80, 0.15); color: #4caf50;">+${"%.0f".format(m.agentCalloutCompliancePct - m.baselineCalloutCompliancePct)}%</span></td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </section>

            <!-- Section 3: Test Breakdown -->
            <section id="test-suite" class="docs-section">
                <h2>Test Suite Prompt Breakdown</h2>
                <p>The prompt suite includes target codelab authoring requests as well as distractor prompts (general documentation, standard READMEs, blog posts) to verify that skills do not hijack non-codelab markdown tasks.</p>

                <div class="man-page">
                    <div class="man-section">
                        <div class="man-section-title">DETAILED PROMPT CASES</div>
                        <div class="man-content">
                            <table class="options-table">
                                <thead>
                                    <tr>
                                        <th>Prompt ID</th>
                                        <th>Difficulty</th>
                                        <th>Category</th>
                                        <th>Autonomous Trigger</th>
                                        <th>Klaat Compiler Result</th>
                                        <th>Prompt Preview</th>
                                    </tr>
                                </thead>
                                <tbody>
$rows
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </section>

            <!-- Section 4: Methodology -->
            <section id="methodology" class="docs-section">
                <h2>Evaluation Methodology</h2>
                <p>Our eval suite uses a two-stage approach combining autonomous agent simulation and compiler-backed deterministic validation:</p>

                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: var(--space-md); margin-top: var(--space-md);">
                    <div style="background: var(--card); border: 1px solid var(--border); border-radius: var(--border-radius); padding: var(--space-lg);">
                        <h3 style="display: flex; align-items: center; gap: var(--space-xs); margin-bottom: var(--space-sm);">
                            <span class="material-symbols-outlined" style="color: var(--accent);">psychology</span>
                            1. Autonomous Invocation Test
                        </h3>
                        <p style="font-size: 0.95rem; color: var(--secondary);">
                            We do not hardcode <code>SKILL.md</code> into the prompt. Instead, we present the agent with a catalog of skill descriptions and a <code>read_skill</code> tool. We check whether the model independently chooses to invoke <code>read_skill("klaat-markdown-author")</code> when handed a codelab prompt, and whether it correctly leaves the tool untouched for general markdown requests.
                        </p>
                    </div>

                    <div style="background: var(--card); border: 1px solid var(--border); border-radius: var(--border-radius); padding: var(--space-lg);">
                        <h3 style="display: flex; align-items: center; gap: var(--space-xs); margin-bottom: var(--space-sm);">
                            <span class="material-symbols-outlined" style="color: #4caf50;">fact_check</span>
                            2. Deterministic Compiler Verification
                        </h3>
                        <p style="font-size: 0.95rem; color: var(--secondary);">
                            Rather than relying only on subjective LLM judges, we feed the agent's raw output directly into Klaat's official <code>MarkdownParser</code>. A test only passes if the output extracts valid YAML metadata (id, summary, categories), valid per-step durations (<code>Duration: &gt; 0</code>), and produces compilable HTML with zero parser errors.
                        </p>
                    </div>
                </div>

                <div class="card" style="margin-top: var(--space-lg); padding: var(--space-md); border: 1px solid var(--border); border-radius: var(--border-radius); background: var(--bg-alt);">
                    <div style="font-size: 0.85rem; color: var(--secondary);">
                        <strong>Run this eval locally:</strong>
                        <pre style="margin-top: 8px;"><code class="language-bash">./gradlew evalSkills -PupdateDocs=true</code></pre>
                    </div>
                </div>
            </section>
        </main>
    </div>

    <script>
        document.addEventListener('DOMContentLoaded', () => {
            const sidebarLinks = document.querySelectorAll('.docs-sidebar-link');
            const sections = document.querySelectorAll('.docs-section');

            const observerOptions = {
                root: null,
                rootMargin: '-100px 0px -70%',
                threshold: 0
            };

            const observer = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        const activeId = entry.target.getAttribute('id');
                        sidebarLinks.forEach(link => {
                            if (link.getAttribute('href') === '#' + activeId) {
                                link.classList.add('active');
                            } else {
                                link.classList.remove('active');
                            }
                        });
                    }
                });
            }, observerOptions);

            sections.forEach(section => observer.observe(section));
        });
    </script>
</body>
</html>
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
