const API_BASE_URL = "http://localhost:8081/api";

let cacheUsuarios = [];
let cacheEmpresas = [];
let cacheRoles = [];

let modalEditarUsuario = null;
let modalCrearUsuario = null;
let modalCrearEmpresa = null;

const CONFIG_KEY = "globaliso_admin_config_v1";

document.addEventListener("DOMContentLoaded", () => {
    validarSesion();
    cargarDatosUsuario();
    configurarLogout();

    prepararVistaInicial();

    configurarModalUsuario();
    configurarModalCrearUsuario();
    configurarModalCrearEmpresa();

    configurarReportes();
    configurarConfiguracion();

    cargarResumenDashboard();
    cargarUsuarios();
    cargarEmpresas();
    cargarRoles();
});

function validarSesion() {
    const token = localStorage.getItem("token");
    const rol = (localStorage.getItem("rol") || "").toUpperCase();

    if (!token) {
        window.location.href = "login.html";
        return;
    }

    if (!rol.includes("ADMIN")) {
        alert("No tienes permisos para acceder a este panel");
        window.location.href = "login.html";
    }
}

function cargarDatosUsuario() {
    const nombre = localStorage.getItem("nombre") || "Administrador";
    setText("nombreAdmin", nombre);
}

function prepararVistaInicial() {
    setText("totalUsuarios", "Cargando...");
    setText("totalEmpresas", "Cargando...");
    setText("totalEvaluaciones", "Cargando...");

    setText("usuariosTotalResumen", "Cargando...");
    setText("empresasResumen", "Cargando...");

    setText("ultimaAuditoria", "Cargando...");
    setText("ultimoReporte", "Cargando...");
    setText("estadoSistema", "Operativo");

    setText("usuariosTotalSeccion", "Cargando...");
    setText("eventosRecientes", "Pendiente");

    setText("empresasRegistradas", "Cargando...");
    setText("serviciosTotalesEmpresas", "Cargando...");
    setText("totalImplementadores", "Cargando...");
    setText("totalCapacitadores", "Cargando...");
    setText("totalAuditores", "Cargando...");

    const tablaUsuariosBody = document.getElementById("tablaUsuariosBody");
    if (tablaUsuariosBody) {
        tablaUsuariosBody.innerHTML = `<tr><td colspan="6" class="text-center">Cargando usuarios...</td></tr>`;
    }

    const tablaEmpresasBody = document.getElementById("tablaEmpresasBody");
    if (tablaEmpresasBody) {
        tablaEmpresasBody.innerHTML = `<tr><td colspan="9" class="text-center">Cargando empresas...</td></tr>`;
    }
}

/* =========================
   MODALES
========================= */

function configurarModalUsuario() {
    const modalElement = document.getElementById("modalEditarUsuario");
    if (modalElement) modalEditarUsuario = new bootstrap.Modal(modalElement);

    const btnGuardar = document.getElementById("btnGuardarUsuario");
    if (btnGuardar) btnGuardar.addEventListener("click", guardarEdicionUsuario);
}

function configurarModalCrearUsuario() {
    const modalElement = document.getElementById("modalCrearUsuario");
    if (modalElement) modalCrearUsuario = new bootstrap.Modal(modalElement);

    const btnAbrir = document.getElementById("btnAbrirModalCrearUsuario");
    if (btnAbrir) {
        btnAbrir.addEventListener("click", async () => {
            await prepararFormularioCrearUsuario();
            ocultarErrorCrearUsuario();
            if (modalCrearUsuario) modalCrearUsuario.show();
        });
    }

    const btnGuardar = document.getElementById("btnGuardarNuevoUsuario");
    if (btnGuardar) btnGuardar.addEventListener("click", guardarNuevoUsuario);
}

