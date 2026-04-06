// ================================
// DATOS QUE PUEDES CAMBIAR FACIL
// ================================

// PEGA AQUI EL LINK DE YOUTUBE DEL MODULO 2
const VIDEO_MODULO_2 = 'https://www.youtube.com/watch?v=TU_VIDEO_AQUI';

// PEGA AQUI EL LINK DE LA DIAPOSITIVA DEL MODULO 3
const DIAPOSITIVA_MODULO_3 = 'https://docs.google.com/presentation/d/TU_ID/embed';

// DATOS DE PERFIL
const NOMBRE_CAPACITADOR = 'Capacitador';
const EMAIL_CAPACITADOR = 'capacitador@globaliso.com';
const EMPRESA_CAPACITADOR = 'Global ISO Security';

// ================================
// INIT
// ================================
document.addEventListener('DOMContentLoaded', () => {
  cargarPerfil();
  verModulo(1);

  document.getElementById('btnLogout').addEventListener('click', () => {
    window.location.href = 'login.html';
  });
});

// ================================
// PERFIL
// ================================
function cargarPerfil() {
  setEl('nombreCapacitadorSidebar', NOMBRE_CAPACITADOR);
  setEl('perfilNombreTitulo', NOMBRE_CAPACITADOR);
  setVal('perfilNombreInput', NOMBRE_CAPACITADOR);
  setVal('perfilEmailInput', EMAIL_CAPACITADOR);
  setVal('perfilEmpresaInput', EMPRESA_CAPACITADOR);
}

// ================================
// NAVEGACION
// ================================
window.mostrarSeccion = function(seccion, btn) {
  document.querySelectorAll('.seccion').forEach(s => s.classList.remove('activa'));

  const target = document.getElementById('seccion-' + seccion);
  if (target) target.classList.add('activa');

  document.querySelectorAll('.btn-menu').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');
};

// ================================
// MODULOS
// ================================
window.verModulo = function(numeroModulo, btn = null) {
  document.querySelectorAll('.btn-modulo').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');

  const titulo = document.getElementById('tituloModuloActivo');
  const descripcion = document.getElementById('descripcionModuloActivo');
  const contenido = document.getElementById('contenidoModuloActivo');

  if (numeroModulo === 1) {
    titulo.textContent = 'Módulo 1: Fundamentos de ISO 27001';
    descripcion.textContent = 'Conceptos básicos para comprender el Sistema de Gestión de Seguridad de la Información.';
    contenido.innerHTML = `
      <div class="modulo-texto">
        <h5>¿Qué es ISO 27001?</h5>
        <p>
          ISO 27001 es una norma internacional que establece los requisitos para implementar,
          mantener y mejorar un Sistema de Gestión de Seguridad de la Información (SGSI).
        </p>

        <h5>¿Qué es un SGSI?</h5>
        <p>
          Es un conjunto de políticas, procesos, procedimientos y controles diseñados para
          proteger la información de una organización frente a riesgos internos y externos.
        </p>

        <h5>Objetivos del SGSI</h5>
        <ul>
          <li>Proteger la información sensible de la organización.</li>
          <li>Reducir riesgos relacionados con la seguridad de la información.</li>
          <li>Garantizar continuidad, confianza y cumplimiento.</li>
          <li>Establecer controles claros y responsabilidades definidas.</li>
        </ul>

        <h5>Principios clave</h5>
        <ul>
          <li><strong>Confidencialidad:</strong> la información solo debe ser accesible para personas autorizadas.</li>
          <li><strong>Integridad:</strong> la información debe mantenerse exacta, completa y sin alteraciones no autorizadas.</li>
          <li><strong>Disponibilidad:</strong> la información debe estar accesible cuando se necesite.</li>
        </ul>

        <h5>Contexto de la organización</h5>
        <p>
          Antes de implementar ISO 27001, la empresa debe identificar su contexto interno y externo,
          las necesidades de las partes interesadas y el alcance del sistema de gestión.
        </p>

        <h5>Roles y responsabilidades</h5>
        <p>
          Es importante que cada persona dentro de la organización tenga claras sus responsabilidades
          en materia de seguridad de la información, desde la alta dirección hasta los usuarios operativos.
        </p>

        <h5>Importancia de la concienciación</h5>
        <p>
          La seguridad de la información no depende solo de la tecnología, sino también del comportamiento
          del personal. Por eso, la capacitación y la sensibilización son fundamentales.
        </p>
      </div>
    `;
  }

  else if (numeroModulo === 2) {
    titulo.textContent = 'Módulo 2: Riesgos y Controles';
    descripcion.textContent = 'Video de apoyo sobre riesgos, amenazas, vulnerabilidades y controles de seguridad.';

    const embedUrl = convertirYoutubeAEmbed(VIDEO_MODULO_2);

    contenido.innerHTML = `
      <iframe
        class="embed-frame"
        src="${embedUrl}"
        title="Video del módulo 2"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
        allowfullscreen>
      </iframe>
    `;
  }

  else if (numeroModulo === 3) {
    titulo.textContent = 'Módulo 3: Incidentes, Cumplimiento y Cierre';
    descripcion.textContent = 'Presentación visual sobre gestión de incidentes, cumplimiento y cierre de capacitación.';

    contenido.innerHTML = `
      <iframe
        class="embed-frame"
        src="${DIAPOSITIVA_MODULO_3}"
        title="Diapositiva del módulo 3">
      </iframe>
    `;
  }
};

// ================================
// HELPERS
// ================================
function convertirYoutubeAEmbed(url) {
  try {
    const u = new URL(url);

    if (u.hostname.includes('youtube.com')) {
      const videoId = u.searchParams.get('v');
      return `https://www.youtube.com/embed/${videoId}`;
    }

    if (u.hostname.includes('youtu.be')) {
      const videoId = u.pathname.replace('/', '');
      return `https://www.youtube.com/embed/${videoId}`;
    }

    return url;
  } catch {
    return url;
  }
}

function setEl(id, valor) {
  const el = document.getElementById(id);
  if (el) el.textContent = valor;
}

function setVal(id, valor) {
  const el = document.getElementById(id);
  if (el) el.value = valor;
}