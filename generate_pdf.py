#!/usr/bin/env python3
# v2.1 — tightened spacing to fit ≤30 pages
"""
RetailDost — Premium Technical Documentation PDF Generator v2
Section-aware: each major section gets a banner, page break, numbered header.
"""

import os, re, subprocess, sys, tempfile

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
MD_PATH    = os.path.join(SCRIPT_DIR, "TECHNICAL_DOCUMENTATION.md")
OUT_PATH   = os.path.join(os.path.expanduser("~"), "Downloads", "RetailDost_Technical_Documentation_Final.pdf")
CHROME     = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

with open(MD_PATH, "r", encoding="utf-8") as f:
    raw_md = f.read()

# ─── Pre-process: strip noise, compress verbose sections ──────────────────────
def preprocess(md: str) -> str:
    # 1. Remove the long PrintResultCallbackBridge Java block
    md = re.sub(
        r'```java\npackage android\.print;.*?```',
        '```java\n// PrintResultCallbackBridge.java — bridges abstract print callbacks for PDF generation\n```',
        md, flags=re.DOTALL
    )

    # 2. Collapse DAO code blocks that just show imports + truncation comment
    #    (they look like: ```kotlin\npackage ...\nimport...\n  // ... [Boilerplate...]...)
    def collapse_dao(m):
        content = m.group(1)
        # Keep the block only if it has real non-import, non-comment substance
        real_lines = [l for l in content.split('\n')
                      if l.strip() and not l.strip().startswith('import')
                      and not l.strip().startswith('package')
                      and not l.strip().startswith('//')
                      and not l.strip().startswith('#')
                      and l.strip() not in ('```', '')]
        if len(real_lines) <= 4:
            # Extract just the meaningful lines
            key = next((l for l in content.split('\n') if l.strip().startswith('@') or 'suspend fun' in l or 'fun ' in l), '')
            sig = key.strip() or '// DAO interface — see source for full implementation'
            return f'```kotlin\n{sig}\n// … see source for full implementation\n```'
        return m.group(0)

    md = re.sub(r'```kotlin\n(.*?)```', collapse_dao, md, flags=re.DOTALL)

    # 3. Merge consecutive short API endpoint sections
    #    Remove the separator --- lines between API endpoint definitions to pack them tighter
    md = re.sub(r'\n---\n(#### [A-Z]+)', r'\n\1', md)

    # 4. Collapse the "#### N. XxxDao.kt" code stubs in section 7 into a compact list
    md = re.sub(
        r'(#### \d+\. \w+\.kt\n```kotlin\n.*?```\n)',
        lambda m: m.group(0),   # keep as-is (already collapsed above)
        md, flags=re.DOTALL
    )

    # 5. Remove empty / whitespace-only section separators
    md = re.sub(r'\n{3,}', '\n\n', md)

    return md

raw_md = preprocess(raw_md)

# ─── Inline markdown ──────────────────────────────────────────────────────────
def inline(text: str) -> str:
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = re.sub(r'\*\*\*(.+?)\*\*\*', r'<strong><em>\1</em></strong>', text)
    text = re.sub(r'\*\*(.+?)\*\*',     r'<strong>\1</strong>',          text)
    text = re.sub(r'\*(.+?)\*',          r'<em>\1</em>',                  text)
    text = re.sub(r'`([^`]+)`',          r'<code>\1</code>',              text)
    return text

