const API_BASE_URL = "/api";
const SESSION_KEYS = ["token", "nombre", "email", "rol", "rolId", "permisosRol", "empresaId", "empresaNombre"];

function migrateLegacySession() {
  if (!sessionStorage.getItem("token") && localStorage.getItem("token")) {
    SESSION_KEYS.forEach(key => {
      const value = localStorage.getItem(key);
      if (value != null) sessionStorage.setItem(key, value);
      localStorage.removeItem(key);
    });
  }
}

migrateLegacySession();

document.addEventListener("DOMContentLoaded", () => {
  if (sessionStorage.getItem("token")) return routeByRole(sessionStorage.getItem("rol"));
  const form = document.getElementById("loginForm");
  const errorBox = document.getElementById("mensajeError");
  const button = document.getElementById("loginButton");
  const password = document.getElementById("password");

  document.getElementById("togglePassword").addEventListener("click", e => {
    password.type = password.type === "password" ? "text" : "password";
    e.currentTarget.innerHTML = `<i class="bi bi-${password.type === "password" ? "eye" : "eye-slash"}"></i>`;
  });

  document.getElementById("verifyButton").addEventListener("click", verifyCertificate);
  document.getElementById("verifyCode").addEventListener("keydown", event => {
    if (event.key === "Enter") { event.preventDefault(); verifyCertificate(); }
  });

  form.addEventListener("submit", async event => {
    event.preventDefault();
    hideError();
    const email = document.getElementById("email").value.trim();
    const pass = password.value;
    if (!email || !pass) return showError("Ingrese el correo y la contraseña.");
    button.disabled = true;
    button.innerHTML = '<span class="spinner-border spinner-border-sm"></span><span>Validando...</span>';
    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: "POST",
        headers: {"Content-Type":"application/json", "Accept":"application/json"},
        cache: "no-store",
        body: JSON.stringify({email, password:pass})
      });
      const raw = await response.text();
      let data; try { data = JSON.parse(raw); } catch { data = {message:raw}; }
      if (!response.ok) throw new Error(data.message || data.error || raw || "Credenciales inválidas");

      SESSION_KEYS.forEach(key => localStorage.removeItem(key));
      sessionStorage.setItem("token", data.token);
      sessionStorage.setItem("nombre", data.nombre || "");
      sessionStorage.setItem("email", data.email || email);
      sessionStorage.setItem("rol", data.rol || "");
      sessionStorage.setItem("rolId", String(data.rolId || ""));
      sessionStorage.setItem("permisosRol", data.permisos || "{}");
      if (data.empresaId != null) sessionStorage.setItem("empresaId", String(data.empresaId));
      if (data.empresaNombre) sessionStorage.setItem("empresaNombre", data.empresaNombre);
      routeByRole(data.rol);
    } catch (error) {
      showError(error.message || "No fue posible conectarse con el servidor.");
      button.disabled = false;
      button.innerHTML = '<span>Ingresar</span><i class="bi bi-arrow-right"></i>';
    }
  });

  function showError(message) { errorBox.textContent = message; errorBox.style.display = "block"; }
  function hideError() { errorBox.style.display = "none"; }
});

async function verifyCertificate() {
  const input = document.getElementById("verifyCode");
  const button = document.getElementById("verifyButton");
  const result = document.getElementById("verifyResult");
  const code = input.value.trim();
  if (!code) {
    result.className = "verify-result error";
    result.textContent = "Ingrese el código de verificación.";
    return;
  }
  button.disabled = true;
  result.className = "verify-result";
  result.textContent = "Consultando...";
  try {
    const response = await fetch(`${API_BASE_URL}/constancias-capacitacion/verificar/${encodeURIComponent(code)}`, {cache:"no-store"});
    const raw = await response.text();
    let data; try { data = JSON.parse(raw); } catch { data = {message:raw}; }
    if (!response.ok) throw new Error(data.message || "No se encontró la constancia");
    result.className = `verify-result ${data.valida ? "ok" : "warn"}`;
    result.innerHTML = `<strong>${escapeText(data.valida ? "Constancia vigente" : "Constancia no vigente")}</strong><span>${escapeText(data.nombreCompleto)} · ${escapeText(data.capacitacion)}</span><small>${escapeText(data.empresa)} · ${formatDate(data.fechaEmision)}</small>`;
  } catch (error) {
    result.className = "verify-result error";
    result.textContent = error.message || "No fue posible verificar la constancia.";
  } finally {
    button.disabled = false;
  }
}

function escapeText(value) {
  const div = document.createElement("div");
  div.textContent = String(value ?? "");
  return div.innerHTML;
}

function formatDate(value) {
  if (!value) return "Fecha no disponible";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? escapeText(value) : date.toLocaleDateString("es-CO");
}

function routeByRole(value) {
  const role = String(value || "").toUpperCase();
  if (role.includes("ADMIN")) return location.replace("admin.html");
  if (role.includes("IMPLEMENTADOR")) return location.replace("implementador.html");
  if (role.includes("AUDITOR")) return location.replace("auditor.html");
  if (role.includes("CAPACITADOR")) return location.replace("capacitador.html");
  return location.replace("empresa.html");
}
