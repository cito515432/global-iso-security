let currentUser, service, serviceId;
let dashboard = {}, courses = [], rpmAnalyses = [], certificates = [], questions = [];
let selectedCourseId = null;

const $ = id => document.getElementById(id);
const currentCourse = () => courses.find(c => Number(c.id) === Number(selectedCourseId));

document.addEventListener("DOMContentLoaded", async () => {
  if (!App.requireAuth(["CAPACITADOR"])) return;
  App.bindNavigation();
  bindEvents();
  try {
    currentUser = await App.user();
    $("profileEmail").textContent = currentUser.email || "—";
    service = await App.api("/servicios/mi-servicio");
    serviceId = service.servicioId;
    $("serviceBadge").innerHTML = App.badge(service.estado);
    $("profileService").innerHTML = `<div class="detail-list"><p><small>Servicio</small><strong>#${serviceId}</strong></p><p><small>Sector</small><strong>${App.esc(service.sectorNombre || "—")}</strong></p><p><small>Estado</small><strong>${App.badge(service.estado)}</strong></p></div>`;
    await loadAll();
  } catch (e) {
    App.toast(e.message, "error");
  }
});

function bindEvents() {
  $("refreshBtn").onclick = loadAll;
  document.querySelectorAll("[data-open-course]").forEach(b => b.onclick = () => openCourse());
  $("programSearch").oninput = renderPrograms;
  $("programStatus").onchange = renderPrograms;
  $("participantSearch").oninput = renderParticipants;
  $("certificateSearch").oninput = renderCertificates;
  $("contentCourse").onchange = e => selectCourse(e.target.value);
  $("participantCourse").onchange = e => selectCourse(e.target.value);
  $("addModuleBtn").onclick = () => openModule();
  $("addQuestionBtn").onclick = () => openQuestion();
  $("addParticipantBtn").onclick = () => openParticipant();
  $("courseForm").onsubmit = saveCourse;
  $("moduleForm").onsubmit = saveModule;
  $("participantForm").onsubmit = saveParticipant;
  $("questionForm").onsubmit = saveQuestion;
  $("evaluationForm").onsubmit = submitEvaluation;
}

async function loadAll() {
  if (!serviceId) return;
  try {
    [dashboard, courses, rpmAnalyses, certificates] = await Promise.all([
      App.api(`/formacion/servicio/${serviceId}/dashboard`),
      App.api(`/formacion/servicio/${serviceId}`),
      App.api(`/rpm/servicio/${serviceId}`),
      App.api(`/constancias-capacitacion/servicio/${serviceId}`)
    ]);
    if (!selectedCourseId || !courses.some(c => Number(c.id) === Number(selectedCourseId))) selectedCourseId = courses[0]?.id || null;
    renderAll();
    await loadQuestions();
  } catch (e) {
    App.toast(e.message, "error");
  }
}

function renderAll() {
  renderDashboard();
  renderPrograms();
  renderSelectors();
  renderCourseContent();
  renderParticipants();
  renderRpm();
  renderCertificates();
  renderResults();
}

function metric(label, value, icon, hint = "") {
  return `<div class="card-app metric"><span class="icon bi ${icon}"></span><div class="label">${App.esc(label)}</div><div class="value">${App.esc(value)}</div><div class="hint">${App.esc(hint)}</div></div>`;
}

