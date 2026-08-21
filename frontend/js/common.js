const API_BASE = "/api";
const App = (() => {
  const token = () => localStorage.getItem("token");
  const role = () => (localStorage.getItem("rol") || "").toUpperCase();
  async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (token()) headers.set("Authorization", `Bearer ${token()}`);
    if (!(options.body instanceof FormData) && options.body != null && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
    const type = res.headers.get("content-type") || "";
    let body;
    if (type.includes("application/json")) body = await res.json();
    else if (type.includes("application/pdf") || type.includes("spreadsheet") || type.includes("octet-stream")) body = await res.blob();
    else body = await res.text();
    if (!res.ok) {
      const message = typeof body === "string" ? body : body?.message || body?.error || `Error ${res.status}`;
      if (res.status === 401) logout();
      throw new Error(message);
    }
    return body;
  }
  function requireAuth(allowed = []) {
    if (!token()) { window.location.href = "login.html"; return false; }
    if (allowed.length && !allowed.some(r => role().includes(r.toUpperCase()))) { routeByRole(); return false; }
    return true;
  }
  function routeByRole() {
    const r = role();
    if (r.includes("ADMIN")) return location.replace("admin.html");
    if (r.includes("IMPLEMENTADOR")) return location.replace("implementador.html");
    if (r.includes("AUDITOR")) return location.replace("auditor.html");
    if (r.includes("CAPACITADOR")) return location.replace("capacitador.html");
    return location.replace("empresa.html");
  }
  function logout() { localStorage.clear(); window.location.href = "login.html"; }
  async function user() {
    const u = await api("/usuarios/me");
    localStorage.setItem("nombre", u.nombre || ""); localStorage.setItem("email", u.email || ""); localStorage.setItem("rol", u.rol || role());
    if (u.empresa) { localStorage.setItem("empresaId", u.empresa.id); localStorage.setItem("empresaNombre", u.empresa.nombre); }
    document.querySelectorAll("[data-user-name]").forEach(e => e.textContent = u.nombre || "Usuario");
    document.querySelectorAll("[data-user-role]").forEach(e => e.textContent = pretty(u.rol || role()));
    document.querySelectorAll("[data-company-name]").forEach(e => e.textContent = u.empresa?.nombre || "Sin empresa asignada");
    return u;
  }
  function bindNavigation() {
    document.querySelectorAll("[data-section]").forEach(btn => btn.addEventListener("click", () => {
      document.querySelectorAll(".section").forEach(s => s.classList.remove("active"));
      document.querySelectorAll("[data-section]").forEach(b => b.classList.remove("active"));
      document.getElementById(`section-${btn.dataset.section}`)?.classList.add("active"); btn.classList.add("active");
      window.scrollTo({top:0,behavior:"smooth"});
    }));
    document.querySelectorAll("[data-logout]").forEach(b => b.addEventListener("click", logout));
    document.querySelector("[data-mobile-menu]")?.addEventListener("click",()=>document.querySelector(".sidebar")?.classList.toggle("open"));
  }
  function toast(message, type = "success") {
    let area = document.querySelector(".toast-area"); if (!area) { area = document.createElement("div"); area.className = "toast-area"; document.body.appendChild(area); }
    const el = document.createElement("div"); el.className = `toast-app ${type}`; el.textContent = message; area.appendChild(el); setTimeout(() => el.remove(), 4200);
  }
  function esc(v) { return String(v ?? "").replace(/[&<>'"]/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;",'"':"&quot;"}[c])); }
  function pretty(v) { return String(v ?? "").replaceAll("_", " ").toLowerCase().replace(/\b\w/g, x => x.toUpperCase()); }
  function badge(v) {
    const x = String(v ?? "").toUpperCase(); let c = "b-info";
    if (["BAJA","BASE","IMPLEMENTADO","VALIDADA","COMPLETADA","COMPLETADO","APROBADA","CERRADO"].includes(x)) c="b-low";
    else if (["MEDIA","PENDIENTE","PLANIFICADO","PARCIAL","EN_PROCESO","EN_TRATAMIENTO","MODIFICADA"].includes(x)) c="b-medium";
    else if (["ALTA","RECHAZADA","NO_INICIADO","ABIERTO","NO_EFECTIVO"].includes(x)) c="b-high";
    else if (["CRITICA","CRÍTICA"].includes(x)) c="b-critical";
    return `<span class="badge-app ${c}">${esc(pretty(x))}</span>`;
  }
  function progress(v) { const n = Math.max(0, Math.min(100, Number(v)||0)); return `<div class="progress-app"><span style="width:${n}%"></span></div><small class="text-muted-app">${Math.round(n)}%</small>`; }
  function fmtDate(v) { if (!v) return "—"; const d = new Date(v); return Number.isNaN(d.getTime()) ? esc(v) : d.toLocaleDateString("es-CO"); }
  function modal(id) { const el=document.getElementById(id); return bootstrap.Modal.getOrCreateInstance(el); }
  function downloadBlob(blob, filename) { const u=URL.createObjectURL(blob); const a=document.createElement("a"); a.href=u;a.download=filename;document.body.appendChild(a);a.click();a.remove();URL.revokeObjectURL(u); }
  function mlPanel(a, compact = false) {
    const ml = a?.ml;
    if (!ml || !ml.prioridad) {
      return `<div class="ml-insight pending"><div><strong><i class="bi bi-stars"></i> Machine Learning</strong><span class="ml-kicker">Experimental</span></div><p>Estimación pendiente. El análisis determinista RPM sigue siendo válido y debe ser revisado por una persona.</p></div>`;
    }
    const conf = Math.round((Number(ml.confianza) || 0) * 100);
    const mismatch = ml.coincideConRpm === false;
    const low = conf < 70;
    const cls = mismatch ? "disagree" : (low ? "caution" : "agree");
    const probs = Object.entries(ml.probabilidades || {}).sort((a,b)=>b[1]-a[1]).slice(0,4);
    const probHtml = compact ? "" : `<div class="ml-probs">${probs.map(([k,v])=>`<span>${esc(pretty(k))}: <strong>${Math.round(Number(v)*100)}%</strong></span>`).join("")}</div>`;
    const note = mismatch ? "RPM y ML difieren: requiere revisión humana prioritaria." : (low ? "Coincidencia con confianza estimada baja: revisar con cautela." : "RPM y ML coinciden; la validación humana sigue siendo obligatoria.");
    return `<div class="ml-insight ${cls}"><div class="ml-head"><div><strong><i class="bi bi-stars"></i> Estimación ML</strong><span class="ml-kicker">Experimental · ${esc(ml.versionModelo || "modelo")}</span></div><div>${badge(ml.prioridad)} <strong>${conf}%</strong></div></div><p>${esc(note)}</p>${probHtml}</div>`;
  }
  return {api,requireAuth,routeByRole,logout,user,bindNavigation,toast,esc,pretty,badge,progress,fmtDate,modal,downloadBlob,mlPanel,role};
})();