function configurarModalCrearEmpresa() {
    const modalElement = document.getElementById("modalCrearEmpresa");
    if (modalElement) modalCrearEmpresa = new bootstrap.Modal(modalElement);

    const btnAbrir = document.getElementById("btnAbrirModalCrearEmpresa");
    if (btnAbrir) {
        btnAbrir.addEventListener("click", () => {
            setValue("crearEmpresaNombre", "");
            setValue("crearEmpresaSector", "");
            ocultarErrorCrearEmpresa();
            if (modalCrearEmpresa) modalCrearEmpresa.show();
        });
    }

    const btnGuardar = document.getElementById("btnGuardarNuevaEmpresa");
    if (btnGuardar) btnGuardar.addEventListener("click", guardarNuevaEmpresa);
}

/* =========================
   DASHBOARD
========================= */

async function cargarResumenDashboard() {
    const token = localStorage.getItem("token");

    try {
        const response = await fetch(`${API_BASE_URL}/dashboard/resumen`, {
            method: "GET",
            headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` }
        });

        const texto = await response.text();
        let data = {};
        try {
            data = texto ? JSON.parse(texto) : {};
        } catch {
            mostrarErrorDashboard();
            return;
        }

        if (!response.ok) {
            mostrarErrorDashboard();
            return;
        }

        const totalUsuarios = toNumber(data.totalUsuarios);
        const totalEmpresas = toNumber(data.totalEmpresas);
        const totalEvaluaciones = toNumber(data.totalEvaluaciones);

        setText("totalUsuarios", totalUsuarios);
        setText("totalEmpresas", totalEmpresas);
        setText("totalEvaluaciones", totalEvaluaciones);

        setText("usuariosTotalResumen", totalUsuarios);
        setText("empresasResumen", totalEmpresas);

        setText("ultimaAuditoria", valorODefecto(data.ultimaAuditoria, "No disponible"));
        setText("ultimoReporte", valorODefecto(data.ultimoReporte, "No disponible"));
        setText("estadoSistema", valorODefecto(data.estadoSistema, "Operativo"));

        setText("usuariosTotalSeccion", totalUsuarios);
        setText("eventosRecientes", valorODefecto(data.eventosRecientes, "Pendiente"));
    } catch (error) {
        console.error("Error cargando resumen del dashboard:", error);
        mostrarErrorDashboard();
    }
}

function mostrarErrorDashboard() {
    setText("totalUsuarios", "Error");
    setText("totalEmpresas", "Error");
    setText("totalEvaluaciones", "Error");
    setText("usuariosTotalResumen", "Error");
    setText("empresasResumen", "Error");
    setText("ultimaAuditoria", "Error");
    setText("ultimoReporte", "Error");
}

/* =========================
   USUARIOS
========================= */

async function cargarUsuarios() {
    const token = localStorage.getItem("token");
    const tablaBody = document.getElementById("tablaUsuariosBody");
    if (!tablaBody) return;

    try {
        const response = await fetch(`${API_BASE_URL}/usuarios`, {
            method: "GET",
            headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` }
        });

        const texto = await response.text();
        let usuarios = [];
        try {
            usuarios = texto ? JSON.parse(texto) : [];
        } catch {
            tablaBody.innerHTML = `<tr><td colspan="6" class="text-center">Error al leer usuarios</td></tr>`;
            return;
        }

        if (!response.ok) {
            tablaBody.innerHTML = `<tr><td colspan="6" class="text-center">No se pudieron cargar los usuarios</td></tr>`;
            return;
        }

        cacheUsuarios = Array.isArray(usuarios) ? usuarios : [];
        renderizarUsuarios(cacheUsuarios);
        setText("usuariosTotalSeccion", cacheUsuarios.length);
    } catch (error) {
        console.error("Error cargando usuarios:", error);
        tablaBody.innerHTML = `<tr><td colspan="6" class="text-center">Error de conexión al cargar usuarios</td></tr>`;
    }
}