function renderDashboard() {
  $("metrics").innerHTML = [
    metric("Programas", dashboard.programas || 0, "bi-journal-richtext", `${dashboard.activos || 0} activos`),
    metric("Participantes", dashboard.participantes || 0, "bi-people", `${dashboard.pendientes || 0} por aprobar`),
    metric("Finalización", `${dashboard.progresoPromedio || 0}%`, "bi-graph-up"),
    metric("Puntaje promedio", dashboard.puntajePromedio || 0, "bi-clipboard-data"),
    metric("Aprobados", dashboard.aprobados || 0, "bi-check-circle"),
    metric("Constancias", dashboard.constancias || 0, "bi-patch-check", `${dashboard.rpm || 0} programas RPM`)
  ].join("");

  const active = courses.filter(c => c.estado !== "COMPLETADA").slice(0, 5);
  $("activePrograms").innerHTML = active.length ? active.map(c => `
    <div class="priority-card ${c.creadaPorRpm ? "high" : ""}">
      <div class="d-flex justify-content-between gap-2"><h4>${App.esc(c.titulo)}</h4>${c.creadaPorRpm ? App.badge("RPM") : App.badge(c.estado)}</div>
      <p>${App.esc(c.objetivo || c.descripcion || "Sin objetivo definido")}</p>
      <div class="mt-2">${App.progress(c.progresoPromedio)}</div>
    </div>`).join("") : empty("No hay programas en ejecución", "bi-journal-plus");

  const recs = trainingRecommendations().filter(x => x.decision.estado !== "RECHAZADA").slice(0, 5);
  $("rpmPreview").innerHTML = recs.length ? recs.map(x => `
    <div class="priority-card ${String(x.analysis.prioridad).toLowerCase()}"><h4>${App.esc(x.analysis.controlCodigo || "RPM")} · ${App.badge(x.analysis.prioridad)}</h4><p>${App.esc(x.decision.accion)}</p></div>`).join("") : empty("No hay recomendaciones formativas pendientes", "bi-activity");
}

function renderPrograms() {
  const query = ($("programSearch").value || "").toLowerCase();
  const state = $("programStatus").value;
  const filtered = courses.filter(c => (!state || c.estado === state) && `${c.titulo} ${c.objetivo} ${c.controlCodigo}`.toLowerCase().includes(query));
  $("programBody").innerHTML = filtered.map(c => `
    <tr>
      <td><strong>${App.esc(c.titulo)}</strong><br><span class="text-muted-app">${App.esc(c.objetivo || c.descripcion || "—")}</span>${c.controlCodigo ? `<br><small>Control: ${App.esc(c.controlCodigo)}</small>` : ""}</td>
      <td>${c.creadaPorRpm ? App.badge("RPM") : App.badge("MANUAL")}</td>
      <td>${App.badge(c.estado)}</td>
      <td><small>Inicio: ${App.fmtDate(c.fechaInicio)}<br>Límite: ${App.fmtDate(c.fechaLimite)}</small></td>
      <td>${c.participantes.length}</td>
      <td style="min-width:125px">${App.progress(c.progresoPromedio)}</td>
      <td><strong>${Math.round(c.puntajePromedio || 0)}</strong>/100<br><small>${c.aprobados || 0} aprobados · ${c.preguntasActivas || 0} preguntas</small></td>
      <td><div class="inline-actions"><button class="btn-app" onclick="selectCourse(${c.id});showSection('contenido')" title="Contenido"><i class="bi bi-collection-play"></i></button><button class="btn-app" onclick="openCourse(${c.id})" title="Editar"><i class="bi bi-pencil"></i></button><button class="btn-app btn-danger-app" onclick="deleteCourse(${c.id})" title="Eliminar"><i class="bi bi-trash"></i></button></div></td>
    </tr>`).join("") || `<tr><td colspan="8" class="empty">No se encontraron capacitaciones.</td></tr>`;
}

function renderSelectors() {
  const html = courses.length ? courses.map(c => `<option value="${c.id}" ${Number(c.id) === Number(selectedCourseId) ? "selected" : ""}>${App.esc(c.titulo)}</option>`).join("") : `<option value="">No hay capacitaciones</option>`;
  document.querySelectorAll(".course-selector").forEach(s => s.innerHTML = html);
  $("addModuleBtn").disabled = !selectedCourseId;
  $("addQuestionBtn").disabled = !selectedCourseId;
  $("addParticipantBtn").disabled = !selectedCourseId;
}