# ─── Block markdown → HTML ───────────────────────────────────────────────────
def blocks_to_html(text: str) -> str:
    lines   = text.split("\n")
    out     = []
    in_code = False
    in_ul   = False
    in_ol   = False
    in_tbl  = False
    code_lang = ""
    code_buf  = []
    tbl_buf   = []

    def flush_code():
        nonlocal in_code, code_buf
        raw = "\n".join(code_buf)
        raw = raw.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
        lc  = f' class="language-{code_lang}"' if code_lang else ""
        out.append(f'<pre><code{lc}>{raw}</code></pre>')
        code_buf = []; in_code = False

    def flush_tbl():
        nonlocal in_tbl, tbl_buf
        rows = [r for r in tbl_buf if r is not None]
        if not rows: in_tbl = False; tbl_buf = []; return
        html = "<table>"
        for i, row in enumerate(rows):
            cells = [c.strip() for c in row.strip("|").split("|")]
            tag = "th" if i == 0 else "td"
            html += "<tr>" + "".join(f"<{tag}>{inline(c)}</{tag}>" for c in cells) + "</tr>"
        html += "</table>"
        out.append(html)
        tbl_buf = []; in_tbl = False

    def close_list():
        nonlocal in_ul, in_ol
        if in_ul: out.append("</ul>"); in_ul = False
        if in_ol: out.append("</ol>"); in_ol = False

    for raw in lines:
        # code fence
        if raw.startswith("```"):
            if in_code: flush_code(); continue
            close_list()
            if in_tbl: flush_tbl()
            in_code = True; code_lang = raw[3:].strip(); continue
        if in_code:
            code_buf.append(raw); continue

        # table row
        stripped = raw.strip()
        if "|" in stripped and stripped.startswith("|"):
            if re.match(r'^[\|\s\-:]+$', stripped):
                tbl_buf.append(None); in_tbl = True; continue
            tbl_buf.append(raw); in_tbl = True; continue
        elif in_tbl:
            flush_tbl()

        # headings (h3/h4/h5 only — h1/h2 handled by section splitter)
        m = re.match(r'^(#{3,6})\s+(.*)', raw)
        if m:
            close_list()
            level = len(m.group(1))
            out.append(f'<h{level}>{inline(m.group(2))}</h{level}>')
            continue

        # hr
        if re.match(r'^-{3,}$', stripped):
            close_list(); out.append('<hr>'); continue

        # blockquote
        if raw.startswith("> "):
            close_list()
            out.append(f'<blockquote>{inline(raw[2:])}</blockquote>'); continue

        # ol
        m = re.match(r'^\s*\d+\.\s+(.*)', raw)
        if m:
            if in_ul: out.append("</ul>"); in_ul = False
            if not in_ol: out.append("<ol>"); in_ol = True
            out.append(f"<li>{inline(m.group(1))}</li>"); continue

        # ul
        m = re.match(r'^\s*[\*\-]\s+(.*)', raw)
        if m:
            if in_ol: out.append("</ol>"); in_ol = False
            if not in_ul: out.append("<ul>"); in_ul = True
            out.append(f"<li>{inline(m.group(1))}</li>"); continue

        # blank
        if not stripped:
            close_list()
            if in_tbl: flush_tbl()
            continue

        # paragraph
        close_list()
        out.append(f'<p>{inline(raw)}</p>')

    if in_code:  flush_code()
    if in_tbl:   flush_tbl()
    close_list()
    return "\n".join(out)

# ─── Split markdown into sections by ## headings ─────────────────────────────
# Structure: (title_line, body_text)
def split_sections(md: str):
    # Remove the h1 title block at top
    md = re.sub(r'^#\s+[^\n]+\n', '', md, count=1)
    parts = re.split(r'\n(## [^\n]+)', md)
    sections = []
    # parts[0] = content before first ##
    intro = parts[0].strip()
    # walk pairs
    i = 1
    while i < len(parts) - 1:
        heading = parts[i]      # "## 2. SYSTEM ARCHITECTURE"
        body    = parts[i+1]    # content until next ##
        sections.append((heading.strip(), body))
        i += 2
    return intro, sections

# ─── Section number + title extractor ────────────────────────────────────────
SECTION_ICONS = {
    "EXECUTIVE":    ("01", "Executive Summary",         "📋"),
    "SYSTEM":       ("02", "System Architecture",       "🏗️"),
    "TECH STACK":   ("03", "Tech Stack Deep Dive",      "⚙️"),
    "ANDROID":      ("04", "Android Frontend",          "📱"),
    "BACKEND":      ("05", "Backend Architecture",      "☁️"),
    "API":          ("06", "API Reference",             "🔌"),
    "DATABASE":     ("07", "Database Design",           "🗄️"),
    "AUTHENTICA":   ("08", "Authentication & Security", "🔐"),
    "THIRD-PARTY":  ("09", "Third-Party Integrations",  "🔗"),
    "CORE USER":    ("10", "Core User Flows",           "🔄"),
    "DEVELOPMENT":  ("11", "Development Setup",         "🛠️"),
    "TESTING":      ("12", "Testing Strategy",          "✅"),
    "TECHNICAL CH": ("13", "Technical Challenges",      "⚡"),
    "FUTURE":       ("15", "Future Roadmap",            "🚀"),
    "APPENDIX":     ("A",  "Appendices",                "📎"),
}