function renderizarUsuarios(usuarios) {
    const tablaBody = document.getElementById("tablaUsuariosBody");
    if (!tablaBody) return;

    if (!usuarios || usuarios.length === 0) {
        tablaBody.innerHTML = `<tr><td colspan="6" class="text-center">No hay usuarios registrados</td></tr>`;
        return;
    }

    tablaBody.innerHTML = usuarios
        .map((usuario, index) => {
            const id = Number(usuario.id);
            const nombre = usuario.nombre || "Sin nombre";
            const email = usuario.email || "Sin email";
            const rol = usuario.rol?.nombre || usuario.rol || "Sin rol";

            let empresa = "Sin empresa";
            if (Array.isArray(usuario.empresas)) {
                empresa =
                    usuario.empresas
                        .map((e) => e?.nombre || e)
                        .filter(Boolean)
                        .join(", ") || "Sin empresa";
            } else {
                empresa = usuario.empresa?.nombre || usuario.empresa || "Sin empresa";
            }

            return `
                <tr>
                    <td>${index + 1}</td>
                    <td>${escapeHtml(nombre)}</td>
                    <td>${escapeHtml(email)}</td>
                    <td><span class="badge-rol ${claseRol(rol)}">${escapeHtml(rol)}</span></td>
                    <td>${escapeHtml(empresa)}</td>
                    <td>
                        <button class="btn btn-sm btn-accion-editar" title="Editar" onclick="abrirEditarUsuario(${id})">
                            <i class="bi bi-pencil-fill"></i>
                        </button>
                        <button class="btn btn-sm btn-accion-eliminar" title="Eliminar" onclick="eliminarUsuario(${id}, '${escapeJs(nombre)}')">
                            <i class="bi bi-trash-fill"></i>
                        </button>
                    </td>
                </tr>
            `;
        })
        .join("");
}

async function abrirEditarUsuario(id) {
    const usuario = cacheUsuarios.find((u) => Number(u.id) === Number(id));
    if (!usuario) {
        alert("No se encontró el usuario");
        return;
    }

    await cargarRoles();
    if (cacheEmpresas.length === 0) await cargarEmpresas();

    setValue("editUsuarioId", usuario.id || "");
    setValue("editNombre", usuario.nombre || "");
    setValue("editEmail", usuario.email || "");
    setValue("editPassword", "");

    const selectRol = document.getElementById("editRol");
    const rolActualId = usuario.rol_id || usuario.rol?.id || "";

    if (selectRol) {
        selectRol.innerHTML =
            `<option value="">Selecciona un rol</option>` +
            cacheRoles
                .map(
                    (rol) => `
                <option value="${rol.id}" ${String(rol.id) === String(rolActualId) ? "selected" : ""}>
                    ${escapeHtml(rol.nombre)}
                </option>
            `
                )
                .join("");
    }

    const selectEmpresa = document.getElementById("editEmpresa");
    const empresaActualId = usuario.empresa_id || usuario.empresa?.id || "";

    if (selectEmpresa) {
        selectEmpresa.innerHTML =
            `<option value="">Selecciona una empresa</option>` +
            cacheEmpresas
                .map(
                    (empresa) => `
                <option value="${empresa.id}" ${String(empresa.id) === String(empresaActualId) ? "selected" : ""}>
                    ${escapeHtml(empresa.nombre)}
                </option>
            `
                )
                .join("");
    }

    ocultarErrorEditarUsuario();
    if (modalEditarUsuario) modalEditarUsuario.show();
}