async function selectCourse(id) {
  selectedCourseId = id ? Number(id) : null;
  renderSelectors();
  renderCourseContent();
  renderParticipants();
  await loadQuestions();
}

function renderCourseContent() {
  const c = currentCourse();
  if (!c) {
    $("courseSummary").innerHTML = empty("Cree una capacitación para comenzar", "bi-journal-plus");
    $("moduleList").innerHTML = empty("Sin módulos", "bi-collection-play");
    return;
  }
  $("courseSummary").innerHTML = `<div class="course-hero"><div><h3>${App.esc(c.titulo)}</h3><p class="small-app text-muted-app">${App.esc(c.objetivo || c.descripcion || "Sin objetivo definido")}</p><div class="stat-line"><span>Estado <strong>${App.pretty(c.estado)}</strong></span><span>Público <strong>${App.esc(c.publicoObjetivo || "Por definir")}</strong></span><span>Mínimo <strong>${c.puntajeMinimo}/100</strong></span>${c.creadaPorRpm ? `<span>Origen <strong>RPM · ${App.esc(c.controlCodigo || "")}</strong></span>` : ""}</div></div>${App.badge(c.estado)}</div>`;
  $("moduleList").innerHTML = c.modulos.length ? [...c.modulos].sort((a,b) => a.orden-b.orden).map(m => `
    <div class="module-card"><div class="d-flex justify-content-between gap-2"><h4>${m.orden}. ${App.esc(m.titulo)}</h4><div class="inline-actions"><button class="btn-app" onclick="openModule(${m.id})"><i class="bi bi-pencil"></i></button><button class="btn-app btn-danger-app" onclick="deleteModule(${m.id})"><i class="bi bi-trash"></i></button></div></div><p>${App.esc(m.descripcion || m.contenido || "Sin descripción")}</p><div class="stat-line"><span><i class="bi bi-clock"></i> ${m.duracionMinutos} min</span><span>${m.obligatorio ? "Obligatorio" : "Opcional"}</span>${m.materialUrl ? `<span><a class="link-app" href="${App.esc(m.materialUrl)}" target="_blank">Material</a></span>` : ""}${m.videoUrl ? `<span><a class="link-app" href="${App.esc(m.videoUrl)}" target="_blank">Video</a></span>` : ""}</div></div>`).join("") : empty("Aún no hay módulos. Agregue contenido, materiales y videos.", "bi-collection-play");
}

async function loadQuestions() {
  if (!selectedCourseId) { questions = []; renderQuestions(); return; }
  try { questions = await App.api(`/formacion/capacitaciones/${selectedCourseId}/preguntas`); renderQuestions(); }
  catch (e) { questions = []; renderQuestions(); App.toast(e.message, "error"); }
}

function renderQuestions() {
  $("questionList").innerHTML = questions.length ? questions.map(q => `
    <div class="question-card"><div class="d-flex justify-content-between gap-2"><h4>${q.orden}. ${App.esc(q.enunciado)}</h4><div class="inline-actions"><button class="btn-app" onclick="openQuestion(${q.id})"><i class="bi bi-pencil"></i></button><button class="btn-app btn-danger-app" onclick="deleteQuestion(${q.id})"><i class="bi bi-trash"></i></button></div></div><p>A. ${App.esc(q.opcionA)} · B. ${App.esc(q.opcionB)}${q.opcionC ? ` · C. ${App.esc(q.opcionC)}` : ""}${q.opcionD ? ` · D. ${App.esc(q.opcionD)}` : ""}</p><div class="stat-line"><span>Correcta <strong>${App.esc(q.respuestaCorrecta)}</strong></span><span>Puntos <strong>${q.puntos}</strong></span><span>${q.activa ? App.badge("ACTIVA") : App.badge("INACTIVA")}</span></div></div>`).join("") : empty("No hay preguntas. Cree un banco para aplicar y calificar evaluaciones reales.", "bi-ui-checks");
}

