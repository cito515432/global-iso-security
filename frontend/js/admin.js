let companies=[], services=[], users=[], roles=[], sectors=[], controls=[], organizationSummaries=new Map();
const contextFlags=[
  ["manejaDatosSensibles","Maneja datos sensibles"],["usaServiciosNube","Usa servicios en la nube"],
  ["permiteTrabajoRemoto","Permite trabajo remoto"],["procesaPagos","Procesa pagos"],
  ["infraestructuraPropia","Cuenta con infraestructura propia"],["dependeProveedores","Depende de terceros/proveedores"],
  ["servicioCritico24x7","Opera servicios críticos 24/7"],["manejaMenores","Trata datos de menores"],
  ["operaOtIot","Opera entornos OT/IoT"]
];

document.addEventListener("DOMContentLoaded", async()=>{
  if(!App.requireAuth(["ADMIN"])) return;
  App.bindNavigation();
  bindSectionTitles(); bindForms(); renderContextChecks();
  await App.user();
  await refreshAll();
});

async function refreshAll(){
  try{
    [companies,services,users,roles,sectors,controls]=await Promise.all([
      App.api("/empresas"),App.api("/servicios"),App.api("/usuarios"),App.api("/roles"),App.api("/sectores"),App.api("/catalogo-controles")
    ]);
    services.sort((a,b)=>new Date(b.fechaCreacion||0)-new Date(a.fechaCreacion||0));
    await loadOrganizationSummaries();
    populateSelects(); renderDashboard(); renderOrganizations(); renderUsers(); renderRoles(); renderControls();
    await loadContext();
  }catch(error){App.toast(error.message,"error");}
}

