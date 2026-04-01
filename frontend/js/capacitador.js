// capacitador.js (frontend demo)
const API_URL = "http://localhost:8081/api";

let empresas = [];
let empresaActual = null;

const KEY_MATERIALES = (empresaId) => `cap_materiales_empresa_${empresaId}`;
const KEY_FIRMAS = "cap_firmas_demo";

document.addEventListener("DOMContentLoaded", () => {
  cargarPerfil();
  configurarLogout();
  cargarEmpresas();
  cargarFirmasDemo();

  const btnGuardar = document.getElementById("btnGuardarMaterial");
  if (btnGuardar) btnGuardar.addEventListener("click", guardarMaterial);

  const btnFirmar = document.getElementById("btnFinalizarFirmar");
  if (btnFirmar) btnFirmar.addEventListener("click", finalizarYFirmar);
});

function cargarPerfil() {
  const nombre = localStorage.getItem("nombre") || "Capacitador";
  const email = localStorage.getItem("email") || "";

  const sidebar = document.getElementById("nombreCapacitadorSidebar");
  if (sidebar) sidebar.textContent = nombre;

  const perfilTitulo = document.getElementById("perfilNombreTitulo");
  if (perfilTitulo) perfilTitulo.textContent = nombre;

  const perfilNombre = document.getElementById("perfilNombreInput");
  if (perfilNombre) perfilNombre.value = nombre;

  const perfilEmail = document.getElementById("perfilEmailInput");
  if (perfilEmail) perfilEmail.value = email;
}

function configurarLogout() {
  const btn = document.getElementById("btnLogout");
  if (!btn) return;

  btn.addEventListener("click", () => {
    localStorage.clear();
    window.location.href = "login.html";
  });
}

async function cargarEmpresas() {
  const token = localStorage.getItem("token");
  const cont = document.getElementById("listaEmpresas");
  if (!cont) return;

  // Intento backend (si existe)
  try {
    const res = await fetch(`${API_URL}/empresas`, {
      headers: { Authorization: `Bearer ${token}` }
    });

    const texto = await res.text();
    let data = [];
    try { data = texto ? JSON.parse(texto) : []; } catch { data = []; }

    if (res.ok && Array.isArray(data)) {
      empresas = data.map(e => ({
        id: e.id,
        nombre: e.nombre || "Sin nombre",
        sector: e.sector?.nombre || e.sector || "Sin sector"
      }));
    }
  } catch {
    // ignore
  }

  // Fallback demo si backend no responde o viene vacío
  if (empresas.length === 0) {
    empresas = [
      { id: 1, nombre: "USB tecnologia", sector: "Tecnologia" },
      { id: 2, nombre: "Empresa ABC", sector: "Servicios" }
    ];
  }

  renderEmpresas();
}

function renderEmpresas() {
  const cont = document.getElementById("listaEmpresas");
  if (!cont) return;

  cont.innerHTML = empresas.map(e => `
    <div class="col-md-4">
      <div class="empresa-card" id="empresa-${e.id}" onclick="seleccionarEmpresa(${e.id})">
        <div class="d-flex align-items-center gap-3">
          <i class="bi bi-building" style="font-size:1.8rem; color:#10b981"></i>
          <div>
            <p class="text-white fw-bold mb-0">${escapeHtml(e.nombre)}</p>
            <small class="text-white-50">Sector: ${escapeHtml(e.sector)}</small>
          </div>
        </div>
        <div class="mt-2">
          <span class="pill pendiente">Pendiente</span>
          <small class="text-white-50 ms-2">Capacitación</small>
        </div>
      </div>
    </div>
  `).join("");
}

// Se usa desde onclick en HTML (debe ser global)
window.seleccionarEmpresa = function(id) {
  const emp = empresas.find(x => Number(x.id) === Number(id));
  if (!emp) return;

  empresaActual = emp;

  document.querySelectorAll(".empresa-card").forEach(c => c.classList.remove("seleccionada"));
  const card = document.getElementById(`empresa-${id}`);
  if (card) card.classList.add("seleccionada");

  const label = document.getElementById("empresaSeleccionadaNombre");
  if (label) label.textContent = emp.nombre;

  const btnGuardar = document.getElementById("btnGuardarMaterial");
  if (btnGuardar) btnGuardar.disabled = false;

  const btnFirmar = document.getElementById("btnFinalizarFirmar");
  if (btnFirmar) btnFirmar.disabled = false;

  cargarMaterialesEmpresa();
};