function renderParticipants() {
  const c = currentCourse();
  const q = ($("participantSearch").value || "").toLowerCase();
  const list = (c?.participantes || []).filter(p => `${p.nombre} ${p.email} ${p.documento} ${p.cargo}`.toLowerCase().includes(q));
  const certByParticipant = new Map(certificates.filter(x => x.participante?.id).map(x => [Number(x.participante.id), x]));
  $("participantBody").innerHTML = list.map(p => {
    const cert = certByParticipant.get(Number(p.id));
    const passed = p.puntajeEvaluacion != null && p.puntajeEvaluacion >= c.puntajeMinimo;
    return `<tr><td><strong>${App.esc(p.nombre)}</strong><br><span class="text-muted-app">${App.esc(p.email)}</span></td><td>${App.esc(p.cargo || "—")}<br><small>${App.esc(p.documento || "Sin documento")}</small></td><td>${App.badge(p.estado)}</td><td style="min-width:125px">${App.progress(p.progresoPorcentaje)}</td><td>${p.puntajeEvaluacion == null ? "—" : `<strong>${Math.round(p.puntajeEvaluacion * 10)/10}</strong>/100<br>${passed ? App.badge("APROBADA") : App.badge("NO_APROBADA")}`}</td><td>${p.intentos || 0}</td><td>${cert ? `${App.badge(cert.estado)}<br><small>${App.esc(cert.codigoVerificacion)}</small>` : "Pendiente"}</td><td><div class="inline-actions"><button class="btn-app" onclick="openParticipant(${p.id})" title="Editar"><i class="bi bi-pencil"></i></button><button class="btn-app btn-success-app" onclick="openEvaluation(${p.id})" title="Evaluar"><i class="bi bi-clipboard-check"></i></button>${cert ? `<button class="btn-app" onclick="downloadCertificate(${cert.id})" title="Descargar constancia"><i class="bi bi-file-earmark-pdf"></i></button>` : `<button class="btn-app" onclick="issueCertificate(${p.id})" title="Emitir constancia"><i class="bi bi-patch-plus"></i></button>`}<button class="btn-app btn-danger-app" onclick="deleteParticipant(${p.id})" title="Eliminar"><i class="bi bi-trash"></i></button></div></td></tr>`;
  }).join("") || `<tr><td colspan="8" class="empty">${c ? "No hay participantes asignados." : "Seleccione o cree una capacitación."}</td></tr>`;
}

function trainingRecommendations() {
  const latest = [];
  const seen = new Set();
  rpmAnalyses.forEach(a => {
    const key = a.soaControlId || a.id;
    if (seen.has(key)) return;
    seen.add(key);
    (a.decisiones || []).filter(d => d.tipoAccion === "CAPACITACION").forEach(d => latest.push({analysis:a, decision:d}));
  });
  return latest;
}

function renderRpm() {
  const data = trainingRecommendations();
  $("rpmList").innerHTML = data.length ? data.map(({analysis:a, decision:d}) => {
    let actions = "";
    if (d.estado === "PENDIENTE") actions = `<button class="btn-app btn-success-app" onclick="approveRpm(${d.id})"><i class="bi bi-check2"></i> Aprobar recomendación</button> <button class="btn-app btn-danger-app" onclick="rejectRpm(${d.id})"><i class="bi bi-x-lg"></i> Rechazar</button>`;
    else if (["APROBADA","MODIFICADA"].includes(d.estado)) actions = `<button class="btn-app btn-primary-app" onclick="createCourseFromRpm(${d.id})"><i class="bi bi-journal-plus"></i> Convertir en capacitación</button>`;
    else if (d.estado === "EN_EJECUCION") actions = `<span class="small-app text-muted-app"><i class="bi bi-check-circle"></i> Programa creado y en ejecución.</span>`;
    return `<div class="card-app mb-3"><div class="d-flex justify-content-between gap-3"><div><h3>${App.esc(a.controlCodigo || "Análisis RPM")} · ${App.esc(a.controlTitulo || "")}</h3><p class="small-app text-muted-app">${App.esc(a.explicacion || a.resumen)}</p></div><div>${App.badge(a.prioridad)} <strong>${a.puntaje}/100</strong></div></div><div class="signal-list">${(a.senales || []).map(s => `<span class="signal" title="${App.esc(s.descripcion)}">${App.esc(s.codigo)} +${s.peso}</span>`).join("")}</div><div class="priority-card"><h4>${App.badge(d.estado)} ${App.esc(App.pretty(d.tipoAccion))}</h4><p>${App.esc(d.accion)}</p>${d.justificacion ? `<p><strong>Validación:</strong> ${App.esc(d.justificacion)}</p>` : ""}<div class="mt-2">${actions}</div></div></div>`;
  }).join("") : `<div class="card-app">${empty("No hay recomendaciones de capacitación generadas por RPM. El motor las crea cuando detecta una brecha humana asociada a controles y hallazgos.", "bi-activity")}</div>`;
}