async function guardarEdicionUsuario() {
    const token = localStorage.getItem("token");

    const id = getValue("editUsuarioId");
    const nombre = getValue("editNombre").trim();
    const email = getValue("editEmail").trim();
    const password = getValue("editPassword").trim();
    const rolId = getValue("editRol");
    const empresaId = getValue("editEmpresa");

    if (!nombre || !email) {
        mostrarErrorEditarUsuario("Nombre y correo son obligatorios.");
        return;
    }

    const payload = {
        nombre,
        email,
        rol_id: rolId ? Number(rolId) : null,
        empresa_id: empresaId ? Number(empresaId) : null
    };

    if (password) payload.rawPassword = password;

    try {
        const response = await fetch(`${API_BASE_URL}/usuarios/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
            body: JSON.stringify(payload)
        });

        const texto = await response.text();
        let data = {};
        try {
            data = texto ? JSON.parse(texto) : {};
        } catch {}

        if (!response.ok) {
            console.error("PUT usuarios", response.status, texto);
            mostrarErrorEditarUsuario(data.message || data.error || texto || "No se pudo actualizar el usuario.");
            return;
        }

        if (modalEditarUsuario) modalEditarUsuario.hide();

        alert("Usuario actualizado correctamente");
        await cargarUsuarios();
        await cargarResumenDashboard();
    } catch (error) {
        console.error("Error guardando usuario:", error);
        mostrarErrorEditarUsuario("Error de conexión al actualizar el usuario.");
    }
}

async function eliminarUsuario(id, nombre) {
    const token = localStorage.getItem("token");

    const confirmado = confirm(
        `Vas a eliminar al usuario "${nombre}".\n\nEsta acción no se puede deshacer.\n\n¿Deseas continuar?`
    );
    if (!confirmado) return;

    try {
        const response = await fetch(`${API_BASE_URL}/usuarios/${id}`, {
            method: "DELETE",
            headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` }
        });

        const texto = await response.text();
        let data = {};
        try {
            data = texto ? JSON.parse(texto) : {};
        } catch {}

        if (!response.ok) {
            alert(data.message || texto || "No se pudo eliminar el usuario");
            return;
        }

        alert("Usuario eliminado correctamente");
        await cargarUsuarios();
        await cargarResumenDashboard();
    } catch (error) {
        console.error("Error eliminando usuario:", error);
        alert("Error de conexión al eliminar el usuario");
    }
}

async function prepararFormularioCrearUsuario() {
    if (cacheRoles.length === 0) await cargarRoles();
    if (cacheEmpresas.length === 0) await cargarEmpresas();

    setValue("crearNombre", "");
    setValue("crearEmail", "");
    setValue("crearPassword", "");

    const selectRol = document.getElementById("crearRol");
    if (selectRol) {
        selectRol.innerHTML =
            `<option value="">Selecciona un rol</option>` +
            cacheRoles.map((rol) => `<option value="${rol.id}">${escapeHtml(rol.nombre)}</option>`).join("");
    }

    const selectEmpresa = document.getElementById("crearEmpresa");
    if (selectEmpresa) {
        selectEmpresa.innerHTML =
            `<option value="">Selecciona una empresa</option>` +
            cacheEmpresas.map((empresa) => `<option value="${empresa.id}">${escapeHtml(empresa.nombre)}</option>`).join("");
    }
}

/* Crear usuario: depende del backend actual */
async function guardarNuevoUsuario() {
    const token = localStorage.getItem("token");

    const nombre = getValue("crearNombre").trim();
    const email = getValue("crearEmail").trim();
    const password = getValue("crearPassword").trim();
    const rolId = getValue("crearRol");
    const empresaId = getValue("crearEmpresa");

    if (!nombre || !email || !password || !rolId || !empresaId) {
        mostrarErrorCrearUsuario("Todos los campos son obligatorios.");
        return;
    }

    const payload = {
        nombre,
        email,
        rol_id: Number(rolId),
        empresa_id: Number(empresaId),
        rawPassword: password
    };

    try {
        const response = await fetch(`${API_BASE_URL}/usuarios`, {
            method: "POST",
            headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
            body: JSON.stringify(payload)
        });

        const texto = await response.text();
        let data = {};
        try {
            data = texto ? JSON.parse(texto) : {};
        } catch {}

        if (!response.ok) {
            console.error("POST usuarios", response.status, texto);
            mostrarErrorCrearUsuario(data.message || data.error || texto || "No se pudo crear el usuario.");
            return;
        }

        if (modalCrearUsuario) modalCrearUsuario.hide();

        alert("Usuario creado correctamente");
        await cargarUsuarios();
        await cargarResumenDashboard();
    } catch (error) {
        console.error("Error creando usuario:", error);
        mostrarErrorCrearUsuario("Error de conexión al crear el usuario.");
    }
}