function cargarMaterialesEmpresa() {
  const body = document.getElementById("tablaMaterialesBody");
  if (!body) return;

  if (!empresaActual) {
    body.innerHTML = `<tr><td colspan="5" class="text-white-50">Selecciona una empresa para ver materiales.</td></tr>`;
    return;
  }

  const raw = localStorage.getItem(KEY_MATERIALES(empresaActual.id));
  let materiales = [];
  try { materiales = raw ? JSON.parse(raw) : []; } catch { materiales = []; }

  if (!materiales.length) {
    body.innerHTML = `<tr><td colspan="5" class="text-white-50">No hay materiales aún para esta empresa.</td></tr>`;
    return;
  }

  body.innerHTML = materiales.map((m, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(m.titulo)}</td>
      <td>${m.youtube ? `<a href="${escapeAttr(m.youtube)}" target="_blank" style="color:#10b981">Ver</a>` : '-'}</td>
      <td>${m.archivoNombre ? escapeHtml(m.archivoNombre) : '-'}</td>
      <td>${escapeHtml(m.fecha)}</td>
    </tr>
  `).join("");
}

function guardarMaterial() {
  if (!empresaActual) return;

  const titulo = (document.getElementById("matTitulo")?.value || "").trim();
  const youtube = (document.getElementById("matYoutube")?.value || "").trim();
  const fileInput = document.getElementById("matArchivo");
  const archivo = fileInput?.files?.[0];

  const estado = document.getElementById("capEstado");

  if (!titulo) {
    if (estado) estado.textContent = "El título es obligatorio.";
    return;
  }

  const raw = localStorage.getItem(KEY_MATERIALES(empresaActual.id));
  let materiales = [];
  try { materiales = raw ? JSON.parse(raw) : []; } catch { materiales = []; }

  materiales.unshift({
    titulo,
    youtube: youtube || null,
    archivoNombre: archivo ? archivo.name : null,
    fecha: new Date().toLocaleString()
  });

  localStorage.setItem(KEY_MATERIALES(empresaActual.id), JSON.stringify(materiales));

  // limpiar campos
  document.getElementById("matTitulo").value = "";
  document.getElementById("matYoutube").value = "";
  if (fileInput) fileInput.value = "";

  if (estado) estado.textContent = "Material agregado correctamente (demo).";

  cargarMaterialesEmpresa();
}

function finalizarYFirmar() {
  if (!empresaActual) return;

  const nombre = localStorage.getItem("nombre") || "Capacitador";

  const raw = localStorage.getItem(KEY_FIRMAS);
  let firmas = [];
  try { firmas = raw ? JSON.parse(raw) : []; } catch { firmas = []; }

  firmas.unshift({
    empresaId: empresaActual.id,
    empresaNombre: empresaActual.nombre,
    estado: "FIRMADA",
    firmante: nombre,
    fecha: new Date().toLocaleString()
  });

  localStorage.setItem(KEY_FIRMAS, JSON.stringify(firmas));
  cargarFirmasDemo();

  alert(`Capacitación finalizada y firmada para ${empresaActual.nombre} (demo).`);
}

function cargarFirmasDemo() {
  const body = document.getElementById("tablaFirmasBody");
  if (!body) return;

  const raw = localStorage.getItem(KEY_FIRMAS);
  let firmas = [];
  try { firmas = raw ? JSON.parse(raw) : []; } catch { firmas = []; }

  if (!firmas.length) {
    body.innerHTML = `<tr><td colspan="5" class="text-white-50">Sin firmas todavía.</td></tr>`;
    return;
  }

  body.innerHTML = firmas.map((f, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(f.empresaNombre)}</td>
      <td><span class="pill completado">${escapeHtml(f.estado)}</span></td>
      <td>${escapeHtml(f.firmante)}</td>
      <td>${escapeHtml(f.fecha)}</td>
    </tr>
  `).join("");
}

function escapeHtml(texto) {
  return String(texto)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function escapeAttr(texto) {
  return String(texto).replace(/"/g, "&quot;");
}