function renderCertificates() {
  const q = ($("certificateSearch").value || "").toLowerCase();
  const data = certificates.filter(c => `${c.nombreCompleto} ${c.documento} ${c.codigoVerificacion} ${c.capacitacion?.titulo}`.toLowerCase().includes(q));
  $("certificateBody").innerHTML = data.map(c => `<tr><td><strong>${App.esc(c.codigoVerificacion)}</strong><br><small>${App.esc(c.codigoInterno || "")}</small></td><td>${App.esc(c.nombreCompleto)}<br><small>${App.esc(c.documento)}</small></td><td>${App.esc(c.capacitacion?.titulo || "—")}</td><td>${c.puntaje == null ? "—" : `${Math.round(c.puntaje*10)/10}/100`}</td><td>${App.fmtDate(c.fechaFirma)}</td><td>${App.badge(c.estado)}</td><td><button class="btn-app btn-primary-app" onclick="downloadCertificate(${c.id})"><i class="bi bi-download"></i> PDF</button></td></tr>`).join("") || `<tr><td colspan="7" class="empty">No hay constancias emitidas.</td></tr>`;
}

function renderResults() {
  const participants = courses.flatMap(c => c.participantes || []);
  const scored = participants.filter(p => p.puntajeEvaluacion != null);
  const avgScore = scored.length ? Math.round(scored.reduce((s,p) => s + p.puntajeEvaluacion, 0) / scored.length) : 0;
  const completion = participants.length ? Math.round(participants.reduce((s,p) => s + (p.progresoPorcentaje || 0),0) / participants.length) : 0;
  const approved = courses.reduce((sum,c) => sum + (c.aprobados || 0),0);
  $("resultMetrics").innerHTML = [metric("Cobertura", participants.length, "bi-people"), metric("Finalización", `${completion}%`, "bi-bar-chart"), metric("Puntaje", avgScore, "bi-clipboard-data"), metric("Aprobaciones", approved, "bi-award")].join("");
  $("resultBody").innerHTML = courses.map(c => `<tr><td><strong>${App.esc(c.titulo)}</strong></td><td>${c.participantes.length}</td><td style="min-width:140px">${App.progress(c.progresoPromedio)}</td><td>${Math.round(c.puntajePromedio || 0)}/100</td><td>${c.aprobados || 0}</td><td>${c.constancias || 0}</td><td>${c.creadaPorRpm ? App.badge("SI") : App.badge("NO")}</td></tr>`).join("") || `<tr><td colspan="7" class="empty">No hay resultados todavía.</td></tr>`;
}

