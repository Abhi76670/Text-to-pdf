import '@fontsource/roboto/latin-400.css'; import '@fontsource/roboto/latin-700.css';
import '@fontsource/tinos/latin-400.css'; import '@fontsource/tinos/latin-700.css';
import '@fontsource/gelasio/latin-400.css'; import '@fontsource/gelasio/latin-700.css';
import '@fontsource/open-sans/latin-400.css'; import '@fontsource/open-sans/latin-700.css';
import '@fontsource/lato/latin-400.css'; import '@fontsource/lato/latin-700.css';
import '@fontsource/jetbrains-mono/latin-400.css'; import '@fontsource/jetbrains-mono/latin-700.css';
import '@fontsource/fira-code/latin-400.css'; import '@fontsource/fira-code/latin-700.css';
import '@fontsource/cousine/latin-400.css'; import '@fontsource/cousine/latin-700.css';
import '@fontsource/source-code-pro/latin-400.css'; import '@fontsource/source-code-pro/latin-700.css';
import '@fontsource/dancing-script/latin-400.css'; import '@fontsource/dancing-script/latin-700.css';
import '@fontsource/playfair-display/latin-400.css'; import '@fontsource/playfair-display/latin-700.css';
import '@fontsource/pacifico/latin-400.css';
import './style.css';
const w = window as any;
const A = w.Android as undefined | { savePdf(h: string, n: string): void; openFile(u: string): void; shareFile(u: string): void };
const $ = (s: string) => document.querySelector(s) as HTMLElement;
const $$ = (s: string) => Array.from(document.querySelectorAll<HTMLElement>(s));
const ed = $('#ed'), all = $('#all') as HTMLInputElement, fl = $('#fl');
const FONTS: [string, string, string][] = [['Documents','Roboto','Roboto'],['Documents','Times New Roman','Tinos'],['Documents','Georgia','Gelasio'],['Documents','Open Sans','Open Sans'],['Documents','Lato','Lato'],['Coding','JetBrains Mono','JetBrains Mono'],['Coding','Fira Code','Fira Code'],['Coding','Courier New','Cousine'],['Coding','Source Code Pro','Source Code Pro'],['Stylish','Pacifico','Pacifico'],['Stylish','Playfair Display','Playfair Display'],['Stylish','Dancing Script','Dancing Script']];
let selOnly = false;
let cur = 'home', size = 12, last: Range | null = null;
const ls = (k: string, d: any) => { try { return JSON.parse(localStorage.getItem(k) || '') ?? d; } catch { return d; } };

function show(n: string) { cur = n; $$('.scr').forEach(s => s.classList.toggle('on', s.id === n)); fl.classList.remove('on'); if (n === 'pdfs') renderPdfs(); }
$$('[data-go]').forEach(b => b.onclick = () => show(b.dataset.go!));
w.__back = () => { if (cur === 'home') return false; show(cur === 'pv' ? 'editor' : 'home'); return true; };

