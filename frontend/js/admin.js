const API_BASE_URL = "http://localhost:8081/api";

document.addEventListener("DOMContentLoaded", () => {
    validarSesion();
    cargarDatosUsuario();
    cargarResumenDashboard();
    cargarUsuarios();
    configurarLogout();
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
    const nombreAdmin = document.getElementById("nombreAdmin");

    if (nombreAdmin) {
        nombreAdmin.textContent = nombre;
    }
}

async function cargarResumenDashboard() {
    const token = localStorage.getItem("token");

    try {
        const response = await fetch(`${API_BASE_URL}/dashboard/resumen`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            }
        });

        const texto = await response.text();
        let data = {};

        try {
            data = JSON.parse(texto);
        } catch (e) {
            console.error("La respuesta del dashboard no es JSON válido:", texto);
            return;
        }

        if (!response.ok) {
            console.error("Error al cargar dashboard:", data);
            return;
        }

        setText("totalUsuarios", data.totalUsuarios);
        setText("totalEmpresas", data.totalEmpresas);

        const activos =
            (data.serviciosEnProceso || 0) +
            (data.serviciosFirmados || 0);

        setText("serviciosActivos", activos);
        setText("totalEvaluaciones", data.totalEvaluaciones);
    } catch (error) {
        console.error("Error cargando resumen del dashboard:", error);
    }
}

async function cargarUsuarios() {
    const token = localStorage.getItem("token");
    const tablaBody = document.getElementById("tablaUsuariosBody");

    try {
        const response = await fetch(`${API_BASE_URL}/usuarios`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            }
        });

        const texto = await response.text();
        let usuarios = [];

        try {
            usuarios = JSON.parse(texto);
        } catch (e) {
            console.error("La respuesta de usuarios no es JSON válido:", texto);
            tablaBody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center">Error al leer usuarios</td>
                </tr>
            `;
            return;
        }

        if (!response.ok) {
            console.error("Error al cargar usuarios:", usuarios);
            tablaBody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center">No se pudieron cargar los usuarios</td>
                </tr>
            `;
            return;
        }

        renderizarUsuarios(usuarios);
    } catch (error) {
        console.error("Error cargando usuarios:", error);
        tablaBody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center">Error de conexión al cargar usuarios</td>
            </tr>
        `;
    }
}

function renderizarUsuarios(usuarios) {
    const tablaBody = document.getElementById("tablaUsuariosBody");

    if (!usuarios || usuarios.length === 0) {
        tablaBody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center">No hay usuarios registrados</td>
            </tr>
        `;
        return;
    }

    tablaBody.innerHTML = usuarios.map((usuario, index) => {
        const nombre = usuario.nombre || "";
        const email = usuario.email || "";
        const rol = usuario.rol?.nombre || "Sin rol";
        const empresa = usuario.empresa?.nombre || "Sin empresa";

        return `
            <tr>
                <td>${index + 1}</td>
                <td>${escapeHtml(nombre)}</td>
                <td>${escapeHtml(email)}</td>
                <td><span class="badge-rol">${escapeHtml(rol)}</span></td>
                <td>${escapeHtml(empresa)}</td>
                <td><span class="badge-estado activo">Activo</span></td>
                <td>
                    <button class="btn btn-sm btn-accion-editar" title="Editar">
                        <i class="bi bi-pencil-fill"></i>
                    </button>
                    <button class="btn btn-sm btn-accion-eliminar" title="Eliminar">
                        <i class="bi bi-trash-fill"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join("");
}

function setText(id, valor) {
    const elemento = document.getElementById(id);
    if (elemento) {
        elemento.textContent = valor ?? 0;
    }
}

function configurarLogout() {
    const btnLogout = document.getElementById("btnLogout");

    if (btnLogout) {
        btnLogout.addEventListener("click", () => {
            localStorage.removeItem("token");
            localStorage.removeItem("nombre");
            localStorage.removeItem("email");
            localStorage.removeItem("rol");
            window.location.href = "login.html";
        });
    }
}

function escapeHtml(texto) {
    return String(texto)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}