function metric(label,value,icon,hint=""){
  return `<div class="card-app metric"><i class="bi ${icon} icon"></i><div class="label">${App.esc(label)}</div><div class="value">${App.esc(value)}</div><div class="hint">${App.esc(hint)}</div></div>`;
}
function bindSectionTitles(){
  document.querySelectorAll("[data-section]").forEach(btn=>btn.addEventListener("click",()=>{
    document.getElementById("pageTitle").textContent=btn.textContent.trim();
  }));
  document.querySelectorAll("[data-section-jump]").forEach(btn=>btn.addEventListener("click",()=>{
    document.querySelector(`[data-section="${btn.dataset.sectionJump}"]`)?.click();
  }));
}
function bindForms(){
  document.getElementById("companyForm").addEventListener("submit",saveCompany);
  document.getElementById("serviceForm").addEventListener("submit",saveService);
  document.getElementById("userForm").addEventListener("submit",saveUser);
  document.getElementById("contextForm").addEventListener("submit",saveContext);
  ["organizationSearch","organizationStatus"].forEach(id=>document.getElementById(id).addEventListener("input",renderOrganizations));
  ["userSearch","userRoleFilter"].forEach(id=>document.getElementById(id).addEventListener("input",renderUsers));
  ["controlSearch","controlDomain"].forEach(id=>document.getElementById(id).addEventListener("input",renderControls));
  document.getElementById("contextCompany").addEventListener("change",loadContext);
  document.getElementById("reportCompany").addEventListener("change",()=>document.getElementById("reportPreview").innerHTML="");
}
async function loadOrganizationSummaries(){
  organizationSummaries=new Map();
  await Promise.all(companies.map(async company=>{
    try{organizationSummaries.set(company.id,await App.api(`/portal-empresa/empresa/${company.id}`));}
    catch{organizationSummaries.set(company.id,null);}
  }));
}
function populateSelects(){
  const companyOptions=companies.map(c=>`<option value="${c.id}">${App.esc(c.nombre)}</option>`).join("");
  ["serviceCompany","contextCompany","reportCompany"].forEach(id=>{const e=document.getElementById(id);const old=e.value;e.innerHTML=companyOptions;e.value=old||companies[0]?.id||"";});
  const sectorOptions=sectors.map(s=>`<option value="${s.id}">${App.esc(s.nombre)}</option>`).join("");
  ["serviceSector","ctxSector"].forEach(id=>{const e=document.getElementById(id);const old=e.value;e.innerHTML=sectorOptions;e.value=old||sectors[0]?.id||"";});
  document.getElementById("userCompany").innerHTML='<option value="">Sin organización (rol global)</option>'+companyOptions;
  document.getElementById("userRole").innerHTML=roles.map(r=>`<option value="${r.id}">${App.esc(App.pretty(r.nombre))}</option>`).join("");
  document.getElementById("userRoleFilter").innerHTML='<option value="">Todos los roles</option>'+roles.map(r=>`<option value="${App.esc(r.nombre)}">${App.esc(App.pretty(r.nombre))}</option>`).join("");
}
function renderDashboard(){
  const activeServices=services.filter(s=>!["CERRADO","FIRMADO"].includes(s.estado)).length;
  const companyUsers=users.filter(u=>u.empresa).length;
  document.getElementById("adminMetrics").innerHTML=[
    metric("Organizaciones",companies.length,"bi-buildings",`${services.length} servicios`),
    metric("Usuarios",users.length,"bi-people",`${companyUsers} asignados`),
    metric("Servicios activos",activeServices,"bi-shield-check","En implementación"),
    metric("Controles",controls.length,"bi-journal-check","Catálogo de referencia"),
    metric("Roles activos",roles.filter(r=>r.activo!==false).length,"bi-person-badge","Gobierno RBAC"),
    metric("Sectores",sectors.length,"bi-diagram-3","Contexto adaptable")
  ].join("");
  const top=companies.slice(0,6).map(c=>{const x=organizationSummaries.get(c.id);return `<div class="priority-card"><div class="d-flex justify-content-between"><h4>${App.esc(c.nombre)}</h4>${x?App.badge(x.estadoServicio):App.badge("SIN_SERVICIO")}</div><p>${x?`SoA ${x.soa.porcentaje}% · ${x.riesgos.total} riesgos · ${x.rpm.alertasActivas} alertas RPM`:'Aún no tiene un servicio SGSI asociado.'}</p></div>`;}).join("");
  document.getElementById("dashboardOrganizations").innerHTML=top||'<div class="empty"><i class="bi bi-building"></i>No hay organizaciones.</div>';
  document.getElementById("systemStatus").innerHTML=`<h4><i class="bi bi-check-circle"></i> Núcleo funcional listo</h4><p>${controls.length===93?'El catálogo contiene 93 controles de referencia.':`El catálogo contiene ${controls.length} controles; ejecute la verificación.`} Los indicadores se calculan desde MySQL y no desde valores simulados.</p>`;
  document.getElementById("recentServices").innerHTML=services.slice(0,8).map(s=>`<tr><td>${s.id}</td><td>${App.esc(s.empresa?.nombre||'—')}</td><td>${App.esc(s.sector?.nombre||'—')}</td><td>${App.badge(s.estado)}</td><td>${App.fmtDate(s.fechaCreacion)}</td><td><button class="btn-app" onclick="openOrganization(${s.empresa?.id})">Ver</button></td></tr>`).join("")||'<tr><td colspan="6" class="empty">No hay servicios.</td></tr>';
}
function latestService(companyId){return services.find(s=>String(s.empresa?.id)===String(companyId));}
function renderOrganizations(){
  const q=document.getElementById("organizationSearch").value.toLowerCase();const status=document.getElementById("organizationStatus").value;
  const data=companies.filter(c=>c.nombre.toLowerCase().includes(q)).filter(c=>!status||latestService(c.id)?.estado===status);
  document.getElementById("organizationsTable").innerHTML=data.map(c=>{const s=latestService(c.id),x=organizationSummaries.get(c.id);return `<tr><td><strong>${App.esc(c.nombre)}</strong><br><small class="text-muted-app">ID ${c.id}</small></td><td>${s?`#${s.id}`:'—'}</td><td>${App.esc(s?.sector?.nombre||'Sin sector')}</td><td>${App.badge(s?.estado||'SIN_SERVICIO')}</td><td>${x?App.progress(x.soa.porcentaje):'—'}</td><td>${x?`${x.riesgos.total} · ${App.badge(x.riesgos.criticos?'CRITICO':'BAJO')}`:'—'}</td><td>${x?`${x.rpm.alertasActivas} alertas`:'—'}</td><td class="nowrap"><button class="btn-app" onclick="openOrganization(${c.id})"><i class="bi bi-eye"></i></button> <button class="btn-app" onclick="selectContext(${c.id})"><i class="bi bi-sliders"></i></button> <button class="btn-app" onclick="selectReport(${c.id})"><i class="bi bi-file-earmark"></i></button></td></tr>`;}).join("")||'<tr><td colspan="8" class="empty">No se encontraron organizaciones.</td></tr>';
}
function renderUsers(){
  const q=document.getElementById("userSearch").value.toLowerCase(),rf=document.getElementById("userRoleFilter").value;
  const data=users.filter(u=>[u.nombre,u.email,u.empresa?.nombre].some(v=>String(v||'').toLowerCase().includes(q))).filter(u=>!rf||u.rol?.nombre===rf);
  document.getElementById("usersTable").innerHTML=data.map(u=>`<tr><td><strong>${App.esc(u.nombre)}</strong></td><td>${App.esc(u.email)}</td><td>${App.badge(u.rol?.nombre||'SIN_ROL')}</td><td>${App.esc(u.empresa?.nombre||'Rol global')}</td><td><button class="btn-app" onclick="openUserModal(${u.id})"><i class="bi bi-pencil"></i></button> <button class="btn-app btn-danger-app" onclick="deleteUser(${u.id})"><i class="bi bi-trash"></i></button></td></tr>`).join("")||'<tr><td colspan="5" class="empty">No hay usuarios.</td></tr>';
}
function renderRoles(){
  document.getElementById("rolesGrid").innerHTML=roles.map(r=>`<div class="card-app"><div class="d-flex justify-content-between"><h3>${App.esc(App.pretty(r.nombre))}</h3>${App.badge(r.activo===false?'INACTIVO':'ACTIVO')}</div><p class="small-app text-muted-app">${App.esc(r.descripcion||'Sin descripción')}</p><details><summary class="small-app link-app">Permisos técnicos</summary><pre class="small-app mt-2" style="white-space:pre-wrap">${App.esc(formatPermissions(r.permisos))}</pre></details><button class="btn-app mt-2" onclick="toggleRole(${r.id},${r.activo===false})">${r.activo===false?'Activar':'Desactivar'}</button></div>`).join("");
}
function formatPermissions(p){try{return JSON.stringify(typeof p==='string'?JSON.parse(p):p,null,2);}catch{return p||'{}';}}
function renderControls(){
  const q=document.getElementById("controlSearch").value.toLowerCase(),d=document.getElementById("controlDomain").value;
  const data=controls.filter(c=>[c.codigo,c.titulo,c.etiquetas,c.preguntaEvaluacion].some(v=>String(v||'').toLowerCase().includes(q))).filter(c=>!d||c.dominio===d);
  document.getElementById("controlsTable").innerHTML=data.map(c=>`<tr><td><strong>${App.esc(c.codigo)}</strong></td><td>${App.badge(c.dominio)}</td><td><strong>${App.esc(c.titulo)}</strong><br><small class="text-muted-app">${App.esc(c.descripcion||'')}</small></td><td>${App.esc(c.preguntaEvaluacion||'')}</td><td>${String(c.etiquetas||'').split(',').filter(Boolean).map(t=>`<span class="signal">${App.esc(t.trim())}</span>`).join(' ')}</td></tr>`).join("")||'<tr><td colspan="5" class="empty">Sin resultados.</td></tr>';
}
function renderContextChecks(){document.getElementById("contextChecks").innerHTML=contextFlags.map(([id,label])=>`<label class="form-check-app"><input type="checkbox" id="ctx_${id}"> ${label}</label>`).join("");}
async function saveCompany(e){e.preventDefault();try{const c=await App.api("/empresas",{method:"POST",body:JSON.stringify({nombre:document.getElementById("companyName").value})});App.modal("companyModal").hide();e.target.reset();App.toast("Organización creada. Ahora puede crear su servicio SGSI.");await refreshAll();document.getElementById("serviceCompany").value=c.id;App.modal("serviceModal").show();}catch(err){App.toast(err.message,"error");}}
async function saveService(e){e.preventDefault();try{await App.api("/servicios",{method:"POST",body:JSON.stringify({empresa:{id:+document.getElementById("serviceCompany").value},sector:{id:+document.getElementById("serviceSector").value},estado:document.getElementById("serviceStatus").value})});App.modal("serviceModal").hide();App.toast("Servicio creado con SoA de 93 controles.");await refreshAll();}catch(err){App.toast(err.message,"error");}}
function openUserModal(id){const u=users.find(x=>x.id===id);document.getElementById("userId").value=u?.id||"";document.getElementById("userName").value=u?.nombre||"";document.getElementById("userEmail").value=u?.email||"";document.getElementById("userPassword").value="";document.getElementById("userRole").value=u?.rol?.id||roles[0]?.id||"";document.getElementById("userCompany").value=u?.empresa?.id||"";document.getElementById("userModalTitle").textContent=u?'Editar usuario':'Crear usuario';App.modal("userModal").show();}
async function saveUser(e){e.preventDefault();const id=document.getElementById("userId").value;const body={nombre:document.getElementById("userName").value,email:document.getElementById("userEmail").value,rawPassword:document.getElementById("userPassword").value||null,rolId:+document.getElementById("userRole").value,empresaId:document.getElementById("userCompany").value?+document.getElementById("userCompany").value:null};try{await App.api(`/usuarios${id?'/'+id:''}`,{method:id?'PUT':'POST',body:JSON.stringify(body)});App.modal("userModal").hide();App.toast("Usuario guardado.");await refreshAll();}catch(err){App.toast(err.message,"error");}}
async function deleteUser(id){if(!confirm("¿Eliminar este usuario?"))return;try{await App.api(`/usuarios/${id}`,{method:"DELETE"});App.toast("Usuario eliminado.");await refreshAll();}catch(e){App.toast(e.message,"error");}}
async function toggleRole(id,activate){try{await App.api(`/roles/${id}/estado`,{method:"PATCH",body:JSON.stringify({activo:activate})});App.toast("Estado del rol actualizado.");roles=await App.api("/roles");renderRoles();}catch(e){App.toast(e.message,"error");}}
async function reloadCatalog(){try{const r=await App.api("/catalogo-controles/cargar-base",{method:"POST"});controls=await App.api("/catalogo-controles");renderControls();renderDashboard();App.toast(`Catálogo verificado: ${r.total} controles (${r.creados} nuevos).`);}catch(e){App.toast(e.message,"error");}}
async function loadContext(){const companyId=+document.getElementById("contextCompany").value;if(!companyId)return;try{const c=await App.api(`/contexto/empresa/${companyId}`);document.getElementById("ctxSector").value=c.sector?.id||latestService(companyId)?.sector?.id||sectors[0]?.id||"";document.getElementById("ctxSize").value=c.tamano||"PEQUENA";document.getElementById("ctxOwner").value=c.responsableSgsi||"";document.getElementById("ctxThreshold").value=c.umbralAceptacion||6;document.getElementById("ctxScope").value=c.alcanceSgsi||"";contextFlags.forEach(([id])=>document.getElementById(`ctx_${id}`).checked=!!c[id]);const s=latestService(companyId);document.getElementById("contextServiceInfo").innerHTML=s?`<h4>Servicio #${s.id}</h4><p>${App.esc(s.sector?.nombre)} · ${App.pretty(s.estado)}. Al guardar, reinicialice o actualice la SoA para recalcular la relevancia contextual.</p>`:'<h4>Sin servicio</h4><p>Cree un servicio antes de trabajar la SoA.</p>';}catch(e){App.toast(e.message,"error");}}
async function saveContext(e){e.preventDefault();const companyId=+document.getElementById("contextCompany").value;const body={sectorId:+document.getElementById("ctxSector").value,tamano:document.getElementById("ctxSize").value,responsableSgsi:document.getElementById("ctxOwner").value,umbralAceptacion:+document.getElementById("ctxThreshold").value,alcanceSgsi:document.getElementById("ctxScope").value};contextFlags.forEach(([id])=>body[id]=document.getElementById(`ctx_${id}`).checked);try{await App.api(`/contexto/empresa/${companyId}`,{method:"PUT",body:JSON.stringify(body)});const s=latestService(companyId);if(s)await App.api(`/soa/servicio/${s.id}/inicializar`,{method:"POST"});App.toast("Contexto guardado y recomendaciones SoA recalculadas.");await refreshAll();}catch(err){App.toast(err.message,"error");}}
function selectContext(id){document.querySelector('[data-section="contexto"]').click();document.getElementById("contextCompany").value=id;loadContext();}
function selectReport(id){document.querySelector('[data-section="reportes"]').click();document.getElementById("reportCompany").value=id;previewReport();}
function openOrganization(id){selectReport(id);}
async function previewReport(){const id=+document.getElementById("reportCompany").value;if(!id)return;try{const r=await App.api(`/reportes/empresa/${id}`);document.getElementById("reportPreview").innerHTML=`<div class="grid grid-3">${metric("Avance SoA",`${r.porcentajeImplementacion}%`,"bi-list-check")}${metric("Riesgos",r.totalRiesgos,"bi-exclamation-diamond",`${r.riesgosCriticos} críticos`)}${metric("Alertas RPM",r.alertasRpm,"bi-activity")}</div><div class="priority-card"><h4>${App.esc(r.empresaNombre||'Organización')}</h4><p>${r.totalControles} controles · ${r.controlesAplicables} aplicables · ${r.evidenciasValidadas} evidencias validadas · ${r.totalCapacitaciones} capacitaciones.</p></div>`;}catch(e){App.toast(e.message,"error");}}
async function downloadReport(type){const id=+document.getElementById("reportCompany").value;if(!id)return;try{const blob=await App.api(`/reportes/empresa/${id}/${type}`);App.downloadBlob(blob,`global_iso_empresa_${id}.${type==='pdf'?'pdf':'xlsx'}`);}catch(e){App.toast(e.message,"error");}}