function openCourse(id = null) {
  const c = courses.find(x => Number(x.id) === Number(id));
  $("courseForm").reset(); $("courseId").value = c?.id || ""; $("courseTitle").value = c?.titulo || "";
  $("courseObjective").value = c?.objetivo || ""; $("courseDescription").value = c?.descripcion || "";
  $("courseState").value = c?.estado || "PENDIENTE"; $("courseMinScore").value = c?.puntajeMinimo ?? 80;
  $("courseStart").value = toDateTimeInput(c?.fechaInicio); $("courseDeadline").value = toDateTimeInput(c?.fechaLimite);
  $("courseAudience").value = c?.publicoObjetivo || ""; $("courseMaterial").value = c?.materialUrl || ""; $("courseVideo").value = c?.videoUrl || "";
  App.modal("courseModal").show();
}

async function saveCourse(e) {
  e.preventDefault();
  const id = $("courseId").value; const old = courses.find(c => Number(c.id) === Number(id));
  const body = {titulo:$("courseTitle").value, objetivo:$("courseObjective").value, descripcion:$("courseDescription").value, estado:$("courseState").value,
    puntajeMinimo:Number($("courseMinScore").value || 80), fechaInicio:$("courseStart").value || null, fechaLimite:$("courseDeadline").value || null,
    publicoObjetivo:$("courseAudience").value, materialUrl:$("courseMaterial").value, videoUrl:$("courseVideo").value, servicio:{id:serviceId},
    creadaPorRpm:old?.creadaPorRpm || false, motivoRpm:old?.motivoRpm || null, controlCodigo:old?.controlCodigo || null, riesgoIdReferencia:old?.riesgoId || null};
  try { const saved = await App.api(id ? `/capacitaciones/${id}` : "/capacitaciones", {method:id?"PUT":"POST", body:JSON.stringify(body)}); selectedCourseId = saved.id; App.modal("courseModal").hide(); App.toast("Capacitación guardada"); await loadAll(); }
  catch (err) { App.toast(err.message,"error"); }
}

async function deleteCourse(id) {
  if (!confirm("¿Eliminar esta capacitación en borrador y sus elementos asociados? Los programas completados o con constancias se conservan como evidencia.")) return;
  try { await App.api(`/capacitaciones/${id}`,{method:"DELETE"}); App.toast("Capacitación eliminada"); if (Number(selectedCourseId)===Number(id)) selectedCourseId=null; await loadAll(); }
  catch(e){App.toast(e.message,"error");}
}

function openModule(id = null) {
  const c=currentCourse(); if(!c){App.toast("Seleccione una capacitación","error");return;}
  const m=c.modulos.find(x=>Number(x.id)===Number(id)); $("moduleForm").reset(); $("moduleId").value=m?.id||""; $("moduleTitle").value=m?.titulo||"";
  $("moduleOrder").value=m?.orden||c.modulos.length+1; $("moduleDuration").value=m?.duracionMinutos||15; $("moduleRequired").checked=m?.obligatorio??true;
  $("moduleDescription").value=m?.descripcion||""; $("moduleContent").value=m?.contenido||""; $("moduleMaterial").value=m?.materialUrl||""; $("moduleVideo").value=m?.videoUrl||"";
  App.modal("moduleModal").show();
}

async function saveModule(e){e.preventDefault();const id=$("moduleId").value;const body={titulo:$("moduleTitle").value,descripcion:$("moduleDescription").value,contenido:$("moduleContent").value,materialUrl:$("moduleMaterial").value,videoUrl:$("moduleVideo").value,orden:Number($("moduleOrder").value||1),duracionMinutos:Number($("moduleDuration").value||15),obligatorio:$("moduleRequired").checked};try{await App.api(id?`/formacion/capacitaciones/${selectedCourseId}/modulos/${id}`:`/formacion/capacitaciones/${selectedCourseId}/modulos`,{method:id?"PUT":"POST",body:JSON.stringify(body)});App.modal("moduleModal").hide();App.toast("Módulo guardado");await loadAll();}catch(err){App.toast(err.message,"error");}}
async function deleteModule(id){if(!confirm("¿Eliminar el módulo?"))return;try{await App.api(`/formacion/modulos/${id}`,{method:"DELETE"});App.toast("Módulo eliminado");await loadAll();}catch(e){App.toast(e.message,"error");}}

