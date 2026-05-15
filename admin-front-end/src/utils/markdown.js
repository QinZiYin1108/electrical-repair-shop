function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function sanitizeUrl(url) {
  const text = String(url || '').trim();
  if (/^(https?:|mailto:|tel:)/i.test(text)) {
    return escapeHtml(text);
  }
  return '#';
}

function parseInline(text) {
  let html = escapeHtml(text);

  html = html.replace(/`([^`]+)`/g, (_, code) => {
    return `<code style="padding: 2px 6px; border-radius: 6px; background: #f3f6fb; color: #b42318; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 0.92em;">${code}</code>`;
  });

  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_, label, url) => {
    return `<a href="${sanitizeUrl(url)}" style="color: #1677ff; text-decoration: none;">${label}</a>`;
  });

  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/__([^_]+)__/g, '<strong>$1</strong>');
  html = html.replace(/~~([^~]+)~~/g, '<del>$1</del>');

  return html;
}

function renderParagraph(lines) {
  return `<p style="margin: 0 0 16px; color: #243c4e; font-size: 15px; line-height: 1.9;">${lines.map(parseInline).join('<br/>')}</p>`;
}

function renderHeading(level, text) {
  const sizeMap = {
    1: '32px',
    2: '26px',
    3: '22px',
    4: '18px',
    5: '16px',
    6: '15px'
  };
  return `<h${level} style="margin: ${level <= 2 ? '28px' : '22px'} 0 14px; color: #173247; font-size: ${sizeMap[level] || '15px'}; line-height: 1.35; font-weight: 700;">${parseInline(text)}</h${level}>`;
}

function renderList(items, ordered) {
  const tag = ordered ? 'ol' : 'ul';
  const start = ordered ? 'padding-left: 24px;' : 'padding-left: 22px;';
  return `<${tag} style="margin: 0 0 16px; ${start} color: #243c4e; line-height: 1.9;">${items.map(item => `<li style="margin: 8px 0;">${parseInline(item)}</li>`).join('')}</${tag}>`;
}

function renderBlockquote(lines) {
  return `<blockquote style="margin: 0 0 16px; padding: 10px 14px; border-left: 4px solid #9fc5ff; background: #f6faff; color: #4a6072;">${lines.map(parseInline).join('<br/>')}</blockquote>`;
}

function renderCodeBlock(lines) {
  return `<pre style="margin: 0 0 16px; padding: 14px 16px; border-radius: 12px; background: #0f172a; color: #e2e8f0; overflow-x: auto; font-size: 13px; line-height: 1.7;"><code>${escapeHtml(lines.join('\n'))}</code></pre>`;
}

function renderTable(lines) {
  if (lines.length < 2) {
    return renderParagraph(lines);
  }
  const headerCells = lines[0].trim().replace(/^\||\|$/g, '').split('|').map(item => item.trim());
  const alignLine = lines[1].trim();
  if (!/^\|?\s*:?-+:?\s*(\|\s*:?-+:?\s*)+\|?$/.test(alignLine)) {
    return renderParagraph(lines);
  }
  const bodyLines = lines.slice(2);
  const thead = `<tr>${headerCells.map(cell => `<th style="padding: 10px 12px; border: 1px solid #d8e3ee; background: #f5f8fb; text-align: left; color: #173247;">${parseInline(cell)}</th>`).join('')}</tr>`;
  const tbody = bodyLines.map(line => {
    const cells = line.trim().replace(/^\||\|$/g, '').split('|').map(item => item.trim());
    return `<tr>${headerCells.map((_, index) => `<td style="padding: 10px 12px; border: 1px solid #d8e3ee; color: #243c4e; vertical-align: top;">${parseInline(cells[index] || '')}</td>`).join('')}</tr>`;
  }).join('');
  return `<div style="margin: 0 0 16px; overflow-x: auto;"><table style="width: 100%; border-collapse: collapse; font-size: 14px; line-height: 1.8;"><thead>${thead}</thead><tbody>${tbody}</tbody></table></div>`;
}

export function markdownToHtml(markdown) {
  const source = String(markdown || '').replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  const lines = source.split('\n');
  const blocks = [];

  let paragraph = [];
  let listType = '';
  let listItems = [];
  let quoteLines = [];
  let codeLines = [];
  let inCodeBlock = false;
  let tableLines = [];

  function flushParagraph() {
    if (!paragraph.length) return;
    blocks.push(renderParagraph(paragraph));
    paragraph = [];
  }

  function flushList() {
    if (!listItems.length) return;
    blocks.push(renderList(listItems, listType === 'ol'));
    listType = '';
    listItems = [];
  }

  function flushQuote() {
    if (!quoteLines.length) return;
    blocks.push(renderBlockquote(quoteLines));
    quoteLines = [];
  }

  function flushCode() {
    if (!codeLines.length) return;
    blocks.push(renderCodeBlock(codeLines));
    codeLines = [];
  }

  function flushTable() {
    if (!tableLines.length) return;
    blocks.push(renderTable(tableLines));
    tableLines = [];
  }

  function flushAll() {
    flushParagraph();
    flushList();
    flushQuote();
    flushTable();
  }

  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i];
    const trimmed = line.trim();

    if (inCodeBlock) {
      if (/^```/.test(trimmed)) {
        flushCode();
        inCodeBlock = false;
      } else {
        codeLines.push(line);
      }
      continue;
    }

    if (/^```/.test(trimmed)) {
      flushAll();
      inCodeBlock = true;
      codeLines = [];
      continue;
    }

    if (!trimmed) {
      flushAll();
      continue;
    }

    if (/^\|.*\|$/.test(trimmed)) {
      flushParagraph();
      flushList();
      flushQuote();
      tableLines.push(line);
      continue;
    }

    flushTable();

    const headingMatch = trimmed.match(/^(#{1,6})\s+(.*)$/);
    if (headingMatch) {
      flushAll();
      blocks.push(renderHeading(headingMatch[1].length, headingMatch[2].trim()));
      continue;
    }

    if (/^(-{3,}|\*{3,}|_{3,})$/.test(trimmed)) {
      flushAll();
      blocks.push('<hr style="margin: 24px 0; border: none; border-top: 1px solid #d9e2ec;"/>');
      continue;
    }

    const quoteMatch = line.match(/^>\s?(.*)$/);
    if (quoteMatch) {
      flushParagraph();
      flushList();
      quoteLines.push(quoteMatch[1]);
      continue;
    }
    flushQuote();

    const orderedMatch = line.match(/^\s*\d+\.\s+(.*)$/);
    if (orderedMatch) {
      flushParagraph();
      if (listType && listType !== 'ol') {
        flushList();
      }
      listType = 'ol';
      listItems.push(orderedMatch[1]);
      continue;
    }

    const unorderedMatch = line.match(/^\s*[-*+]\s+(.*)$/);
    if (unorderedMatch) {
      flushParagraph();
      if (listType && listType !== 'ul') {
        flushList();
      }
      listType = 'ul';
      listItems.push(unorderedMatch[1]);
      continue;
    }

    flushList();
    paragraph.push(line);
  }

  if (inCodeBlock) {
    flushCode();
  }
  flushAll();

  return blocks.join('') || '<p style="margin: 0; color: #6b7280; line-height: 1.8;">暂无内容</p>';
}