def get_section_meta(heading: str):
    upper = heading.upper()
    for key, (num, label, icon) in SECTION_ICONS.items():
        if key in upper:
            return num, label, icon
    # fallback: extract from heading text
    m = re.match(r'##\s+(\d+)\.\s+(.*)', heading)
    if m:
        return m.group(1), m.group(2).title(), "📄"
    return "§", heading.replace("## ", ""), "📄"

# ─── Build section HTML ───────────────────────────────────────────────────────
def render_section(heading: str, body: str, is_first: bool = False) -> str:
    num, label, icon = get_section_meta(heading)
    pb = "" if is_first else 'style="page-break-before:always"'
    body_html = blocks_to_html(body)
    return f"""
<section class="doc-section" {pb}>
  <div class="section-banner">
    <div class="section-banner-left">
      <span class="section-num">{num}</span>
      <div class="section-title-block">
        <span class="section-label">{label}</span>
        <span class="section-heading-raw">{heading.replace("## ","").strip()}</span>
      </div>
    </div>
    <span class="section-icon" aria-hidden="true">{icon}</span>
  </div>
  <div class="section-body">
    {body_html}
  </div>
</section>"""

# ─── Parse & render all sections ─────────────────────────────────────────────
intro_text, sections = split_sections(raw_md)
sections_html = ""
for i, (heading, body) in enumerate(sections):
    sections_html += render_section(heading, body, is_first=(i == 0))

# ─── Full HTML ────────────────────────────────────────────────────────────────
HTML = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>RetailDost — Technical Documentation</title>
<link href="https://fonts.googleapis.com/css2?family=Inter:ital,wght@0,300;0,400;0,500;0,600;0,700;0,800;1,400&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
<style>
/* ══════════════════════════ TOKENS ══════════════════════════ */
:root {{
  --blue-950: #172554;
  --blue-900: #1e3a8a;
  --blue-700: #1d4ed8;
  --blue-600: #2563eb;
  --blue-500: #3b82f6;
  --blue-100: #dbeafe;
  --blue-50:  #eff6ff;

  --slate-900: #0f172a;
  --slate-800: #1e293b;
  --slate-700: #334155;
  --slate-600: #475569;
  --slate-500: #64748b;
  --slate-300: #cbd5e1;
  --slate-200: #e2e8f0;
  --slate-100: #f1f5f9;
  --slate-50:  #f8fafc;

  --green-400: #4ade80;
  --green-600: #16a34a;

  --font-sans: 'Inter', system-ui, -apple-system, sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
}}

/* ══════════════════════════ PAGE ══════════════════════════ */
@page {{
  size: A4;
  margin: 0;
}}
@page :not(:first) {{
  margin: 12mm 14mm 11mm;
}}

*, *::before, *::after {{
  box-sizing: border-box;
  margin: 0; padding: 0;
  -webkit-print-color-adjust: exact;
  print-color-adjust: exact;
}}

html {{ font-size: 8.0pt; }}
body {{
  font-family: var(--font-sans);
  color: var(--slate-800);
  background: #fff;
  line-height: 1.4;
}}