function openQuestion(id=null){if(!selectedCourseId){App.toast("Seleccione una capacitación","error");return;}const q=questions.find(x=>Number(x.id)===Number(id));$("questionForm").reset();$("questionId").value=q?.id||"";$("questionText").value=q?.enunciado||"";$("questionA").value=q?.opcionA||"";$("questionB").value=q?.opcionB||"";$("questionC").value=q?.opcionC||"";$("questionD").value=q?.opcionD||"";$("questionCorrect").value=q?.respuestaCorrecta||"A";$("questionPoints").value=q?.puntos||1;$("questionOrder").value=q?.orden||questions.length+1;$("questionActive").checked=q?.activa??true;$("questionExplanation").value=q?.explicacion||"";App.modal("questionModal").show();}
async function saveQuestion(e){e.preventDefault();const id=$("questionId").value;const body={enunciado:$("questionText").value,opcionA:$("questionA").value,opcionB:$("questionB").value,opcionC:$("questionC").value,opcionD:$("questionD").value,respuestaCorrecta:$("questionCorrect").value,explicacion:$("questionExplanation").value,puntos:Number($("questionPoints").value||1),orden:Number($("questionOrder").value||1),activa:$("questionActive").checked};try{await App.api(id?`/formacion/capacitaciones/${selectedCourseId}/preguntas/${id}`:`/formacion/capacitaciones/${selectedCourseId}/preguntas`,{method:id?"PUT":"POST",body:JSON.stringify(body)});App.modal("questionModal").hide();App.toast("Pregunta guardada");await loadQuestions();await loadAll();}catch(err){App.toast(err.message,"error");}}
async function deleteQuestion(id){if(!confirm("¿Eliminar la pregunta?"))return;try{await App.api(`/formacion/preguntas/${id}`,{method:"DELETE"});App.toast("Pregunta eliminada");await loadQuestions();await loadAll();}catch(e){App.toast(e.message,"error");}}

function openParticipant(id=null){const c=currentCourse();if(!c){App.toast("Seleccione una capacitación","error");return;}const p=c.participantes.find(x=>Number(x.id)===Number(id));$("participantForm").reset();$("participantId").value=p?.id||"";$("participantName").value=p?.nombre||"";$("participantEmail").value=p?.email||"";$("participantDocument").value=p?.documento||"";$("participantJob").value=p?.cargo||"";$("participantState").value=p?.estado||"ASIGNADO";$("participantProgress").value=p?.progresoPorcentaje||0;$("participantScore").value=p?.puntajeEvaluacion??"";App.modal("participantModal").show();}
async function saveParticipant(e){e.preventDefault();const id=$("participantId").value;const score=$("participantScore").value;const body={nombre:$("participantName").value,email:$("participantEmail").value,documento:$("participantDocument").value,cargo:$("participantJob").value,estado:$("participantState").value,progresoPorcentaje:Number($("participantProgress").value||0),puntajeEvaluacion:score===""?null:Number(score)};try{await App.api(id?`/formacion/capacitaciones/${selectedCourseId}/participantes/${id}`:`/formacion/capacitaciones/${selectedCourseId}/participantes`,{method:id?"PUT":"POST",body:JSON.stringify(body)});App.modal("participantModal").hide();App.toast("Participante guardado");await loadAll();}catch(err){App.toast(err.message,"error");}}
async function deleteParticipant(id){if(!confirm("¿Retirar al participante de esta capacitación?"))return;try{await App.api(`/formacion/participantes/${id}`,{method:"DELETE"});App.toast("Participante eliminado");await loadAll();}catch(e){App.toast(e.message,"error");}}

