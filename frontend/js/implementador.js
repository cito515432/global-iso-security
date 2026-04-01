// implementador.js
// Versión corregida: habilita menú (onclick), nombre, logout, checklist y filtros.

document.addEventListener("DOMContentLoaded", () => {
  cargarDatosUsuario();

  const btnLogout = document.getElementById("btnLogout");
  if (btnLogout) {
    btnLogout.addEventListener("click", () => {
      localStorage.clear();
      window.location.href = "login.html";
    });
  }
});

// Debe ser GLOBAL porque el HTML usa onclick="mostrarSeccion(...)"
window.mostrarSeccion = function (seccion, btn) {
  document.querySelectorAll(".seccion").forEach((s) => s.classList.remove("activa"));

  const target = document.getElementById("seccion-" + seccion);
  if (target) target.classList.add("activa");

  document.querySelectorAll(".btn-menu").forEach((b) => b.classList.remove("active"));
  if (btn) btn.classList.add("active");

  window.scrollTo({ top: 0, behavior: "smooth" });
};

// Debe ser GLOBAL porque el HTML usa onclick="setEstado(...)"
window.setEstado = function (btn, estado) {
  const item = btn.closest(".checklist-item");
  if (!item) return;

  item.dataset.estado = estado;

  item.querySelectorAll(".btn-estado").forEach((b) => b.classList.remove("active"));
  btn.classList.add("active");

  const recBox = item.querySelector(".recomendacion-box");
  if (recBox) {
    recBox.style.display = estado === "no-cumple" || estado === "en-proceso" ? "block" : "none";
  }
};

// Debe ser GLOBAL porque el HTML usa onclick="filtrarItems(...)"
window.filtrarItems = function (filtro, btn) {
  document.querySelectorAll(".btn-filtro").forEach((b) => b.classList.remove("active"));
  if (btn) btn.classList.add("active");

  document.querySelectorAll(".checklist-item").forEach((item) => {
    const estado = item.dataset.estado || "pendiente";
    item.style.display = filtro === "todos" || estado === filtro ? "block" : "none";
  });
};

function cargarDatosUsuario() {
  const nombre = localStorage.getItem("nombre") || "Implementador";
  const email = localStorage.getItem("email") || "";

  const sidebarName = document.getElementById("nombreImplementadorSidebar");
  if (sidebarName) sidebarName.textContent = nombre;

  const perfilTitulo = document.getElementById("perfilNombreTitulo");
  if (perfilTitulo) perfilTitulo.textContent = nombre;

  const perfilNombreInput = document.getElementById("perfilNombreInput");
  if (perfilNombreInput) perfilNombreInput.value = nombre;

  const perfilEmailInput = document.getElementById("perfilEmailInput");
  if (perfilEmailInput) perfilEmailInput.value = email;
}