function toast(msg: string, acts: [string, () => void][] = []) {
  const t = $('#toast'); t.innerHTML = ''; const s = document.createElement('span'); s.textContent = msg; t.append(s);
  acts.forEach(([l, f]) => { const b = document.createElement('button'); b.textContent = l; b.onclick = () => { f(); t.classList.remove('on'); }; t.append(b); });
  t.classList.add('on'); setTimeout(() => t.classList.remove('on'), 7000);
}
const save = () => {
  localStorage.setItem('draft', ed.innerHTML); const t = ed.innerText.trim();
  $('#cnt').textContent = `${t ? t.split(/\s+/).length : 0} words, ${t.length} characters`;
};
function run(fn: () => void) {
  ed.focus(); const s = getSelection()!;
  if (last) { s.removeAllRanges(); s.addRange(last); }
  const whole = (all.checked && !selOnly) || s.isCollapsed;
  if (whole) { const r = document.createRange(); r.selectNodeContents(ed); s.removeAllRanges(); s.addRange(r); }
  document.execCommand('styleWithCSS', false, 'true'); fn();
  if (whole) s.collapseToEnd();
  save(); sync(); setTimeout(sync, 60);
}
const cmd = (c: string, v?: string) => document.execCommand(c, false, v);
function sync() {
  $$('[data-c]').forEach(b => { try { b.classList.toggle('on', document.queryCommandState(b.dataset.c!)); } catch {} });
  const f = document.queryCommandValue('fontName').replace(/["']/g, '');
  const m = FONTS.find(x => x[2] === f); if (m) $('#fbtn').textContent = m[1] + ' ▾';
}
document.addEventListener('selectionchange', () => {
  const s = getSelection()!;
  if (s.rangeCount && ed.contains(s.anchorNode)) { last = s.getRangeAt(0).cloneRange(); sync(); $('#selbar').classList.toggle('on', !s.isCollapsed); }
});
$('#tb').addEventListener('mousedown', e => { if ((e.target as HTMLElement).tagName !== 'INPUT') e.preventDefault(); });
$$('[data-c]').forEach(b => b.onclick = () => run(() => cmd(b.dataset.c!)));
$$('[data-u]').forEach(b => b.onclick = () => { ed.focus(); cmd(b.dataset.u!); save(); });
const bars = (ws: number[], x: (v: number) => number) => `<svg viewBox="0 0 24 24" width="22" height="22" stroke="currentColor" stroke-width="2" stroke-linecap="round">${ws.map((v, i) => `<path d="M${x(v)} ${5 + i * 5}h${v}"/>`).join('')}</svg>`;
const W = [18, 11, 18, 8];
$$('[data-a]').forEach(b => { const a = b.dataset.a; b.innerHTML = bars(a === 'j' ? [18, 18, 18, 18] : W, v => a === 'c' ? (24 - v) / 2 : a === 'r' ? 21 - v : 3); });
($('#fc') as HTMLInputElement).oninput = e => run(() => cmd('foreColor', (e.target as HTMLInputElement).value));
($('#hc') as HTMLInputElement).oninput = e => run(() => cmd('hiliteColor', (e.target as HTMLInputElement).value));

let g = '';
FONTS.forEach(([grp, name, f]) => {
  if (grp !== g) { g = grp; fl.insertAdjacentHTML('beforeend', `<h4>${g}</h4>`); }
  const b = document.createElement('button'); b.textContent = name; b.style.fontFamily = `"${f}"`;
  b.onclick = () => { fl.classList.remove('on'); $('#fbtn').textContent = name + ' ▾'; run(() => cmd('fontName', f)); };
  fl.append(b);
});
$('#fbtn').onclick = () => fl.classList.toggle('on');
function setSize(n: number) {
  size = Math.max(8, Math.min(72, n)); $('#sz').textContent = String(size);
  run(() => { cmd('fontSize', '7'); ed.querySelectorAll<HTMLElement>('[style*="xxx-large"],font[size="7"]').forEach(e => { e.removeAttribute('size'); e.style.fontSize = size + 'pt'; }); });
}
$('#sm').onclick = () => setSize(size - 1); $('#sp').onclick = () => setSize(size + 1);

$('#pvb').onclick = () => {
  const p = $('#page'); p.innerHTML = ed.innerHTML; show('pv');
  const k = (innerWidth - 24) / 794; p.style.transform = `scale(${k})`; $('#pvw').style.height = p.offsetHeight * k + 'px';
};
function buildHtml() {
  const css = (document.querySelector('link[rel=stylesheet]') as HTMLLinkElement | null)?.href || '';
  return `<!doctype html><html><head><meta charset="utf-8"><link rel="stylesheet" href="${css}"><style>body{margin:0;background:#fff;color:#111;font:12pt/1.4 Roboto,sans-serif;word-wrap:break-word}ul,ol{margin:0;padding-left:1.4em}</style></head><body>${ed.innerHTML}<script>document.fonts.ready.then(function(){setTimeout(function(){P.ready()},400)})</script></body></html>`;
}
const fn = $('#fn') as HTMLInputElement;
$('#dl').onclick = () => { fn.value = 'Document-' + new Date().toISOString().slice(0, 10); $('#dlg').classList.add('on'); };
$('#cx').onclick = () => $('#dlg').classList.remove('on');
$('#ok').onclick = () => {
  $('#dlg').classList.remove('on');
  if (!A) return toast('Open this app on Android to save PDFs.');
  toast('Creating PDF...'); A.savePdf(buildHtml(), fn.value.trim() || 'document');
};
let pdfs: { uri: string; name: string }[] = ls('pdfs', []);
w.__saved = (uri: string, name: string) => {
  pdfs.unshift({ uri, name }); localStorage.setItem('pdfs', JSON.stringify(pdfs));
  toast(`Saved to Downloads: ${name}.pdf`, [['Open', () => A!.openFile(uri)], ['Share', () => A!.shareFile(uri)]]);
};
w.__err = (m: string) => toast('Could not save PDF: ' + m);
function renderPdfs() {
  const l = $('#list'); l.innerHTML = pdfs.length ? '' : '<p style="color:var(--mut)">No PDFs yet. Tap Create PDF to make your first one.</p>';
  pdfs.forEach(p => {
    const r = document.createElement('div'); r.className = 'row item'; r.innerHTML = '<b></b><button>Open</button><button>Share</button>';
    r.querySelector('b')!.textContent = p.name + '.pdf'; const [o, s] = Array.from(r.querySelectorAll('button'));
    o.onclick = () => A?.openFile(p.uri); s.onclick = () => A?.shareFile(p.uri); l.append(r);
  });
}
const dm = $('#dm') as HTMLInputElement; dm.checked = ls('dark', false);
const applyDark = () => { document.body.classList.toggle('dark', dm.checked); localStorage.setItem('dark', JSON.stringify(dm.checked)); };
dm.onchange = applyDark; applyDark();
$('#clr').onclick = () => { ed.innerHTML = ''; save(); toast('Draft cleared'); };
ed.innerHTML = localStorage.getItem('draft') || ''; ed.oninput = save; save();

// ---- Code style: monospace + syntax colors on the selection (or whole text) ----
const esc = (s: string) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const KW = 'const|let|var|function|return|if|else|for|while|class|import|from|export|default|new|this|def|fun|val|public|private|static|void|int|float|string|bool|boolean|true|false|null|undefined|try|catch|finally|await|async|in|of|switch|case|break|continue|throw|extends|package|interface|type|enum|struct|self|None|True|False|and|or|not|print|echo|elif|lambda|with|as|is|do|end|select|where|insert|update|delete|create|table';
const TOK = new RegExp(
  /(\/\/[^\n]*|#[^\n]*|\/\*[\s\S]*?\*\/)|("(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'|`(?:\\.|[^`\\])*`)|\b(\d+(?:\.\d+)?)\b/.source +
  '|\\b(' + KW + ')\\b|' +
  /\b([A-Za-z_]\w*)(?=\()|([{}()\[\];,.<>=+\-*\/%!&|:?]+)/.source, 'g');
const COLORS = ['', '#8b98a9', '#15803d', '#ea580c', '#7c3aed', '#2563eb', '#db2777'];
function highlight(code: string) {
  let out = '', idx = 0, m: RegExpExecArray | null; TOK.lastIndex = 0;
  while ((m = TOK.exec(code))) {
    out += esc(code.slice(idx, m.index));
    let k = 1; while (k < 6 && m[k] === undefined) k++;
    const st = k === 1 ? ';font-style:italic' : k === 4 ? ';font-weight:700' : '';
    out += `<span style="color:${COLORS[k]}${st}">${esc(m[0])}</span>`; idx = m.index + m[0].length;
    if (!m[0].length) TOK.lastIndex++;
  }
  return out + esc(code.slice(idx));
}
$('#code').onclick = () => {
  ed.focus(); const s = getSelection()!;
  if (last) { s.removeAllRanges(); s.addRange(last); }
  if ((all.checked && !selOnly) || s.isCollapsed) { const r = document.createRange(); r.selectNodeContents(ed); s.removeAllRanges(); s.addRange(r); }
  const code = s.toString(); if (!code.trim()) return toast('Type or paste some code first.');
  document.execCommand('insertHTML', false, `<span style="font-family:'JetBrains Mono';font-size:${size}pt;white-space:pre-wrap">${highlight(code)}</span>`);
  save(); toast('Code style applied');
};

// ---- Home: side menu and three-dots menu ----
const drawer = $('#drawer'), pop = $('#pop');
$('#menu').onclick = () => drawer.classList.add('on');
$('#more').onclick = e => { e.stopPropagation(); pop.classList.toggle('on'); };
drawer.onclick = () => drawer.classList.remove('on');
document.addEventListener('click', () => pop.classList.remove('on'));
$('#pdark').onclick = () => { dm.checked = !dm.checked; applyDark(); };
$('#pclr').onclick = () => { ed.innerHTML = ''; save(); toast('Draft cleared'); };
$$('.about').forEach(b => b.onclick = () => toast('Text to PDF Maker 1.0. Write, style and save PDFs offline.'));

// ---- Floating "Apply on selected text" bar ----
const sb = $('#selbar');
sb.addEventListener('pointerdown', () => { selOnly = true; });
$('#tb').addEventListener('pointerdown', () => { selOnly = false; });
sb.addEventListener('mousedown', e => { if ((e.target as HTMLElement).tagName !== 'INPUT') e.preventDefault(); });
$('#scode').onclick = () => $('#code').click();
$('#sfont').onclick = () => fl.classList.toggle('on');
($('#sfc') as HTMLInputElement).oninput = e => run(() => cmd('foreColor', (e.target as HTMLInputElement).value));