/* =========================
   EMPRESAS
========================= */

async function cargarEmpresas() {
    const token = localStorage.getItem("token");
    const tablaEmpresasBody = document.getElementById("tablaEmpresasBody");
    if (!tablaEmpresasBody) return;

    const endpoints = [
        `${API_BASE_URL}/empresas`,
        `${API_BASE_URL}/admin/empresas`,
        `${API_BASE_URL}/dashboard/empresas`
    ];

    for (const endpoint of endpoints) {
        try {
            const response = await fetch(endpoint, {
                method: "GET",
                headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` }
            });

            const texto = await response.text();
            let empresas = [];

            try {
                empresas = texto ? JSON.parse(texto) : [];
            } catch {
                continue;
            }

            if (!response.ok || !Array.isArray(empresas)) continue;

            cacheEmpresas = empresas;
            renderizarEmpresas(empresas);
            return;
        } catch (error) {
            console.error(`Error cargando empresas desde ${endpoint}:`, error);
        }
    }

    tablaEmpresasBody.innerHTML = `<tr><td colspan="9" class="text-center">No se pudieron cargar las empresas desde la API.</td></tr>`;
}

function renderizarEmpresas(empresas) {
    const tablaEmpresasBody = document.getElementById("tablaEmpresasBody");
    if (!tablaEmpresasBody) return;

    if (!empresas || empresas.length === 0) {
        tablaEmpresasBody.innerHTML = `<tr><td colspan="9" class="text-center">No hay empresas registradas</td></tr>`;
        setText("empresasRegistradas", 0);
        setText("serviciosTotalesEmpresas", 0);
        setText("totalImplementadores", 0);
        setText("totalCapacitadores", 0);
        setText("totalAuditores", 0);
        return;
    }

    const implementadores = new Set();
    const capacitadores = new Set();
    const auditores = new Set();

    tablaEmpresasBody.innerHTML = empresas
        .map((empresa, index) => {
            const id = Number(empresa.id);
            const nombre = empresa.nombre || "Sin nombre";
            const sector = empresa.sector?.nombre || empresa.sector_nombre || empresa.sector || "Sin sector";
            const totalServicios = empresa.totalServicios ?? empresa.servicios ?? 0;
            const implementador = empresa.implementador?.nombre || empresa.implementador || "No asignado";
            const capacitador = empresa.capacitador?.nombre || empresa.capacitador || "No asignado";
            const auditor = empresa.auditor?.nombre || empresa.auditor || "No asignado";
            const estadoGeneral = empresa.estadoGeneral || empresa.estado || "No definido";

            if (implementador !== "No asignado") implementadores.add(implementador);
            if (capacitador !== "No asignado") capacitadores.add(capacitador);
            if (auditor !== "No asignado") auditores.add(auditor);

            return `
                <tr>
                    <td>${index + 1}</td>
                    <td>${escapeHtml(nombre)}</td>
                    <td>${escapeHtml(sector)}</td>
                    <td>${escapeHtml(String(totalServicios))}</td>
                    <td><span class="badge-rol implementador">${escapeHtml(implementador)}</span></td>
                    <td><span class="badge-rol capacitador">${escapeHtml(capacitador)}</span></td>
                    <td><span class="badge-rol auditor">${escapeHtml(auditor)}</span></td>
                    <td>${escapeHtml(estadoGeneral)}</td>
                    <td>
                        <button class="btn btn-sm btn-accion-editar" title="Editar" onclick="editarEmpresa(${id})">
                            <i class="bi bi-pencil-fill"></i>
                        </button>
                        <button class="btn btn-sm btn-accion-eliminar" title="Eliminar" onclick="eliminarEmpresa(${id}, '${escapeJs(nombre)}')">
                            <i class="bi bi-trash-fill"></i>
                        </button>
                    </td>
                </tr>
            `;
        })
        .join("");

    setText("empresasRegistradas", empresas.length);
    setText(
        "serviciosTotalesEmpresas",
        empresas.reduce((acc, e) => acc + toNumber(e.totalServicios ?? e.servicios ?? 0), 0)
    );
    setText("totalImplementadores", implementadores.size);
    setText("totalCapacitadores", capacitadores.size);
    setText("totalAuditores", auditores.size);
}

async function guardarNuevaEmpresa() {
    const token = localStorage.getItem("token");

    const nombre = getValue("crearEmpresaNombre").trim();
    const sector = getValue("crearEmpresaSector").trim();

    if (!nombre) {
        mostrarErrorCrearEmpresa("El nombre de la empresa es obligatorio.");
        return;
    }

    const payload = { nombre, sector };

    try {
        const response = await fetch(`${API_BASE_URL}/empresas`, {
            method: "POST",
            headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
            body: JSON.stringify(payload)
        });

        const texto = await response.text();
        let data = {};
        try {
            data = texto ? JSON.parse(texto) : {};
        } catch {}

        if (!response.ok) {
            console.error("POST empresas", response.status, texto);
            mostrarErrorCrearEmpresa(data.message || data.error || texto || "No se pudo crear la empresa.");
            return;
        }

        if (modalCrearEmpresa) modalCrearEmpresa.hide();

        alert("Empresa creada correctamente");
        await cargarEmpresas();
        await cargarResumenDashboard();
    } catch (error) {
        console.error("Error creando empresa:", error);
        mostrarErrorCrearEmpresa("Error de conexión al crear la empresa.");
    }
}

async function editarEmpresa(id) {
    alert(`Edición de empresa ID ${id} pendiente de implementar.`);
}

async function eliminarEmpresa(id, nombre) {
    const token = localStorage.getItem("token");

    const confirmado = confirm(
        `Vas a eliminar la empresa "${nombre}".\n\nEsta acción no se puede deshacer.\n\n¿Deseas continuar?`
    );
    if (!confirmado) return;

    try {
        const response = await fetch(`${API_BASE_URL}/empresas/${id}`, {
            method: "DELETE",
            headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` }
        });

        const texto = await response.text();
        let data = {};
        try {
            data = texto ? JSON.parse(texto) : {};
        } catch {}

        if (!response.ok) {
            alert(data.message || texto || "No se pudo eliminar la empresa");
            return;
        }

        alert("Empresa eliminada correctamente");
        await cargarEmpresas();
        await cargarResumenDashboard();
    } catch (error) {
        console.error("Error eliminando empresa:", error);
        alert("Error de conexión al eliminar la empresa");
    }
}

