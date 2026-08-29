const API_URL = "/api";

async function login() {
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const errorMsg = document.getElementById('error-msg');

    if (!email || !password) {
        errorMsg.textContent = 'Por favor ingresa tu correo y contraseña.';
        errorMsg.style.display = 'block';
        return;
    }

    try {
        const response = await fetch(`${API_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            cache: 'no-store',
            body: JSON.stringify({ email, password })
        });

        const raw = await response.text();
        let data;
        try { data = JSON.parse(raw); } catch { data = { message: raw }; }

        if (!response.ok) {
            errorMsg.textContent = data.message || 'No fue posible iniciar sesión.';
            errorMsg.style.display = 'block';
            return;
        }

        sessionStorage.setItem('token', data.token);
        sessionStorage.setItem('rol', data.rol || '');
        sessionStorage.setItem('nombre', data.nombre || '');
        localStorage.removeItem('token');
        localStorage.removeItem('rol');
        localStorage.removeItem('nombre');

        const rol = String(data.rol || '').toLowerCase();
        if (rol === 'administrador') {
            window.location.href = 'admin.html';
        } else if (rol === 'implementador') {
            window.location.href = 'implementador.html';
        } else if (rol === 'auditor') {
            window.location.href = 'auditor.html';
        } else if (rol === 'capacitador') {
            window.location.href = 'capacitador.html';
        } else {
            window.location.href = 'empresa.html';
        }
    } catch (error) {
        errorMsg.textContent = 'No se pudo conectar con el servidor.';
        errorMsg.style.display = 'block';
    }
}
