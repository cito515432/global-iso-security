document.addEventListener("DOMContentLoaded", async () => {
  if (!App.requireAuth()) return;
  App.bindNavigation();
  document.querySelectorAll("[data-return-app]").forEach(button =>
    button.addEventListener("click", () => App.routeByRole()));
  try {
    const user = await App.user();
    document.getElementById("profileName").value = user.nombre || "";
    document.getElementById("profileEmail").value = user.email || "";
  } catch (_) { return; }

  document.getElementById("profileForm").addEventListener("submit", async event => {
    event.preventDefault();
    const body = {
      nombre: document.getElementById("profileName").value,
      email: document.getElementById("profileEmail").value,
      currentPassword: document.getElementById("currentPassword").value,
      newPassword: document.getElementById("newPassword").value || null,
      passwordConfirmation: document.getElementById("passwordConfirmation").value || null
    };
    try {
      const updated = await App.api("/usuarios/me", {method:"PUT", body:JSON.stringify(body)});
      alert(`Perfil actualizado para ${updated.email}. Por seguridad, inicie sesión de nuevo.`);
      await App.logout();
    } catch (error) { App.toast(error.message, "error"); }
  });
});
