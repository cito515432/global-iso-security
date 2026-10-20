const API_BASE_URL = "/api";
const SESSION_KEYS = ["token", "nombre", "email", "rol", "rolId", "permisosRol", "empresaId", "empresaNombre"];

function clearSession() {
  SESSION_KEYS.forEach(key => {
    sessionStorage.removeItem(key);
    localStorage.removeItem(key);
  });
}

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

document.addEventListener("DOMContentLoaded", async () => {
  const form = document.getElementById("loginForm");
  const errorBox = document.getElementById("mensajeError");
  const button = document.getElementById("loginButton");
  const password = document.getElementById("password");
  const credentialsPanel = document.getElementById("credentialsPanel");
  const mfaPanel = document.getElementById("mfaPanel");
  const mfaSetup = document.getElementById("mfaSetup");
  const mfaCode = document.getElementById("mfaCode");
  const recoveryPanel = document.getElementById("recoveryPanel");
  const accountRecoveryPanel = document.getElementById("accountRecoveryPanel");
  const recoveryEmail = document.getElementById("recoveryEmail");
  const recoveryCode = document.getElementById("recoveryCode");
  const recoveryLink = document.getElementById("recoveryLink");
  const cancelRecoveryButton = document.getElementById("cancelRecoveryButton");
  const startRecoveryButton = document.getElementById("startRecoveryButton");
  const verifyRecoveryButton = document.getElementById("verifyRecoveryButton");
  const recoveryStatus = document.getElementById("recoveryStatus");
  let challengeToken = null;
  let pendingEmail = "";
  let pendingAuthentication = null;

  if (sessionStorage.getItem("token")) {
    try {
      const response = await fetch(`${API_BASE_URL}/usuarios/me`, {
        headers: {"Authorization": `Bearer ${sessionStorage.getItem("token")}`},
        cache: "no-store"
      });
      if (response.ok) return routeByRole(sessionStorage.getItem("rol"));
    } catch { /* Mantener la pantalla de acceso si no hay red. */ }
    clearSession();
  }

  document.getElementById("togglePassword").addEventListener("click", event => {
    password.type = password.type === "password" ? "text" : "password";
    event.currentTarget.innerHTML = `<i class="bi bi-${password.type === "password" ? "eye" : "eye-slash"}"></i>`;
  });
  document.getElementById("verifyButton").addEventListener("click", verifyCertificate);
  document.getElementById("verifyCode").addEventListener("keydown", event => {
    if (event.key === "Enter") { event.preventDefault(); verifyCertificate(); }
  });

  recoveryLink.addEventListener("click", () => {
    accountRecoveryPanel.hidden = false;
    credentialsPanel.hidden = true;
    mfaPanel.hidden = true;
    recoveryPanel.hidden = true;
    button.hidden = true;
    recoveryEmail.value = document.getElementById("email").value.trim();
    recoveryCode.value = "";
    recoveryStatus.textContent = "Ingrese el correo de la cuenta y solicite un reto MFA.";
    showError("");
    recoveryEmail.focus();
  });

  cancelRecoveryButton.addEventListener("click", resetRecovery);
  startRecoveryButton.addEventListener("click", startAccountRecovery);
  verifyRecoveryButton.addEventListener("click", verifyAccountRecovery);

  document.getElementById("restartMfaButton").addEventListener("click", () => {
    challengeToken = null;
    pendingAuthentication = null;
    mfaCode.value = "";
    document.getElementById("mfaSetupSecret").textContent = "";
    mfaPanel.hidden = true;
    recoveryPanel.hidden = true;
    credentialsPanel.hidden = false;
    button.hidden = false;
    button.innerHTML = '<span>Ingresar</span><i class="bi bi-arrow-right"></i>';
    showError("");
  });

  document.getElementById("finishLoginButton").addEventListener("click", () => {
    if (pendingAuthentication) finalizeAuthentication(pendingAuthentication, pendingEmail);
  });

  form.addEventListener("submit", async event => {
    event.preventDefault();
    showError("");
    button.disabled = true;
    button.innerHTML = '<span class="spinner-border spinner-border-sm"></span><span>Validando...</span>';
    try {
      const email = document.getElementById("email").value.trim();
      const pass = password.value;
      if (!email || !pass) throw new Error("Ingrese el correo y la contraseña.");

      const data = await requestJson(`${API_BASE_URL}/auth/login`, {
        method: "POST",
        headers: {"Content-Type": "application/json", "Accept": "application/json"},
        cache: "no-store",
        body: JSON.stringify({email, password: pass})
      });

      password.value = "";
      if (!data.token) {
        throw new Error("El backend no entregó una sesión válida.");
      }
      finalizeAuthentication(data, email);
    } catch (error) {
      showError(error.message || "No fue posible conectarse con el servidor.");
      button.innerHTML = '<span>Ingresar</span><i class="bi bi-arrow-right"></i>';
    } finally {
      button.disabled = false;
    }
  });

  async function startAccountRecovery() {
    const email = recoveryEmail.value.trim();
    if (!email) {
      showError("Ingrese el correo de la cuenta que desea recuperar.");
      return;
    }

    startRecoveryButton.disabled = true;
    verifyRecoveryButton.disabled = true;
    showError("");
    recoveryStatus.textContent = "Solicitando el reto MFA...";
    try {
      const data = await requestJson(`${API_BASE_URL}/auth/recovery/start`, {
        method: "POST",
        headers: {"Content-Type": "application/json", "Accept": "application/json"},
        cache: "no-store",
        body: JSON.stringify({email})
      });
      if (data.recoveryAvailable !== true || !data.challengeToken) {
        throw new Error("La recuperación con MFA no está disponible para esta cuenta. Contacte al administrador.");
      }
      challengeToken = data.challengeToken;
      pendingEmail = email;
      recoveryCode.disabled = false;
      verifyRecoveryButton.disabled = false;
      startRecoveryButton.disabled = true;
      recoveryStatus.textContent = "Reto generado. Ingrese el código TOTP o un código de recuperación.";
      recoveryCode.focus();
    } catch (error) {
      recoveryStatus.textContent = "";
      showError(error.message || "No fue posible iniciar la recuperación.");
      startRecoveryButton.disabled = false;
    }
  }

  async function verifyAccountRecovery() {
    const code = recoveryCode.value.trim();
    if (!challengeToken) {
      showError("Primero solicite el reto de recuperación.");
      return;
    }
    if (!code) {
      showError("Ingrese el código de su autenticador o un código de recuperación.");
      return;
    }

    verifyRecoveryButton.disabled = true;
    showError("");
    recoveryStatus.textContent = "Validando el segundo factor...";
    try {
      const data = await requestJson(`${API_BASE_URL}/auth/recovery/verify`, {
        method: "POST",
        headers: {"Content-Type": "application/json", "Accept": "application/json"},
        cache: "no-store",
        body: JSON.stringify({challengeToken, code})
      });
      if (!data.token) throw new Error("La recuperación no entregó una sesión válida.");
      finalizeAuthentication(data, pendingEmail);
    } catch (error) {
      showError(error.message || "El código MFA no es válido o el reto expiró.");
      verifyRecoveryButton.disabled = false;
    }
  }

  function resetRecovery() {
    challengeToken = null;
    pendingEmail = "";
    recoveryCode.value = "";
    recoveryCode.disabled = true;
    startRecoveryButton.disabled = false;
    verifyRecoveryButton.disabled = true;
    accountRecoveryPanel.hidden = true;
    credentialsPanel.hidden = false;
    button.hidden = false;
    button.innerHTML = '<span>Ingresar</span><i class="bi bi-arrow-right"></i>';
    recoveryStatus.textContent = "";
    showError("");
  }

  async function requestJson(url, options) {
    const response = await fetch(url, options);
    const raw = await response.text();
    let data;
    try { data = JSON.parse(raw); } catch { data = {message: raw}; }
    if (!response.ok) throw new Error(data.message || data.error || "No fue posible completar la autenticación.");
    return data;
  }

  function finalizeAuthentication(data, email) {
    if (!data.token) {
      throw new Error("El backend no entregó un token de sesión.");
    }
    clearSession();
    sessionStorage.setItem("token", data.token);
    sessionStorage.setItem("nombre", data.nombre || "");
    sessionStorage.setItem("email", data.email || email);
    sessionStorage.setItem("rol", data.rol || "");
    sessionStorage.setItem("rolId", String(data.rolId || ""));
    sessionStorage.setItem("permisosRol", data.permisos || "{}");
    if (data.empresaId != null) sessionStorage.setItem("empresaId", String(data.empresaId));
    if (data.empresaNombre) sessionStorage.setItem("empresaNombre", data.empresaNombre);
    routeByRole(data.rol);
  }

  function showError(message) {
    errorBox.textContent = message;
    errorBox.style.display = message ? "block" : "none";
  }
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
    const response = await fetch(`${API_BASE_URL}/constancias-capacitacion/verificar/${encodeURIComponent(code)}`, {cache: "no-store"});
    const raw = await response.text();
    let data; try { data = JSON.parse(raw); } catch { data = {message: raw}; }
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