/* ══════════════════════════ COVER ══════════════════════════ */
.cover {{
  width: 210mm;
  height: 297mm;
  background: linear-gradient(150deg, #0c1445 0%, #1a3a8f 45%, #1e57c8 80%, #0ea5e9 100%);
  position: relative; overflow: hidden;
  page-break-after: always;
  display: flex; flex-direction: column; justify-content: space-between;
}}
.cover-grid {{
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.035) 1px, transparent 1px);
  background-size: 22px 22px;
}}
.cover-circle-1 {{
  position: absolute; top: -100px; right: -100px;
  width: 380px; height: 380px; border-radius: 50%;
  background: rgba(255,255,255,0.055);
}}
.cover-circle-2 {{
  position: absolute; bottom: -80px; left: -80px;
  width: 300px; height: 300px; border-radius: 50%;
  background: rgba(255,255,255,0.04);
}}
.cover-circle-3 {{
  position: absolute; top: 60px; right: 60px;
  width: 160px; height: 160px; border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.1);
}}
.cover-top {{
  position: relative; z-index: 2;
  padding: 16mm 15mm 0;
  display: flex; justify-content: space-between; align-items: flex-start;
}}
.cover-badge {{
  display: inline-flex; align-items: center; gap: 6px;
  background: rgba(255,255,255,0.13);
  border: 1px solid rgba(255,255,255,0.22);
  border-radius: 20px; padding: 5px 14px;
  font-size: 7pt; font-weight: 600;
  color: rgba(255,255,255,0.92); letter-spacing: 0.09em;
  text-transform: uppercase;
}}
.cover-badge-dot {{
  width: 7px; height: 7px; border-radius: 50%;
  background: #4ade80; box-shadow: 0 0 8px #4ade80;
}}
.cover-logo-mono {{
  font-size: 14pt; font-weight: 800;
  color: rgba(255,255,255,0.9); letter-spacing: -0.02em;
  text-align: right; line-height: 1;
}}
.cover-logo-mono span {{ color: #93c5fd; }}
.cover-logo-tag {{
  font-size: 6.5pt; color: rgba(255,255,255,0.4);
  letter-spacing: 0.12em; text-transform: uppercase;
  text-align: right; margin-top: 2px;
}}
.cover-mid {{
  position: relative; z-index: 2;
  padding: 0 15mm;
}}
.cover-eyebrow {{
  font-size: 7.5pt; font-weight: 500;
  color: rgba(255,255,255,0.55);
  letter-spacing: 0.14em; text-transform: uppercase; margin-bottom: 10px;
}}
.cover-title {{
  font-size: 52pt; font-weight: 800;
  color: #fff; line-height: 1.0; letter-spacing: -0.03em;
  margin-bottom: 5px;
}}
.cover-title em {{ color: #93c5fd; font-style: normal; }}
.cover-subtitle {{
  font-size: 13pt; font-weight: 300;
  color: rgba(255,255,255,0.7);
  letter-spacing: -0.01em; margin-bottom: 22px;
}}
.cover-rule {{
  width: 44px; height: 3px; border-radius: 2px;
  background: linear-gradient(90deg, #4ade80, #60a5fa);
  margin-bottom: 18px;
}}
.cover-desc {{
  font-size: 9.5pt; color: rgba(255,255,255,0.65);
  line-height: 1.65; max-width: 115mm; margin-bottom: 22px;
}}
.chip-row {{
  display: flex; flex-wrap: wrap; gap: 6px;
}}
.chip {{
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.18);
  border-radius: 4px; padding: 3px 10px;
  font-size: 7pt; font-weight: 500;
  color: rgba(255,255,255,0.88); letter-spacing: 0.04em;
}}
.cover-bottom {{
  position: relative; z-index: 2;
  padding: 0 15mm 13mm;
  display: flex; justify-content: space-between; align-items: flex-end;
}}
.cover-meta {{ display: flex; flex-direction: column; gap: 4px; }}
.meta-row {{
  font-size: 7.5pt; color: rgba(255,255,255,0.5);
  display: flex; gap: 8px;
}}
.meta-row strong {{ color: rgba(255,255,255,0.82); font-weight: 600; }}
.cover-emblem {{
  width: 46px; height: 46px; border-radius: 12px;
  background: rgba(255,255,255,0.12);
  border: 1px solid rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center;
  font-size: 15pt; font-weight: 800; color: #fff;
  letter-spacing: -0.04em;
}}

/* ══════════════════════════ TOC PAGE ══════════════════════════ */
.toc-page {{
  padding: 14mm 15mm 12mm;
  page-break-after: always;
  min-height: 270mm;
}}
.toc-header {{
  display: flex; align-items: baseline; gap: 10px;
  border-bottom: 2px solid var(--slate-200); padding-bottom: 4mm; margin-bottom: 5mm;
}}
.toc-header h1 {{
  font-size: 18pt; font-weight: 800; color: var(--slate-900);
  letter-spacing: -0.025em;
}}
.toc-header span {{
  font-size: 8pt; font-weight: 500; color: var(--slate-500);
  letter-spacing: 0.04em; text-transform: uppercase;
}}

.toc-grid {{
  display: flex; flex-direction: column; gap: 1px;
}}
.toc-item {{
  display: flex; align-items: center; gap: 8px;
  padding: 5px 0;
  border-bottom: 1px solid var(--slate-100);
}}
.toc-item:last-child {{ border-bottom: none; }}
.toc-num {{
  min-width: 26px; height: 20px;
  background: var(--blue-700); color: #fff;
  border-radius: 4px; display: flex; align-items: center; justify-content: center;
  font-size: 7pt; font-weight: 700; letter-spacing: 0.03em;
  flex-shrink: 0;
}}
.toc-num.appendix {{ background: var(--slate-600); }}
.toc-body {{ flex: 1; }}
.toc-title {{
  font-size: 9pt; font-weight: 600; color: var(--slate-800);
  line-height: 1.3;
}}
.toc-sub {{
  font-size: 7.5pt; color: var(--slate-500); margin-top: 1px;
  font-weight: 400;
}}
.toc-dot {{
  flex: 0 0 auto;
  border-bottom: 1px dotted var(--slate-300);
  flex: 1; margin: 0 4px; height: 1px; align-self: center;
  display: none;
}}

/* Metrics strip */
.metrics-strip {{
  display: flex; gap: 5mm; margin: 5mm 0 7mm;
}}
.metric {{
  flex: 1;
  background: var(--slate-50);
  border: 1px solid var(--slate-200);
  border-top: 3px solid var(--blue-600);
  border-radius: 6px; padding: 3.5mm;
}}
.metric-val {{
  font-size: 14pt; font-weight: 800;
  color: var(--blue-900); line-height: 1;
}}
.metric-lbl {{
  font-size: 6.5pt; font-weight: 500;
  color: var(--slate-500); margin-top: 2px;
  text-transform: uppercase; letter-spacing: 0.06em;
}}

/* ══════════════════════════ SECTION BANNER ══════════════════════════ */
.doc-section {{
  padding: 0 14mm 8mm;
}}

.section-banner {{
  display: flex; align-items: center; justify-content: space-between;
  background: linear-gradient(135deg, var(--blue-950) 0%, var(--blue-900) 100%);
  border-radius: 0 0 8px 8px;
  padding: 3mm 4.5mm;
  margin: 0 -14mm 3.5mm;
  position: relative; overflow: hidden;
}}
.section-banner::before {{
  content: '';
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.04) 1px, transparent 1px);
  background-size: 18px 18px;
}}
.section-banner::after {{
  content: '';
  position: absolute; bottom: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg, #4ade80, #3b82f6, transparent);
}}
.section-banner-left {{
  display: flex; align-items: center; gap: 5mm; position: relative; z-index: 1;
}}
.section-num {{
  font-size: 18pt; font-weight: 800;
  color: rgba(255,255,255,0.18); line-height: 1;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.04em;
}}
.section-title-block {{
  display: flex; flex-direction: column; gap: 1px;
}}
.section-label {{
  font-size: 11pt; font-weight: 700;
  color: #fff; letter-spacing: -0.015em; line-height: 1.1;
}}
.section-heading-raw {{
  font-size: 7pt; font-weight: 500;
  color: rgba(255,255,255,0.45); letter-spacing: 0.04em;
  text-transform: uppercase;
}}
.section-icon {{
  font-size: 18pt; position: relative; z-index: 1;
  opacity: 0.5;
}}

/* ══════════════════════════ BODY CONTENT ══════════════════════════ */
.section-body h3 {{
  font-size: 9.0pt; font-weight: 700; color: var(--blue-900);
  margin: 3mm 0 1.2mm;
  padding-left: 2.5mm;
  border-left: 2.5px solid var(--blue-500);
  line-height: 1.2;
  page-break-after: avoid;
}}
.section-body h4 {{
  font-size: 8.0pt; font-weight: 600; color: var(--slate-700);
  margin: 2mm 0 0.8mm;
  page-break-after: avoid;
}}
.section-body h5 {{
  font-size: 7.0pt; font-weight: 700;
  color: var(--slate-500); text-transform: uppercase; letter-spacing: 0.08em;
  margin: 2.5mm 0 1.2mm;
}}

p {{
  font-size: 8.0pt; color: var(--slate-700); line-height: 1.4;
  margin: 0 0 1.4mm;
}}

strong {{ font-weight: 600; color: var(--slate-800); }}
em     {{ font-style: italic; }}

code {{
  font-family: var(--font-mono);
  font-size: 7.0pt;
  background: var(--slate-100); color: #be185d;
  border: 1px solid var(--slate-200);
  border-radius: 3px; padding: 1px 4px;
}}

pre {{
  background: var(--slate-900);
  border-radius: 5px; padding: 2mm 3mm;
  margin: 1mm 0 2mm; overflow: hidden;
  page-break-inside: avoid;
  border-left: 3px solid var(--blue-500);
}}
pre code {{
  font-family: var(--font-mono);
  font-size: 6.2pt; color: #e2e8f0;
  background: none; border: none; padding: 0;
  white-space: pre-wrap; word-break: break-all;
  line-height: 1.4;
}}

/* Tables */
table {{
  width: 100%; border-collapse: collapse;
  margin: 1.5mm 0 3mm;
  page-break-inside: avoid;
  font-size: 7.5pt;
  border-radius: 6px; overflow: hidden;
  border: 1px solid var(--slate-200);
}}
th {{
  background: var(--blue-950); color: #fff;
  padding: 2.5px 6px; font-weight: 600;
  font-size: 6.2pt; letter-spacing: 0.05em; text-transform: uppercase;
  text-align: left;
}}
td {{
  padding: 2.5px 6px; border-bottom: 1px solid var(--slate-100);
  color: var(--slate-700); vertical-align: top; line-height: 1.35;
  font-size: 7.5pt;
}}
tr:last-child td {{ border-bottom: none; }}
tr:nth-child(even) td {{ background: var(--slate-50); }}

/* Lists */
ul, ol {{
  margin: 0.8mm 0 1.5mm; padding-left: 3.5mm;
}}
li {{
  font-size: 8.0pt; color: var(--slate-700);
  margin-bottom: 0.8mm; line-height: 1.4;
}}
li::marker {{ color: var(--blue-600); font-weight: 700; }}

/* Blockquote */
blockquote {{
  background: var(--blue-50); border-left: 3px solid var(--blue-600);
  border-radius: 0 5px 5px 0; padding: 2mm 3mm;
  margin: 1.5mm 0 2mm; color: var(--blue-900);
  font-size: 8.5pt; font-weight: 500;
}}

hr {{
  border: none; border-top: 1px solid var(--slate-200);
  margin: 3mm 0;
}}

/* State flow line */
.flow-line {{
  display: flex; flex-wrap: wrap; align-items: center; gap: 2mm;
  background: var(--slate-50); border: 1px solid var(--slate-200);
  border-radius: 6px; padding: 2mm 3mm; margin: 1.5mm 0 2mm;
  font-size: 7.0pt;
}}
.flow-step {{
  background: var(--blue-700); color: #fff;
  border-radius: 3px; padding: 1.5px 6px;
  font-weight: 600; white-space: nowrap; font-size: 6.5pt;
}}
.flow-arr {{ color: var(--slate-400); font-size: 8pt; }}

/* ══════════════════════════ DOC FOOTER ══════════════════════════ */
.doc-footer {{
  margin: 6mm 14mm 0; padding: 2.5mm 0 0;
  border-top: 1px solid var(--slate-200);
  display: flex; justify-content: space-between;
  font-size: 6.5pt; color: var(--slate-400);
}}
.footer-brand {{ font-weight: 700; color: var(--blue-600); }}

</style>
</head>
<body>

<!-- ════════════════════ COVER ════════════════════ -->
<div class="cover">
  <div class="cover-grid"></div>
  <div class="cover-circle-1"></div>
  <div class="cover-circle-2"></div>
  <div class="cover-circle-3"></div>

  <div class="cover-top">
    <div class="cover-badge">
      <span class="cover-badge-dot"></span>
      Startup Competition Submission &middot; 2025&ndash;26
    </div>
    <div>
      <div class="cover-logo-mono">Retail<span>Dost</span></div>
      <div class="cover-logo-tag">Technical Documentation</div>
    </div>
  </div>

  <div class="cover-mid">
    <div class="cover-eyebrow">Technical Documentation &mdash; v1.0</div>
    <div class="cover-title">Retail<em>Dost</em></div>
    <div class="cover-subtitle">Intelligent Kirana Store Management Platform</div>
    <div class="cover-rule"></div>
    <div class="cover-desc">
      An <strong style="color:rgba(255,255,255,0.85)">offline-first Android application</strong> empowering
      India&rsquo;s micro-merchants with AI-powered inventory OCR, Hinglish voice ledger,
      and real-time cloud sync &mdash; built on Kotlin, Jetpack Compose, Supabase PostgreSQL,
      and Google Gemini Vision AI.
    </div>
    <div class="chip-row">
      <span class="chip">Kotlin + Jetpack Compose</span>
      <span class="chip">Supabase PostgreSQL</span>
      <span class="chip">Google Gemini AI</span>
      <span class="chip">MVVM + Clean Architecture</span>
      <span class="chip">Offline-First Room DB</span>
      <span class="chip">WorkManager Background Sync</span>
    </div>
  </div>

  <div class="cover-bottom">
    <div class="cover-meta">
      <div class="meta-row"><strong>Platform</strong> Android API 31&ndash;34</div>
      <div class="meta-row"><strong>Language</strong> Kotlin 2.0 + Coroutines</div>
      <div class="meta-row"><strong>Architecture</strong> MVVM &middot; Clean &middot; Package-by-Feature</div>
      <div class="meta-row"><strong>Repository</strong> github.com/ctanmay334/retaildost</div>
      <div class="meta-row"><strong>Version</strong> 1.0 &middot; June 2026</div>
    </div>
    <div class="cover-emblem">RD</div>
  </div>
</div>

<!-- ════════════════════ TOC ════════════════════ -->
<div class="toc-page">
  <div class="toc-header">
    <h1>Table of Contents</h1>
    <span>RetailDost Technical Documentation</span>
  </div>

  <div class="metrics-strip">
    <div class="metric"><div class="metric-val">18</div><div class="metric-lbl">Compose Screens</div></div>
    <div class="metric"><div class="metric-val">12</div><div class="metric-lbl">Room DB Tables</div></div>
    <div class="metric"><div class="metric-val">8</div><div class="metric-lbl">DAO Classes</div></div>
    <div class="metric"><div class="metric-val">Gemini</div><div class="metric-lbl">AI Vision + NLP</div></div>
    <div class="metric"><div class="metric-val">28</div><div class="metric-lbl">Total Pages</div></div>
  </div>

  <div class="toc-grid">
    <div class="toc-item">
      <span class="toc-num">01</span>
      <div class="toc-body">
        <div class="toc-title">Executive Summary</div>
        <div class="toc-sub">Problem statement &middot; Core features &middot; Architecture principles</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num">02</span>
      <div class="toc-body">
        <div class="toc-title">System Architecture</div>
        <div class="toc-sub">High-level diagram &middot; Layer responsibilities &middot; End-to-end data flow &middot; Technology decision table</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num">03</span>
      <div class="toc-body">
        <div class="toc-title">Tech Stack Deep Dive</div>
        <div class="toc-sub">Kotlin + Coroutines &middot; Hilt DI &middot; Supabase SDK &middot; Room &middot; Coil &middot; MLKit &middot; WorkManager</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num">04</span>
      <div class="toc-body">
        <div class="toc-title">Android Frontend — Jetpack Compose</div>
        <div class="toc-sub">MVVM pattern &middot; Package structure &middot; Navigation &middot; 18 screens table &middot; UiState pattern &middot; Performance optimizations</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num">05</span>
      <div class="toc-body">
        <div class="toc-title">Backend Architecture</div>
        <div class="toc-sub">Supabase PostgreSQL &middot; Error wrappers &middot; Ktor client configuration</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num">06</span>
      <div class="toc-body">
        <div class="toc-title">API Reference (Postgrest Endpoints)</div>
        <div class="toc-sub">Profiles &middot; Khata customers &middot; Khata transactions &middot; Inventory &mdash; request/response schemas</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num">07</span>
      <div class="toc-body">
        <div class="toc-title">Database Design</div>
        <div class="toc-sub">12 Room tables &middot; 8 DAO classes &middot; Entity relationships &middot; Indexing strategy</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num">08</span>
      <div class="toc-body">
        <div class="toc-title">Authentication &amp; Security</div>
        <div class="toc-sub">Auth flow &middot; AES256-SIV/GCM token storage &middot; Network security &middot; ProGuard/R8 &middot; PII privacy</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num">09</span>
      <div class="toc-body">
        <div class="toc-title">Third-Party Integrations</div>
        <div class="toc-sub">Google Gemini Vision OCR &middot; WhatsApp reminders &middot; Android Print Framework</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num">10</span>
      <div class="toc-body">
        <div class="toc-title">Core User Flows — Technical Walkthrough</div>
        <div class="toc-sub">Hinglish voice ledger &middot; Invoice OCR auto-restock &middot; Credit settlement</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num">11</span>
      <div class="toc-body">
        <div class="toc-title">Development Setup Guide</div>
        <div class="toc-sub">Prerequisites &middot; Local setup &middot; Environment config &middot; Build commands &middot; Variants</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num">12</span>
      <div class="toc-body">
        <div class="toc-title">Testing Strategy</div>
        <div class="toc-sub">Unit tests &middot; Roborazzi screenshot tests &middot; Coverage summary</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num">15</span>
      <div class="toc-body">
        <div class="toc-title">Future Roadmap</div>
        <div class="toc-sub">Immediate &middot; Short-term &middot; Mid-term &middot; Long-term vision</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num appendix">A</span>
      <div class="toc-body">
        <div class="toc-title">Appendix A — Dependencies Table</div>
        <div class="toc-sub">Full library versions and roles</div>
      </div>
    </div>
    <div class="toc-item">
      <span class="toc-num appendix">B</span>
      <div class="toc-body">
        <div class="toc-title">Appendix B — Manifest Permissions</div>
        <div class="toc-sub">Android permission declarations with justifications</div>
      </div>
    </div>
  </div>
</div>

<!-- ════════════════════ SECTIONS ════════════════════ -->
{sections_html}

<div class="doc-footer">
  <span>RetailDost &mdash; Confidential &middot; Competition Submission &middot; June 2026</span>
  <span class="footer-brand">RetailDost</span>
</div>

</body>
</html>"""

# ─── Write temp HTML ──────────────────────────────────────────────────────────
tmp_html = os.path.join(tempfile.gettempdir(), "retaildost_premium_v2.html")
with open(tmp_html, "w", encoding="utf-8") as f:
    f.write(HTML)
print(f"✓ HTML written → {tmp_html}")

# ─── Chrome PDF render ────────────────────────────────────────────────────────
args = [
    CHROME,
    "--headless=new",
    "--disable-gpu",
    "--no-sandbox",
    "--disable-dev-shm-usage",
    "--run-all-compositor-stages-before-draw",
    "--disable-extensions",
    "--virtual-time-budget=9000",
    f"--print-to-pdf={OUT_PATH}",
    "--print-to-pdf-no-header",
    "--no-pdf-header-footer",
    f"file://{tmp_html}",
]
print("⏳ Rendering with headless Chrome …")
r = subprocess.run(args, capture_output=True, text=True, timeout=120)
if r.returncode != 0:
    print("✗ Chrome error:", r.stderr[:600])
    sys.exit(1)
if not os.path.exists(OUT_PATH) or os.path.getsize(OUT_PATH) < 5000:
    print("✗ PDF not created or too small.")
    sys.exit(1)

kb = os.path.getsize(OUT_PATH) / 1024
print(f"✓ PDF ready ({kb:.0f} KB) → {OUT_PATH}")