function openEvaluation(participantId){const c=currentCourse();const p=c?.participantes.find(x=>Number(x.id)===Number(participantId));const active=questions.filter(q=>q.activa);if(!p)return;if(!active.length){App.toast("Primero cree al menos una pregunta activa","error");showSection("contenido");return;}$("evaluationParticipantId").value=p.id;$("evaluationParticipant").textContent=`${p.nombre} · Puntaje mínimo: ${c.puntajeMinimo}/100`;$("evaluationQuestions").innerHTML=active.map((q,i)=>`<div class="question-card"><h4>${i+1}. ${App.esc(q.enunciado)} <small>(${q.puntos} punto${q.puntos===1?"":"s"})</small></h4><div class="answer-grid">${[["A",q.opcionA],["B",q.opcionB],["C",q.opcionC],["D",q.opcionD]].filter(x=>x[1]).map(([k,v])=>`<label class="answer-option"><input type="radio" name="eval_q_${q.id}" value="${k}"> <strong>${k}.</strong> ${App.esc(v)}</label>`).join("")}</div></div>`).join("");App.modal("evaluationModal").show();}
async function submitEvaluation(e){e.preventDefault();const participantId=$("evaluationParticipantId").value;const responses={};questions.filter(q=>q.activa).forEach(q=>{const selected=document.querySelector(`input[name="eval_q_${q.id}"]:checked`);if(selected)responses[q.id]=selected.value;});try{const result=await App.api(`/formacion/capacitaciones/${selectedCourseId}/participantes/${participantId}/evaluar`,{method:"POST",body:JSON.stringify({respuestas:responses})});App.modal("evaluationModal").hide();App.toast(`Evaluación calificada: ${result.puntaje}/100 · ${result.aprobado?"Aprobada":"No aprobada"}`,result.aprobado?"success":"error");await loadAll();}catch(err){App.toast(err.message,"error");}}

async function approveRpm(id){try{await App.api(`/rpm/decisiones/${id}`,{method:"PUT",body:JSON.stringify({estado:"APROBADA",justificacion:"Recomendación formativa validada por el capacitador."})});App.toast("Recomendación aprobada");await loadAll();}catch(e){App.toast(e.message,"error");}}
async function rejectRpm(id){const reason=prompt("Explique por qué esta capacitación no es pertinente o cómo se tratará la señal de otra forma:");if(!reason)return;try{await App.api(`/rpm/decisiones/${id}`,{method:"PUT",body:JSON.stringify({estado:"RECHAZADA",justificacion:reason})});App.toast("Recomendación rechazada con justificación");await loadAll();}catch(e){App.toast(e.message,"error");}}
async function createCourseFromRpm(id){try{const c=await App.api(`/rpm/decisiones/${id}/crear-capacitacion`,{method:"POST"});selectedCourseId=c.id;App.toast("Programa creado desde RPM. Complete su contenido y participantes.");await loadAll();showSection("contenido");}catch(e){App.toast(e.message,"error");}}

async function issueCertificate(participantId){try{await App.api(`/constancias-capacitacion/participante/${participantId}`,{method:"POST"});App.toast("Constancia emitida");await loadAll();}catch(e){App.toast(e.message,"error");}}
async function downloadCertificate(id){try{const blob=await App.api(`/constancias-capacitacion/${id}/pdf`);App.downloadBlob(blob,`constancia-global-iso-${id}.pdf`);}catch(e){App.toast(e.message,"error");}}

function showSection(name){document.querySelectorAll(".section").forEach(s=>s.classList.remove("active"));document.querySelectorAll("[data-section]").forEach(b=>b.classList.toggle("active",b.dataset.section===name));$(`section-${name}`)?.classList.add("active");window.scrollTo({top:0,behavior:"smooth"});}
function empty(text,icon){return `<div class="empty"><i class="bi ${icon}"></i>${App.esc(text)}</div>`;}
function toDateTimeInput(v){if(!v)return"";const d=new Date(v);if(Number.isNaN(d.getTime()))return String(v).slice(0,16);const pad=n=>String(n).padStart(2,"0");return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;}
