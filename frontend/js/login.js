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
      if (challengeToken) {
        const code = mfaCode.value.trim();
        if (!code) throw new Error("Ingrese el código de su autenticador o un código de recuperación.");
        const data = await requestJson(`${API_BASE_URL}/auth/mfa/verify`, {
          method: "POST",
          headers: {"Content-Type": "application/json", "Accept": "application/json"},
          cache: "no-store",
          body: JSON.stringify({challengeToken, code})
        });
        if (data.mfaConfigured !== true || !data.token) {
          throw new Error("El backend no confirmó MFA; el acceso permanece bloqueado.");
        }
        mfaCode.value = "";
        if (Array.isArray(data.recoveryCodes) && data.recoveryCodes.length > 0) {
          pendingAuthentication = data;
          document.getElementById("recoveryCodesDisplay").textContent = data.recoveryCodes.join("\n");
          mfaPanel.hidden = true;
          recoveryPanel.hidden = false;
          button.hidden = true;
          return;
        }
        finalizeAuthentication(data, pendingEmail);
        return;
      }

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
      if (data.mfaRequired !== true || !data.challengeToken) {
        throw new Error("El backend no exige MFA; el inicio de sesión fue bloqueado.");
      }
      challengeToken = data.challengeToken;
      pendingEmail = email;
      credentialsPanel.hidden = true;
      mfaPanel.hidden = false;
      document.getElementById("mfaMessage").textContent = data.mfaSetupRequired
        ? "Activa tu segundo factor con una aplicación compatible con TOTP. Luego ingresa el código de seis dígitos."
        : "Abre tu aplicación autenticadora e ingresa el código actual.";
      mfaSetup.hidden = !data.mfaSetupRequired;
      if (data.mfaSetupRequired) {
        if (!data.setupSecret) throw new Error("El backend no entregó la clave para configurar MFA.");
        document.getElementById("mfaSetupSecret").textContent = data.setupSecret;
      }
      button.innerHTML = '<span>Verificar y entrar</span><i class="bi bi-shield-check"></i>';
      mfaCode.focus();
    } catch (error) {
      showError(error.message || "No fue posible conectarse con el servidor.");
      button.innerHTML = challengeToken
        ? '<span>Verificar y entrar</span><i class="bi bi-shield-check"></i>'
        : '<span>Ingresar</span><i class="bi bi-arrow-right"></i>';
    } finally {
      button.disabled = false;
    }
  });

  async function requestJson(url, options) {
    const response = await fetch(url, options);
    const raw = await response.text();
    let data;
    try { data = JSON.parse(raw); } catch { data = {message: raw}; }
    if (!response.ok) throw new Error(data.message || data.error || "No fue posible completar la autenticación.");
    return data;
  }

  function finalizeAuthentication(data, email) {
    if (data.mfaConfigured !== true || !data.token) {
      throw new Error("El backend no confirmó MFA; el acceso permanece bloqueado.");
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
