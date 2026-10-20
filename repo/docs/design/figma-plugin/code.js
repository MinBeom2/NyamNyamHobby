// 냠냠취미 디자인 컨셉북 v1.0 — 남은 화면(01·02, 06~09, C-2) 적용 플러그인
// Figma 데스크톱 › 플러그인 › 개발 › 매니페스트에서 플러그인 가져오기 → manifest.json 선택 → 실행
const LOGS = [];
const log = s => LOGS.push(s);
figma.showUI("<html><body style=\"font:12px/1.55 -apple-system,'Segoe UI','Malgun Gothic',sans-serif;margin:14px;color:#1B2A31;background:#FFFBF2\">\n<b style=\"font-size:14px\">냠냠취미 · 디자인 컨셉북 적용</b>\n<pre id=\"l\" style=\"white-space:pre-wrap;background:#fff;border:1px solid #E8DFCC;border-radius:12px;padding:10px;margin:10px 0;max-height:300px;overflow:auto\">적용하는 중이에요… (1분 정도 걸릴 수 있어요)</pre>\n<button id=\"c\" style=\"height:36px;padding:0 16px;border:0;border-radius:12px;background:#007A86;color:#fff;font-weight:600;cursor:pointer\">닫기</button>\n<script>onmessage=e=>{if(e.data&&e.data.pluginMessage)document.getElementById('l').textContent=e.data.pluginMessage};document.getElementById('c').onclick=()=>parent.postMessage({pluginMessage:'close'},'*')</script>\n</body></html>", { width: 460, height: 440, title: '냠냠취미 컨셉북 적용' });
figma.ui.onmessage = m => { if (m === 'close') figma.closePlugin(); };
function __AL(dir) { const f = figma.createFrame(); f.layoutMode = dir; f.primaryAxisSizingMode = 'AUTO'; f.counterAxisSizingMode = 'AUTO'; f.fills = []; f.clipsContent = false; return f; }
(async () => {
  try {
    let __PAGE = figma.currentPage;
    const hasScreens = p => p.children.some(n => n.type === 'SECTION' && n.name.startsWith('06'));
    if (!hasScreens(__PAGE)) {
      for (const p of figma.root.children) { await p.loadAsync(); if (hasScreens(p)) { __PAGE = p; break; } }
      if (!hasScreens(__PAGE)) throw new Error('와이어프레임 페이지(섹션 00~10, C)를 찾지 못했어요. 냠냠취미 Figma 파일에서 실행해 주세요.');
      await figma.setCurrentPageAsync(__PAGE);
    }
    const PAGE = __PAGE;
    const hex = h => ({ r: parseInt(h.slice(1, 3), 16) / 255, g: parseInt(h.slice(3, 5), 16) / 255, b: parseInt(h.slice(5, 7), 16) / 255 });
    const _col = (await figma.variables.getLocalVariableCollectionsAsync())[0]; const _mode = _col.modes[0].modeId;
    const V = {}; for (const v of await figma.variables.getLocalVariablesAsync('COLOR')) V[v.name] = v;
    const CA = { ink: 'text/primary', 'text-2': 'text/secondary', 'text-3': 'text/disabled', line: 'line/default', surface: 'bg/rice', placeholder: 'image/placeholder', primary: 'primary/main', 'primary-tint': 'primary/subtle', dark: 'bg/inverse', white: 'bg/surface', warn: 'status/warning', 'warn-tint': 'status/warning-subtle', danger: 'status/error', overlay: 'bg/overlay', page: 'bg/page', rice: 'bg/rice', spark: 'brand/spark', peach: 'brand/peach', mint: 'brand/mint', teal: 'brand/teal', success: 'status/success', 'success-tint': 'status/success-subtle', ptext: 'primary/text' };
    const vk = k => V[k] ? k : CA[k];
    const paint = (k, opacity) => { const v = V[vk(k)]; let c = v.valuesByMode[_mode]; const p = figma.variables.setBoundVariableForPaint({ type: 'SOLID', color: { r: c.r, g: c.g, b: c.b } }, 'color', v); return opacity != null ? { ...p, opacity } : p; };
    const tss = await figma.getLocalTextStylesAsync(); const TS = {}; for (const s of tss) TS[s.name] = s;
    const SA = { 'Title/XL': 'display/l', 'Title/L': 'h1', 'Title/M': 'h2', 'Title/S': 'h3', 'Body/L': 'body/l', 'Body/M': 'body/m', 'Body/S': 'body/m', 'Caption': 'caption', 'Label/M': 'label', 'Label/S': 'label', 'Label/XS': 'label/s', 'Number/L': 'number/l', 'Display/L': 'display/l', 'Display/M': 'display/m' };
    const sk = s => TS[s] ? s : SA[s];
    await Promise.all([...new Set(tss.map(s => JSON.stringify(s.fontName)))].map(f => figma.loadFontAsync(JSON.parse(f))));
    const C = {}; for (const c of PAGE.findAllWithCriteria({ types: ['COMPONENT', 'COMPONENT_SET'] })) { if (c.type === 'COMPONENT' && c.parent.type === 'COMPONENT_SET') continue; C[c.name] = c; }
    const pk = (owner, name) => Object.keys(owner.componentPropertyDefinitions).find(k => k.split('#')[0] === name);
    const SHAPES = ['VECTOR', 'ELLIPSE', 'RECTANGLE', 'POLYGON', 'STAR', 'BOOLEAN_OPERATION'];
    async function loadFontsOf(node) { const fs = new Set(); for (const t of node.findAll(n => n.type === 'TEXT')) { if (t.fontName !== figma.mixed) fs.add(JSON.stringify(t.fontName)); else for (const s of t.getStyledTextSegments(['fontName'])) fs.add(JSON.stringify(s.fontName)); } await Promise.all([...fs].map(f => figma.loadFontAsync(JSON.parse(f)))); }
    async function T(chars, style = 'body/m', color = 'ink', o = {}) {
      const t = figma.createText(); await t.setTextStyleIdAsync(TS[sk(style)].id); t.characters = chars; t.fills = [paint(color)];
      if (o.w) { t.resize(o.w, t.height); t.textAutoResize = 'HEIGHT'; }
      if (o.align) t.textAlignHorizontal = o.align;
      t.name = o.name || chars.slice(0, 24);
      return t;
    }
    function AL(dir = 'HORIZONTAL', o = {}) {
      const f = __AL(dir);
      f.name = o.name || (dir === 'HORIZONTAL' ? 'Row' : 'Col');
      f.fills = o.fill ? [paint(o.fill, o.op)] : [];
      if (o.gap != null) f.itemSpacing = o.gap;
      if (o.p != null) { const a = Array.isArray(o.p) ? (o.p.length === 2 ? [o.p[0], o.p[1], o.p[0], o.p[1]] : o.p) : [o.p, o.p, o.p, o.p]; f.paddingTop = a[0]; f.paddingRight = a[1]; f.paddingBottom = a[2]; f.paddingLeft = a[3]; }
      if (o.r != null) f.cornerRadius = o.r;
      if (o.stroke) { f.strokes = [paint(o.stroke)]; f.strokeWeight = o.sw || 1; f.strokeAlign = 'INSIDE'; }
      if (o.align) f.counterAxisAlignItems = o.align;
      if (o.justify) f.primaryAxisAlignItems = o.justify;
      if (o.w || o.h) {
        f.resize(o.w || f.width, o.h || f.height);
        const wA = dir === 'HORIZONTAL' ? 'primaryAxisSizingMode' : 'counterAxisSizingMode';
        const hA = dir === 'HORIZONTAL' ? 'counterAxisSizingMode' : 'primaryAxisSizingMode';
        f[wA] = o.w ? 'FIXED' : 'AUTO'; f[hA] = o.h ? 'FIXED' : 'AUTO';
      }
      if (o.wrap) { f.layoutWrap = 'WRAP'; f.counterAxisSpacing = o.wrapGap != null ? o.wrapGap : (o.gap || 8); }
      return f;
    }
    function add(p, ...kids) { for (const k of kids) if (k) p.appendChild(k); return p; }
    function fillW(n) { if (n.type === 'TEXT') n.textAutoResize = 'HEIGHT'; n.layoutSizingHorizontal = 'FILL'; return n; }
    function fillH(n) { n.layoutSizingVertical = 'FILL'; return n; }
    function spacer(p) { const s = figma.createFrame(); s.name = 'spacer'; s.fills = []; s.resize(1, 1); p.appendChild(s); if (p.layoutMode === 'VERTICAL') s.layoutSizingVertical = 'FILL'; else s.layoutSizingHorizontal = 'FILL'; return s; }
    function recolor(inst, color) { for (const v of inst.findAll(x => SHAPES.includes(x.type))) v.fills = [paint(color)]; return inst; }
    function icon(n, size = 20, color = 'ink') { const i = C['Icon/' + n].createInstance(); i.rescale(size / 24); if (color !== 'ink') recolor(i, color); return i; }
    function btn(label, ty = 'Primary', sz = 'M', st = 'Default') { ty = ty === 'Ghost' ? 'Text' : (ty === 'Accent' ? 'Primary' : ty); const s = C['Button']; const i = s.children.find(c => c.name === `Type=${ty}, Size=${sz}, State=${st}`).createInstance(); i.setProperties({ [pk(s, 'Label')]: label }); return i; }
    function chip(label, st = 'Default') { const s = C['Chip']; const i = s.children.find(c => c.name === `State=${st}`).createInstance(); i.setProperties({ [pk(s, 'Label')]: label }); return i; }
    function badge(label, tone = 'Teal') { tone = tone === 'Warn' ? 'Peach' : tone; const s = C['Badge']; const i = s.children.find(c => c.name === `Tone=${tone}`).createInstance(); i.setProperties({ [pk(s, 'Label')]: label }); return i; }
    function field(label, value, comp = 'Input', filled = false) { const c = C[comp]; const i = c.createInstance(); i.setProperties({ [pk(c, 'Label')]: label, [pk(c, 'Value')]: value }); if (filled) { const v = i.findOne(n => n.name === 'Value' && n.type === 'TEXT'); if (v) v.fills = [paint('ink')]; } return i; }
    function img(caption, w, h, r) { const c = C['Placeholder/Image']; const i = c.createInstance(); i.resize(w, h); i.setProperties({ [pk(c, 'Caption')]: caption }); if (r != null) i.cornerRadius = r; return i; }
    function avatar(ch = '지', size = 32) { const c = C['Avatar']; const i = c.createInstance(); i.setProperties({ [pk(c, 'Initial')]: ch }); if (size !== 32) i.rescale(size / 32); return i; }
    function pose(id = 'P1', size = 160) { const s = C['Character']; const v = s.children.find(c => c.name.startsWith(`Pose=${id}`)); const i = v.createInstance(); i.rescale(size / 160); return i; }
    function logo(h = 36) { const i = C['Logo'].createInstance(); if (h !== 36) i.rescale(h / 36); return i; }
    async function gnb(kind = '회원', active) {
      const i = C[`GNB/${kind}`].createInstance();
      if (active) { const it = i.findOne(n => n.name === `nav/${active}`); if (it) { const u = it.findOne(n => n.name === 'underline'); if (u) u.visible = true; const t = it.findOne(n => n.type === 'TEXT'); if (t) t.fills = [paint('ink')]; } }
      return i;
    }
    async function screen(id, title, kind = '회원', active, o = {}) {
      const f = AL('VERTICAL', { name: `${id}_${title.replace(/\s+/g, '')}`, w: 1440, fill: o.bg || 'page' });
      f.clipsContent = true;
      if (kind) add(f, await gnb(kind, active));
      const body = AL(o.dir || 'VERTICAL', { name: 'Body', gap: o.gap != null ? o.gap : 24, p: o.p || [40, 120, 80, 120], align: o.align });
      add(f, body); fillW(body);
      return { f, body, minH: o.minH || 960 };
    }
    async function note(s, text) { const b = AL('HORIZONTAL', { name: 'Dev note', fill: 'rice', p: [16, 120], gap: 12, align: 'CENTER' }); add(b, badge('개발 메모', 'Dark')); const t = await T(text, 'body/m', 'text-2'); add(b, t); add(s.f, b); fillW(b); fillW(t); if (s.f.height < s.minH) s.body.minHeight = s.body.height + (s.minH - s.f.height); return s.f; }
    function panel(o = {}) { return AL(o.dir || 'VERTICAL', { name: o.name || 'Panel', fill: o.fill || 'white', stroke: o.stroke === false ? null : (o.stroke || 'line'), r: o.r != null ? o.r : 16, p: o.p != null ? o.p : 24, gap: o.gap != null ? o.gap : 16, w: o.w, h: o.h, align: o.align, justify: o.justify }); }
    async function header(title, sub, o = {}) { const h = AL('VERTICAL', { gap: 8, name: 'Header' }); if (o.crumb) add(h, await T(o.crumb, 'caption', 'text-2')); add(h, await T(title, o.size || 'h1')); if (sub) add(h, await T(sub, 'body/m', 'text-2')); return h; }
    function row(gap = 12, o = {}) { return AL('HORIZONTAL', { gap, align: o.align || 'CENTER', name: o.name || 'Row', wrap: o.wrap, p: o.p, fill: o.fill, r: o.r, stroke: o.stroke }); }
    function colm(gap = 12, o = {}) { return AL('VERTICAL', { gap, name: o.name || 'Col', w: o.w, p: o.p, fill: o.fill, r: o.r, stroke: o.stroke, align: o.align }); }
    function progress(rate, w = 240, color = 'teal') { const tr = figma.createFrame(); tr.name = `progress ${Math.round(rate * 100)}%`; tr.resize(w, 8); tr.cornerRadius = 4; tr.fills = [paint('line')]; const b = figma.createRectangle(); b.resize(Math.max(4, w * rate), 8); b.cornerRadius = 4; b.fills = [paint(color)]; tr.appendChild(b); b.constraints = { horizontal: 'SCALE', vertical: 'STRETCH' }; return tr; }
    function divider(w) { const r = figma.createRectangle(); r.name = 'divider'; r.resize(w || 100, 1); r.fills = [paint('line')]; return r; }
    async function tabs(items, active = 0) { const r = AL('HORIZONTAL', { gap: 24, name: 'Tabs', align: 'MAX' }); for (let k = 0; k < items.length; k++) { const c = AL('VERTICAL', { gap: 8, name: `tab/${items[k]}`, align: 'CENTER' }); const t = await T(items[k], 'label', k === active ? 'ink' : 'text-2'); const u = figma.createRectangle(); u.resize(Math.max(8, t.width), 2); u.fills = k === active ? [paint('primary')] : []; add(c, t, u); add(r, c); } return r; }
    async function metaItem(ic, text, o = {}) { const r = AL('HORIZONTAL', { gap: 4, align: 'CENTER', name: 'meta/' + ic }); add(r, icon(ic, 16, 'text-2'), await T(text, o.style || 'caption', 'text-2', { name: 'text' })); return r; }
    function shadow(n, kind = 'modal') { const st = { hover: 'shadow/hover', modal: 'shadow/modal' }[kind]; return figma.getLocalEffectStylesAsync().then(ss => { const s = ss.find(x => x.name === st); if (s) return n.setEffectStyleIdAsync(s.id); }); }

    // ───────── 공통 도우미 ─────────
    const touched = [];
    async function step(name, fn) {
      try { const r = await fn(); log(`✔ ${name}${r ? ' — ' + r : ''}`); }
      catch (e) { log(`✖ ${name} — ${e && e.message ? e.message : String(e)}`); }
    }
    const F = id => { for (const sec of PAGE.children) { if (sec.type !== 'SECTION') continue; const f = sec.children.find(n => n.type === 'FRAME' && (n.name.startsWith(id + '_') || n.name.startsWith(id + ' '))); if (f) return f; } return null; };
    const need = (n, what) => { if (!n) throw new Error(what + ' 못 찾음'); return n; };
    async function mcInfo(n) { if (!n || n.type !== 'INSTANCE') return null; const m = await n.getMainComponentAsync(); if (!m) return null; const set = m.parent && m.parent.type === 'COMPONENT_SET' ? m.parent.name : m.name; return { set, name: m.name }; }
    async function instancesOf(root, setName) { const out = []; for (const i of root.findAllWithCriteria({ types: ['INSTANCE'] })) { const m = await mcInfo(i); if (m && m.set === setName) out.push(i); } return out; }
    const firstText = n => n.findOne(x => x.type === 'TEXT');
    const labelOf = n => { const t = firstText(n); return t ? t.characters : ''; };
    async function setLabel(inst, s) { await loadFontsOf(inst); const k = Object.keys(inst.componentProperties).find(x => x.split('#')[0] === 'Label'); if (k) inst.setProperties({ [k]: s }); else { const t = firstText(inst); if (t) t.characters = s; } }
    const variantOf = (set, nm) => C[set] && C[set].children.find(c => c.name === nm);
    async function swapBtn(inst, ty, sz, label) {
      const lbl = label != null ? label : labelOf(inst);
      const v = variantOf('Button', `Type=${ty}, Size=${sz}, State=Default`); if (!v) throw new Error(`버튼 변형 없음 ${ty}/${sz}`);
      const fill = inst.layoutSizingHorizontal === 'FILL';
      inst.swapComponent(v); await setLabel(inst, lbl);
      if (fill) inst.layoutSizingHorizontal = 'FILL';
    }
    async function swapBadge(inst, tone, label) { const v = variantOf('Badge', `Tone=${tone}`); if (!v) return; const lbl = label != null ? label : labelOf(inst); inst.swapComponent(v); await setLabel(inst, lbl); }
    async function buttonsByLabel(root, lbl) { return (await instancesOf(root, 'Button')).filter(b => labelOf(b) === lbl); }
    const hasText = (root, s) => root.findOne(n => n.type === 'TEXT' && n.characters.includes(s));
    const isPose = n => n.type === 'INSTANCE' && (n.name.startsWith('Character') || n.name.startsWith('Pose'));
    const bodyOf = f => need(f.children.find(n => n.name === 'Body'), 'Body');

    // ───────── 01 · 02 (컨셉북 18장: A 틀 카드 440 · 모서리 24 · 카드 위 로고 48) ─────────
    const get = id => figma.getNodeByIdAsync(id);
    async function cardA(id, withLogo) {
      const p = need(await get(id), '카드 ' + id); await loadFontsOf(p);
      p.resize(440, p.height); p.primaryAxisSizingMode = 'AUTO';
      p.cornerRadius = 24; p.paddingTop = p.paddingBottom = 40; p.paddingLeft = p.paddingRight = 40; p.itemSpacing = 16;
      for (const c of p.children) {
        if (c.name === 'Logo' || isPose(c)) continue;
        if (c.type === 'INSTANCE' || (c.type === 'FRAME' && c.name !== 'Row') || (c.type === 'TEXT' && c.width > 200)) { try { c.layoutSizingHorizontal = 'FILL'; } catch (e) { } }
      }
      for (const c of p.children.filter(n => n.type === 'FRAME' && n.name === 'Row' && n.width > 300)) { try { c.layoutSizingHorizontal = 'FILL'; } catch (e) { } }
      if (withLogo && !p.children.find(n => n.name === 'Logo')) { const lg = logo(48); p.insertChild(0, lg); p.counterAxisAlignItems = 'MIN'; }
      const h = p.children.find(n => n.type === 'TEXT'); if (h) await h.setTextStyleIdAsync(TS['h2'].id);
      return p;
    }
    async function primaryL(id) { const b = await get(id); if (!b || b.type !== 'INSTANCE') return; await swapBtn(b, 'Primary', 'L'); try { b.layoutSizingHorizontal = 'FILL'; } catch (e) { } }

    await step('01-1 로그인', async () => {
      await cardA('2:389', true);
      await primaryL('2:400');
      const err = await get('2:399'); if (err && err.type === 'TEXT') err.fills = [paint('danger')];
      const brands = { Google: ['#FFFFFF', '#1F1F1F', true], Kakao: ['#FEE500', '#191919', false], Naver: ['#03C75A', '#FFFFFF', false], '카카오': ['#FEE500', '#191919', false], '네이버': ['#03C75A', '#FFFFFF', false], '구글': ['#FFFFFF', '#1F1F1F', true] };
      let n = 0;
      for (const id of ['2:412', '2:414', '2:416']) {
        const b = await get(id); if (!b) continue; await loadFontsOf(b);
        const t = firstText(b); if (!t) continue;
        const key = Object.keys(brands).find(k => t.characters.includes(k)) || 'Google'; const [bg, fg, border] = brands[key];
        b.resize(b.width, 48); try { b.layoutSizingHorizontal = 'FILL'; } catch (e) { }
        b.fills = [{ type: 'SOLID', color: hex(bg) }]; b.strokes = border ? [paint('line')] : []; t.fills = [{ type: 'SOLID', color: hex(fg) }];
        n++;
      }
      touched.push('2:360');
      return `소셜 버튼 ${n}개`;
    });
    await step('01-2 · 01-3 · 01-4 회원가입 · 비밀번호', async () => {
      await cardA('2:452', true); await primaryL('2:491');
      const sent = await cardA('2:495', false);
      if (!sent.findOne(n => n.name === 'iconCircle')) { const ic = await get('2:496'); if (ic) { const at = sent.children.indexOf(ic); ic.remove(); const circ = AL('HORIZONTAL', { name: 'iconCircle', fill: 'primary-tint', r: 24, align: 'CENTER', justify: 'CENTER', w: 48, h: 48 }); sent.insertChild(Math.max(0, at), circ); add(circ, icon('check', 24, 'primary')); } }
      await cardA('2:540', true); await primaryL('2:547');
      await cardA('2:587', true); await primaryL('2:598');
      await cardA('2:600', false);
      touched.push('2:422', '2:511', '2:557');
    });
    await step('01-5 이용 상황 설정', async () => {
      const hdrRow = need(await get('2:646'), '머리 줄'); const hdr = need(await get('2:647'), '머리 글');
      await loadFontsOf(hdrRow);
      if (!hdrRow.findOne(n => n.name === 'Greeting')) { const wrap = row(16, { name: 'Greeting' }); hdrRow.insertChild(0, wrap); add(wrap, pose('P1', 96), hdr); }
      const t = await get('2:648'); if (t && t.type === 'TEXT') { await loadFontsOf(t.parent); t.characters = '어떻게 취미를 즐기고 싶나요?'; await t.setTextStyleIdAsync(TS['h1'].id); }
      const pb = await get('2:769'); if (pb && pb.type === 'INSTANCE') await swapBtn(pb, 'Primary', 'L', '저장하고 숏폼 탐색 시작');
      touched.push('2:612');
    });
    await step('02-1 마이페이지', async () => {
      const prof = need(await get('3:529'), '프로필 카드'); await loadFontsOf(prof); prof.cornerRadius = 16;
      if (!prof.findOne(n => n.name === 'avatar (한 입 자국)')) {
        const av = need(await get('3:530'), '아바타'); const at = prof.children.indexOf(av);
        const holder = figma.createFrame(); holder.name = 'avatar (한 입 자국)'; holder.resize(72, 72); holder.fills = []; holder.clipsContent = true; prof.insertChild(Math.max(0, at), holder);
        holder.appendChild(av); av.x = 0; av.y = 0;
        for (const [d, x, y] of [[20, 58, -6], [14, 50, -8]]) { const e = figma.createEllipse(); e.name = 'bite'; e.resize(d, d); e.x = x; e.y = y; e.fills = [paint('white')]; holder.appendChild(e); }
      }
      const labels = ['관심 취미', '진행 중', '보관한 숏폼', '내 글'], nums = ['4', '2', '12', '3'];
      const ids = ['3:566', '3:569', '3:572', '3:575'];
      for (let k = 0; k < 4; k++) { const p = await get(ids[k]); if (!p) continue; await loadFontsOf(p); const ts = p.children.filter(n => n.type === 'TEXT'); if (ts[0]) { ts[0].characters = nums[k]; await ts[0].setTextStyleIdAsync(TS['number/l'].id); } if (ts[1]) ts[1].characters = labels[k]; p.cornerRadius = 16; }
      touched.push('3:495');
    });

    // ───────── 06 내 취미 (컨셉북 17장) ─────────
    await step('06-1 내 취미', async () => {
      const f = need(F('06-1'), '06-1'); await loadFontsOf(f); const body = bodyOf(f);
      let n = 0;
      for (const p of body.children.filter(x => x.type === 'FRAME' && x.layoutMode === 'HORIZONTAL')) {
        const prog = p.findOne(x => x.type === 'FRAME' && x.name.startsWith('progress'));
        if (!prog) continue;
        const im = p.children[0]; if (im && im.type === 'INSTANCE') { im.resize(64, 64); im.cornerRadius = 8; }
        const paused = (await buttonsByLabel(p, '다시 시작')).length > 0;
        if (paused) for (const r of prog.children) r.fills = [paint('text-3')];
        const sub = p.findOne(x => x.type === 'TEXT' && /(\d+)개 중 (\d+)개/.test(x.characters));
        if (sub && prog.parent && prog.parent.name !== 'progressRow') {
          const m = sub.characters.match(/(\d+)개 중 (\d+)개/); const holder = prog.parent; const at = holder.children.indexOf(prog);
          const pr = row(8, { name: 'progressRow' }); holder.insertChild(at, pr); pr.appendChild(prog);
          pr.layoutSizingHorizontal = 'FILL'; prog.layoutSizingHorizontal = 'FILL';
          pr.appendChild(await T(`${m[2]}/${m[1]}`, 'caption', 'text-2', { name: 'count' }));
        }
        n++;
      }
      for (const b of await buttonsByLabel(f, '관심 해제')) await setLabel(b, '해제');
      touched.push(f.id);
      return `가로 행 ${n}개`;
    });
    await step('06-2 주차별 TODO', async () => {
      const f = need(F('06-2'), '06-2'); await loadFontsOf(f); const body = bodyOf(f);
      for (const b of await instancesOf(f, 'Button')) { const l = labelOf(b); if (l.startsWith('✎')) await setLabel(b, l.replace(/^✎\s*/, '')); }
      const pt = hasText(body, '16개 중 5개 완료'); if (pt) pt.characters = '5/16 완료 · 31% · 건너뜀 1 · 10.12 시작';
      const isWeekTitle = t => t.type === 'TEXT' && t.characters === '2주차 · 기본 동작 익히기';
      const two = need(body.children.find(x => x.type === 'FRAME' && x.layoutMode === 'HORIZONTAL' && x.findOne(isWeekTitle)), '본문 2단');
      const lc = need(two.children.find(x => x.type === 'FRAME' && x.findOne(isWeekTitle)), '활동 열');
      const rc = two.children.find(x => x.type === 'FRAME' && x !== lc && x.name !== 'Week nav' && !!hasText(x, '이번 주 진행'));
      if (!two.findOne(x => x.name === 'Week nav')) {
        const nav = colm(4, { name: 'Week nav', w: 160 }); two.insertChild(0, nav); nav.primaryAxisSizingMode = 'AUTO';
        for (let k = 1; k <= 8; k++) {
          const cur = k === 2, done = k === 1;
          const it = row(6, { p: [10, 12], r: 12, fill: cur ? 'primary-tint' : null, name: `week/${k}` });
          add(it, await T(`${k}주차`, cur ? 'label' : 'body/m', cur ? 'ptext' : (done ? 'text-2' : 'ink')));
          if (done) add(it, icon('check', 16, 'text-2'));
          add(nav, it); fillW(it);
        }
        const wk = body.children.find(x => x !== two && x.type === 'FRAME' && x.layoutMode === 'HORIZONTAL' && x.children.length >= 8 && !!x.findOne(t => t.type === 'TEXT' && t.characters.startsWith('1주차')));
        if (wk) wk.remove();
      }
      let n = 0;
      for (const p of lc.children.filter(x => x.type === 'FRAME' && x.findOne(c => c.name === 'checkbox'))) {
        const cb = p.findOne(c => c.name === 'checkbox'); if (cb) { cb.resize(24, 24); cb.cornerRadius = 8; }
        for (const bd of await instancesOf(p, 'Badge')) if (labelOf(bd) === '완료') await swapBadge(bd, 'Success');
        const bs = await instancesOf(p, 'Button');
        for (const b of bs) if (labelOf(b) === '건너뛰기') await swapBtn(b, 'Secondary', 'S');
        const hasMemo = bs.some(b => labelOf(b).includes('메모')) || !!p.findOne(x => x.type === 'TEXT' && x.characters === '메모');
        const doneBtn = bs.find(b => labelOf(b) === '완료');
        if (doneBtn && !hasMemo && doneBtn.parent) doneBtn.parent.appendChild(btn('메모', 'Secondary', 'S'));
        n++;
      }
      if (rc && !rc.findOne(x => x.name === 'Week done (state)')) {
        const card = panel({ name: 'Week done (state)', fill: 'rice', stroke: false, align: 'CENTER', gap: 8, p: 24 });
        add(card, await T('주차를 모두 끝냈을 때', 'caption', 'text-2'), pose('P5', 96));
        add(card, await T('2주차 완료!', 'display/m', 'ink'), await T('다음 한 입도 준비됐어요.', 'body/m', 'text-2'), btn('3주차 보기', 'Primary', 'M'));
        rc.insertChild(0, card); fillW(card);
      }
      touched.push(f.id);
      return `활동 ${n}개`;
    });

    // ───────── 07 장소 · 정보 (컨셉북 17장) ─────────
    await step('07-1 활동 장소', async () => {
      const f = need(F('07-1'), '07-1'); await loadFontsOf(f);
      for (const b of await buttonsByLabel(f, '이 지역으로 저장')) await setLabel(b, '변경');
      const map = need(f.findOne(x => x.type === 'FRAME' && x.name.startsWith('Map')), '지도');
      const b2 = map.parent;
      const list = need(b2.children.find(x => x !== map && x.type === 'FRAME' && x.layoutMode === 'VERTICAL'), '장소 목록');
      list.resize(488, list.height); list.primaryAxisSizingMode = 'AUTO';
      let n = 0;
      for (const p of list.children.filter(x => x.type === 'FRAME' && x.name === 'Panel')) {
        let info = p.children.find(x => x.name === 'Info');
        if (!info) {
          const kids = [...p.children];
          p.layoutMode = 'HORIZONTAL'; p.itemSpacing = 16; p.counterAxisAlignItems = 'MIN';
          info = colm(6, { name: 'Info' }); p.appendChild(info);
          for (const k of kids) info.appendChild(k);
          info.layoutSizingHorizontal = 'FILL';
          for (const k of kids) if (k.type === 'FRAME') { try { k.layoutSizingHorizontal = 'FILL'; } catch (e) { } }
          p.insertChild(0, img('사진', 72, 72, 8));
          try { p.layoutSizingHorizontal = 'FILL'; p.layoutSizingVertical = 'HUG'; } catch (e) { }
        }
        const r2 = info.children.find(x => x.type === 'FRAME' && x.children.some(c => c.type === 'TEXT' && c.characters === '·'));
        if (r2) {
          const dot = r2.children.find(c => c.type === 'TEXT' && c.characters === '·');
          const fee = r2.children[r2.children.indexOf(dot) + 1];
          if (fee && fee.type === 'TEXT') { const at = r2.children.indexOf(fee); r2.insertChild(at, badge(fee.characters, 'Peach')); fee.remove(); dot.remove(); }
        }
        n++;
      }
      for (const pin of map.children.filter(x => x.type === 'INSTANCE')) recolor(pin, pin.width > 30 ? 'ink' : 'primary');
      touched.push(f.id);
      return `장소 행 ${n}개`;
    });
    await step('07-2 정보 피드', async () => {
      const f = need(F('07-2'), '07-2'); await loadFontsOf(f); const body = bodyOf(f);
      const ext = !!C['Icon/open-in-new'];
      let n = 0;
      for (const p of body.children.filter(x => x.type === 'FRAME' && x.layoutMode === 'HORIZONTAL' && x.name === 'Panel')) {
        const im = p.children[0]; if (im && im.type === 'INSTANCE' && im.width >= 100) { im.resize(128, 96); im.cornerRadius = 16; }
        for (const b of await instancesOf(p, 'Button')) {
          if (!labelOf(b).startsWith('자세히 보기') || (b.parent && b.parent.name === 'linkRow')) continue;
          await swapBtn(b, 'Text', 'S', ext ? '자세히 보기' : '자세히 보기 ↗');
          if (ext) { const at = p.children.indexOf(b); const lr = row(2, { name: 'linkRow' }); p.insertChild(at, lr); lr.appendChild(b); lr.appendChild(icon('open-in-new', 16, 'primary')); }
        }
        n++;
      }
      touched.push(f.id);
      return `피드 행 ${n}개`;
    });

    // ───────── 08 커뮤니티 (컨셉북 18장) ─────────
    await step('08-1 게시판 목록', async () => {
      const f = need(F('08-1'), '08-1'); await loadFontsOf(f); const body = bodyOf(f);
      const nav = body.children[0], rc = body.children[1];
      for (const it of nav.children.filter(x => x.type === 'FRAME' && x.fills && x.fills.length)) { const t = it.findOne(x => x.type === 'TEXT'); if (t) t.fills = [paint('ptext')]; }
      const wb = (await instancesOf(nav, 'Button')).find(b => labelOf(b).includes('글쓰기'));
      const title = rc.children.find(x => x.type === 'TEXT' && x.characters.endsWith('게시판'));
      if (wb && title) {
        const hr = row(16, { name: 'Header row' }); rc.insertChild(rc.children.indexOf(title), hr);
        hr.appendChild(title); spacer(hr); hr.appendChild(wb); hr.layoutSizingHorizontal = 'FILL';
        try { wb.layoutSizingHorizontal = 'HUG'; } catch (e) { }
        await setLabel(wb, '글쓰기');
      }
      const tbl = rc.findOne(x => x.name === 'Table');
      if (tbl) for (const r of tbl.children) if ('children' in r) r.children.forEach((c, k) => { if (k >= 3 && c.type === 'FRAME') c.primaryAxisAlignItems = 'MAX'; });
      touched.push(f.id);
    });
    async function narrow(fid, w) {
      const f = need(F(fid), fid); await loadFontsOf(f); const body = bodyOf(f);
      const c = need(body.children.find(x => x.type === 'FRAME' && x.layoutMode === 'VERTICAL' && x.width >= 700), '본문 열');
      c.resize(w, c.height); c.primaryAxisSizingMode = 'AUTO';
      for (const ch of c.children) if (ch.type === 'INSTANCE' && ch.width >= 700) { try { ch.layoutSizingHorizontal = 'FILL'; } catch (e) { } }
      return { f, c };
    }
    await step('08-2 게시글 상세', async () => {
      const { f, c } = await narrow('08-2', 760);
      for (const b of await instancesOf(c, 'Button')) { const l = labelOf(b); if (l.startsWith('♥')) await setLabel(b, l.replace(/^♥\s*/, '')); else if (l === '링크 복사') await swapBtn(b, 'Secondary', 'S'); }
      for (const r of c.findAll(x => x.type === 'FRAME' && x.name === 'reply')) r.paddingLeft = 32;
      touched.push(f.id);
    });
    await step('08-3 글쓰기', async () => {
      const { f, c } = await narrow('08-3', 760);
      for (const ta of await instancesOf(c, 'Textarea')) { const fld = ta.findOne(n => n.name === 'Field'); if (fld && fld.height < 320) fld.resize(fld.width, 320); }
      touched.push(f.id);
    });
    await step('08-4 신고 모달', async () => {
      const f = need(F('08-4'), '08-4'); await loadFontsOf(f);
      try { await narrow('08-4', 760); } catch (e) { }
      const m = f.findOne(x => x.type === 'FRAME' && x.name === 'Panel' && x.width < 600 && !!x.findOne(t => t.type === 'TEXT' && t.characters.startsWith('신고 대상')));
      if (m) { const w0 = m.width; m.resize(480, m.height); m.primaryAxisSizingMode = 'AUTO'; if (m.parent && m.parent.layoutMode === 'NONE') m.x = m.x - (480 - w0) / 2; }
      for (const b of await buttonsByLabel(f, '신고하기')) await swapBtn(b, 'Danger', 'M');
      touched.push(f.id);
    });

    // ───────── 09 알림 · 공지 · 문의 (컨셉북 18장) ─────────
    await step('09-1 알림', async () => {
      const f = need(F('09-1'), '09-1'); await loadFontsOf(f);
      for (const b of await buttonsByLabel(f, '모두 읽음')) await swapBtn(b, 'Text', 'S');
      let un = 0;
      for (const p of f.findAll(x => x.type === 'FRAME' && x.layoutMode === 'HORIZONTAL' && !!x.children.find(d => d.name === 'unread dot'))) {
        const dot = p.children.find(d => d.name === 'unread dot');
        if (dot.fills && dot.fills.length) { dot.fills = [paint('peach')]; p.fills = [paint('rice')]; p.strokes = []; un++; }
        const ts = p.findAll(t => t.type === 'TEXT'); const tm = ts[ts.length - 1];
        if (tm) { if (tm.characters === '오늘 09:00') tm.characters = '5시간 전'; else if (tm.characters === '어제') tm.characters = '1일 전'; }
      }
      touched.push(f.id);
      return `안 읽음 ${un}개`;
    });
    await step('09-2 공지', async () => {
      const f = need(F('09-2'), '09-2'); await loadFontsOf(f);
      const t = f.findOne(x => x.type === 'TEXT' && /^📌/.test(x.characters));
      if (t) {
        const m = t.characters.match(/^📌\s*/); t.deleteCharacters(0, m[0].length);
        const r1 = t.parent.children.find(x => x.type === 'FRAME');
        if (r1 && !r1.findOne(x => x.type === 'TEXT' && x.characters === '공지')) r1.insertChild(0, badge('공지', 'Peach'));
      }
      touched.push(f.id);
    });
    await step('09-3 문의', async () => {
      const f = need(F('09-3'), '09-3'); await loadFontsOf(f);
      let k = 0;
      for (const bd of await instancesOf(f, 'Badge')) if (labelOf(bd) === '답변 완료') { await swapBadge(bd, 'Success'); k++; }
      const lab = f.findOne(x => x.type === 'TEXT' && x.characters === '관리자 답변');
      if (lab) { lab.fills = [paint('ptext')]; let p = lab.parent; while (p && p.name !== 'Panel') p = p.parent; if (p) p.fills = [paint('rice')]; }
      touched.push(f.id);
      return `답변 완료 태그 ${k}개`;
    });

    // ───────── C 공통 상태 (컨셉북 20장) ─────────
    await step('C-2 공통 상태', async () => {
      const f = need(F('C-2'), 'C-2'); await loadFontsOf(f);
      const et = f.findOne(x => x.type === 'TEXT' && x.characters === '아직 관심 저장한 취미가 없어요');
      if (et) {
        const p = et.parent;
        if (!p.children.some(isPose)) { const ic = p.children.find(x => x.type === 'INSTANCE' && x.name.startsWith('Icon')); if (ic) { const at = p.children.indexOf(ic); p.insertChild(at, pose('P3', 160)); ic.remove(); } }
        await et.setTextStyleIdAsync(TS['display/m'].id);
      }
      const er = f.findOne(x => x.type === 'TEXT' && (x.characters === '일시적인 오류가 발생했어요' || x.characters === '목록을 불러오지 못했어요'));
      if (er) {
        er.characters = '목록을 불러오지 못했어요';
        const p = er.parent;
        if (!p.children.some(isPose)) { const ic = p.children.find(x => x.type === 'INSTANCE' && x.name.startsWith('Icon')); if (ic) { const at = p.children.indexOf(ic); p.insertChild(at, pose('P4', 96)); ic.remove(); } }
      }
      const lm = f.findOne(x => x.type === 'FRAME' && x.name === 'Panel' && x.width < 400 && !!x.findOne(t => t.type === 'TEXT' && t.characters === '로그인이 필요해요'));
      if (lm) { lm.cornerRadius = 24; lm.effects = []; await shadow(lm, 'modal'); }
      touched.push(f.id);
    });

    // ───────── 섹션 크기·간격 다시 맞추기 ─────────
    await step('섹션 재배치', async () => {
      const order = ['00 · 홈', '01 · 인증 · 온보딩', '02 · 프로필 · 마이페이지', '03 · 숏폼 탐색', '04 · 취미 검색 · 추천 · 상세', '05 · 보관함', '06 · 내 취미 · TODO', '07 · 장소 · 정보 피드', '08 · 커뮤니티', '09 · 알림 · 공지 · 문의', '10 · 관리자', 'C · 공통 모달 · 상태'];
      const secs = PAGE.children.filter(n => n.type === 'SECTION');
      let y = 0, k = 0;
      for (const name of order) {
        const s = secs.find(n => n.name === name); if (!s) continue;
        const fr = s.children.filter(c => c.type === 'FRAME'); if (!fr.length) continue;
        const w = Math.max(...fr.map(f => f.x + f.width)) + 80, h = Math.max(...fr.map(f => f.y + f.height)) + 80;
        s.resizeWithoutConstraints(w, h); s.x = 0; s.y = y; y += h + 200; k++;
      }
      return `섹션 ${k}개`;
    });

    const nodes = []; for (const id of touched) { const n = await figma.getNodeByIdAsync(id); if (n) nodes.push(n); }
    if (nodes.length) { figma.currentPage.selection = nodes; figma.viewport.scrollAndZoomIntoView(nodes); }
  } catch (e) { log('✖ 중단 — ' + (e && e.message ? e.message : String(e))); }
  const ok = LOGS.filter(s => s.startsWith('✔')).length, ng = LOGS.filter(s => s.startsWith('✖')).length;
  figma.ui.postMessage(LOGS.join('\n') + `\n\n성공 ${ok} · 실패 ${ng}\n되돌리려면 Ctrl+Z, 결과는 이 창을 캡처해서 보내 주세요.`);
})();