/* =========================
   ROLES
========================= */

async function cargarRoles() {
    if (cacheRoles.length > 0) return cacheRoles;

    const token = localStorage.getItem("token");
    const endpoints = [`${API_BASE_URL}/roles`, `${API_BASE_URL}/admin/roles`];

    for (const endpoint of endpoints) {
        try {
            const response = await fetch(endpoint, {
                method: "GET",
                headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` }
            });

            const texto = await response.text();
            let roles = [];

            try {
                roles = texto ? JSON.parse(texto) : [];
            } catch {
                continue;
            }

            if (response.ok && Array.isArray(roles) && roles.length > 0) {
                cacheRoles = roles;
                return roles;
            }
        } catch (error) {
            console.error(`Error cargando roles desde ${endpoint}:`, error);
        }
    }

    cacheRoles = [
        { id: 1, nombre: "ADMINISTRADOR" },
        { id: 2, nombre: "IMPLEMENTADOR" },
        { id: 3, nombre: "AUDITOR" },
        { id: 4, nombre: "CAPACITADOR" },
        { id: 5, nombre: "USUARIO" }
    ];
    return cacheRoles;
}

/* =========================
   REPORTES (3 botones)
========================= */

function configurarReportes() {
    const btnA = document.getElementById("btnReporteAuditoria");
    const btnI = document.getElementById("btnReporteImplementador");
    const btnC = document.getElementById("btnReporteCapacitador");

    if (btnA) btnA.addEventListener("click", () => descargarReporte("auditoria"));
    if (btnI) btnI.addEventListener("click", () => descargarReporte("implementador"));
    if (btnC) btnC.addEventListener("click", () => descargarReporte("capacitador"));
}

function descargarReporte(tipo) {
    const ahora = new Date();
    const iso = ahora.toISOString().slice(0, 19).replaceAll(":", "-");
    const nombreArchivo = `reporte_${tipo}_${iso}.json`;

    const payload = {
        tipo,
        generadoEn: ahora.toISOString(),
        usuario: {
            nombre: localStorage.getItem("nombre") || "Administrador",
            email: localStorage.getItem("email") || null,
            rol: localStorage.getItem("rol") || null
        },
        resumen: {
            totalUsuarios: cacheUsuarios.length,
            totalEmpresas: cacheEmpresas.length
        },
        nota: "Reporte demo generado desde frontend (sin backend de reportes)."
    };

    downloadBlob(JSON.stringify(payload, null, 2), nombreArchivo, "application/json");
    setText("reporteEstado", `Último reporte generado: ${nombreArchivo}`);
}

/* =========================
   CONFIGURACIÓN (localStorage)
========================= */

function configurarConfiguracion() {
    aplicarConfiguracionDesdeStorage();

    const btnGuardar = document.getElementById("btnGuardarConfiguracion");
    if (btnGuardar) btnGuardar.addEventListener("click", guardarConfiguracionEnStorage);
}

function leerConfiguracionUI() {
    return {
        nombreSistema: getValue("cfgNombreSistema").trim(),
        version: getValue("cfgVersion").trim(),
        soporteEmail: getValue("cfgSoporteEmail").trim(),
        estadoSistema: getValue("cfgEstadoSistema"),

        sesionDuracionMin: toNumber(getValue("cfgSesionDuracion")),
        intentosMax: toNumber(getValue("cfgIntentosMax")),
        bloqueoMin: toNumber(getValue("cfgBloqueoMin")),
        sesionAuto: isChecked("sesionAuto"),

        flujo: {
            editarBorrador: isChecked("editarBorrador"),
            editarProceso: isChecked("editarProceso"),
            bloquearFirmado: isChecked("bloquearFirmado"),
            bloquearCerrado: isChecked("bloquearCerrado")
        },

        reportes: {
            pdfActivo: isChecked("pdfActivo"),
            excelActivo: isChecked("excelActivo"),
            logoReporte: isChecked("logoReporte"),
            soloCerrado: isChecked("soloCerrado")
        }
    };
}

function escribirConfiguracionUI(cfg) {
    if (!cfg) return;

    setValue("cfgNombreSistema", cfg.nombreSistema ?? "Global ISO Security");
    setValue("cfgVersion", cfg.version ?? "1.0.0");
    setValue("cfgSoporteEmail", cfg.soporteEmail ?? "soporte@globaliso.com");
    setValue("cfgEstadoSistema", cfg.estadoSistema ?? "Operativo");

    setValue("cfgSesionDuracion", String(cfg.sesionDuracionMin ?? 30));
    setValue("cfgIntentosMax", String(cfg.intentosMax ?? 5));
    setValue("cfgBloqueoMin", String(cfg.bloqueoMin ?? 10));

    setChecked("sesionAuto", cfg.sesionAuto ?? true);

    setChecked("editarBorrador", cfg.flujo?.editarBorrador ?? true);
    setChecked("editarProceso", cfg.flujo?.editarProceso ?? true);
    setChecked("bloquearFirmado", cfg.flujo?.bloquearFirmado ?? true);
    setChecked("bloquearCerrado", cfg.flujo?.bloquearCerrado ?? true);

    setChecked("pdfActivo", cfg.reportes?.pdfActivo ?? true);
    setChecked("excelActivo", cfg.reportes?.excelActivo ?? true);
    setChecked("logoReporte", cfg.reportes?.logoReporte ?? true);
    setChecked("soloCerrado", cfg.reportes?.soloCerrado ?? true);
}

function guardarConfiguracionEnStorage() {
    const cfg = leerConfiguracionUI();
    localStorage.setItem(CONFIG_KEY, JSON.stringify(cfg));
    setText("configEstado", `Configuración guardada (${new Date().toLocaleString()}).`);
    setText("estadoSistema", cfg.estadoSistema || "Operativo");
}

function aplicarConfiguracionDesdeStorage() {
    const raw = localStorage.getItem(CONFIG_KEY);
    if (!raw) {
        setText("configEstado", "No hay configuración guardada (usando valores por defecto).");
        return;
    }

    try {
        const cfg = JSON.parse(raw);
        escribirConfiguracionUI(cfg);
        setText("configEstado", `Configuración cargada (${new Date().toLocaleString()}).`);
        setText("estadoSistema", cfg.estadoSistema || "Operativo");
    } catch {
        setText("configEstado", "No se pudo leer la configuración guardada.");
    }
}

/* =========================
   LOGOUT
========================= */

function configurarLogout() {
    const btnLogout = document.getElementById("btnLogout");
    if (!btnLogout) return;

    btnLogout.addEventListener("click", () => {
        localStorage.removeItem("token");
        localStorage.removeItem("nombre");
        localStorage.removeItem("email");
        localStorage.removeItem("rol");
        window.location.href = "login.html";
    });
}

/* =========================
   ERRORES UI
========================= */

function mostrarErrorEditarUsuario(mensaje) {
    const error = document.getElementById("editUsuarioError");
    if (error) {
        error.textContent = mensaje;
        error.style.display = "block";
    }
}

function ocultarErrorEditarUsuario() {
    const error = document.getElementById("editUsuarioError");
    if (error) {
        error.textContent = "";
        error.style.display = "none";
    }
}

function mostrarErrorCrearUsuario(mensaje) {
    const error = document.getElementById("crearUsuarioError");
    if (error) {
        error.textContent = mensaje;
        error.style.display = "block";
    }
}

function ocultarErrorCrearUsuario() {
    const error = document.getElementById("crearUsuarioError");
    if (error) {
        error.textContent = "";
        error.style.display = "none";
    }
}

function mostrarErrorCrearEmpresa(mensaje) {
    const error = document.getElementById("crearEmpresaError");
    if (error) {
        error.textContent = mensaje;
        error.style.display = "block";
    }
}

function ocultarErrorCrearEmpresa() {
    const error = document.getElementById("crearEmpresaError");
    if (error) {
        error.textContent = "";
        error.style.display = "none";
    }
}

/* =========================
   HELPERS
========================= */

function setText(id, valor) {
    const elemento = document.getElementById(id);
    if (elemento) elemento.textContent = valor ?? "0";
}

function setValue(id, valor) {
    const elemento = document.getElementById(id);
    if (elemento) elemento.value = valor ?? "";
}

function getValue(id) {
    const elemento = document.getElementById(id);
    return elemento ? elemento.value : "";
}

function isChecked(id) {
    const el = document.getElementById(id);
    return !!(el && el.checked);
}

function setChecked(id, checked) {
    const el = document.getElementById(id);
    if (el) el.checked = !!checked;
}

function toNumber(valor) {
    const numero = Number(valor);
    return Number.isFinite(numero) ? numero : 0;
}

function valorODefecto(valor, defecto) {
    return valor === null || valor === undefined || valor === "" ? defecto : valor;
}

function claseRol(rol) {
    const valor = String(rol || "").toLowerCase();
    if (valor.includes("implementador")) return "implementador";
    if (valor.includes("capacitador")) return "capacitador";
    if (valor.includes("auditor")) return "auditor";
    if (valor.includes("administrador")) return "administrador";
    if (valor.includes("usuario")) return "usuario";
    return "";
}

function escapeHtml(texto) {
    return String(texto)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function escapeJs(texto) {
    return String(texto).replace(/\\/g, "\\\\").replace(/'/g, "\\'");
}

function downloadBlob(content, filename, mimeType) {
    const blob = new Blob([content], { type: mimeType || "application/octet-stream" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
}