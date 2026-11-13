const $ = (id) => document.getElementById(id);
let currentUser = null;
let activeExam = null;
let timerInterval = null;

function formBody(form) {
  return new URLSearchParams(new FormData(form)).toString();
}
async function api(path, options = {}) {
  const response = await fetch(path, { credentials: "same-origin", ...options });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || "Something went wrong.");
  return data;
}
function showMessage(id, message, good = false) {
  const el = $(id); el.textContent = message; el.style.color = good ? "#31977e" : "#c95e89";
}
function toast(message) {
  $("toast").textContent = message; $("toast").classList.add("show");
  setTimeout(() => $("toast").classList.remove("show"), 2600);
}
function showOnly(id) {
  ["authView","dashboardView","examView","adminView"].forEach(view => $(view).classList.add("hidden"));
  $(id).classList.remove("hidden");
}
function setSignedIn(user) {
  currentUser = user;
  $("navUser").textContent = `${user.name} · ${user.role.toLowerCase()}`;
  $("logoutBtn").classList.remove("hidden");
  if (user.role === "ADMIN") { showOnly("adminView"); loadAdminExams(); loadAdminReports(); }
  else { showOnly("dashboardView"); $("welcomeName").textContent = user.name.split(" ")[0]; loadDashboard(); }
}
async function loadDashboard() {
  try {
    const exams = await api("/api/exams");
    $("examList").innerHTML = exams.length ? exams.map(exam => `
      <article class="exam-card"><div class="exam-icon">✦</div><h3>${escapeHtml(exam.title)}</h3>
      <p>${escapeHtml(exam.description || "A focused assessment to test your understanding.")}</p>
      <div class="exam-meta">◷ ${exam.duration} minutes · Multiple choice</div>
      <button class="primary" onclick="startExam(${exam.id}, '${escapeJs(exam.title)}', '${escapeJs(exam.description || "")}', ${exam.duration})">Start examination →</button></article>`).join("") : '<p class="muted">No examinations are available yet. Please check back later.</p>';
    const results = await api("/api/results");
    $("resultsList").innerHTML = results.length ? results.map(r => `<div class="result-row"><span>${escapeHtml(r.title)} <small class="muted">· ${escapeHtml(r.submittedAt || "")}</small></span><span class="score">${r.score} / ${r.total}</span></div>`).join("") : '<div class="result-row muted">Your submitted results will appear here.</div>';
  } catch (e) { toast(e.message); }
}
function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, c => ({ "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;" }[c]));
}
function escapeJs(value) { return String(value).replace(/\\/g,"\\\\").replace(/'/g,"\\'").replace(/\n/g," "); }
async function startExam(id, title, description, duration) {
  try {
    const questions = await api(`/api/questions?id=${id}`);
    if (!questions.length) { toast("This examination has no questions yet."); return; }
    activeExam = { id, title, duration, questions };
    $("examTitle").textContent = title; $("examDescription").textContent = description;
    $("durationLabel").textContent = `${duration} minutes`;
    $("questionList").innerHTML = questions.map((q, index) => `
      <article class="question-card"><span class="eyebrow">QUESTION ${index + 1}</span><h3>${escapeHtml(q.text)}</h3>
      ${q.options.map((option, i) => `<label><input type="radio" name="answer_${q.id}" value="${"ABCD"[i]}"> <span><b>${"ABCD"[i]}.</b> ${escapeHtml(option)}</span></label>`).join("")}</article>`).join("");
    showOnly("examView"); startTimer(duration * 60);
  } catch (e) { toast(e.message); }
}
function startTimer(seconds) {
  clearInterval(timerInterval); let remaining = seconds;
  const update = () => {
    const mins = Math.floor(remaining / 60), secs = remaining % 60;
    $("durationLabel").textContent = `${String(mins).padStart(2,"0")}:${String(secs).padStart(2,"0")}`;
    if (remaining <= 0) { clearInterval(timerInterval); submitExam(true); }
    remaining--;
  };
  update(); timerInterval = setInterval(update, 1000);
}
async function loadAdminExams() {
  try {
    const exams = await api("/api/exams");
    $("adminExamSelect").innerHTML = exams.map(e => `<option value="${e.id}">${escapeHtml(e.title)}</option>`).join("");
    if (!exams.length) $("adminExamSelect").innerHTML = '<option value="">Create an examination first</option>';
  } catch (e) { toast(e.message); }
}
async function loadAdminReports() {
  try {
    const [stats, users, results] = await Promise.all([
      api("/api/admin/stats"), api("/api/admin/users"), api("/api/admin/results")
    ]);
    $("adminStudentCount").textContent = stats.students;
    $("adminExamCount").textContent = stats.exams;
    $("adminAttemptCount").textContent = stats.attempts;
    $("adminUsersBody").innerHTML = users.length ? users.map(u => `
      <tr><td>${u.id}</td><td>${escapeHtml(u.name)}</td><td>${escapeHtml(u.email)}</td>
      <td><span class="role-chip">${escapeHtml(u.role)}</span></td><td>${escapeHtml(u.createdAt || "")}</td></tr>`).join("")
      : '<tr><td colspan="5" class="empty-cell">No registered users found.</td></tr>';
    $("adminResultsBody").innerHTML = results.length ? results.map(r => `
      <tr><td>${escapeHtml(r.studentName)}</td><td>${escapeHtml(r.email)}</td><td>${escapeHtml(r.examTitle)}</td>
      <td><strong class="score">${r.score} / ${r.total}</strong></td><td>${escapeHtml(r.submittedAt || "")}</td></tr>`).join("")
      : '<tr><td colspan="5" class="empty-cell">No examination attempts have been submitted yet.</td></tr>';
  } catch (e) {
    toast(e.message);
    $("adminUsersBody").innerHTML = '<tr><td colspan="5" class="empty-cell">Could not load user information.</td></tr>';
    $("adminResultsBody").innerHTML = '<tr><td colspan="5" class="empty-cell">Could not load examination results.</td></tr>';
  }
}
$("refreshAdminData").addEventListener("click", loadAdminReports);

document.querySelectorAll(".tab").forEach(tab => tab.addEventListener("click", () => {
  document.querySelectorAll(".tab").forEach(t => t.classList.toggle("active", t === tab));
  $("loginForm").classList.toggle("hidden", tab.dataset.tab !== "login");
  $("registerForm").classList.toggle("hidden", tab.dataset.tab !== "register");
  $("authMessage").textContent = "";
}));
$("loginForm").addEventListener("submit", async e => {
  e.preventDefault();
  try { const user = await api("/api/login", { method:"POST", headers:{"Content-Type":"application/x-www-form-urlencoded"}, body:formBody(e.target) }); setSignedIn(user); }
  catch (err) { showMessage("authMessage", err.message); }
});
$("registerForm").addEventListener("submit", async e => {
  e.preventDefault();
  try {
    const result = await api("/api/register", { method:"POST", headers:{"Content-Type":"application/x-www-form-urlencoded"}, body:formBody(e.target) });
    showMessage("authMessage", result.message, true); e.target.reset();
    document.querySelector('[data-tab="login"]').click();
  } catch (err) { showMessage("authMessage", err.message); }
});
$("logoutBtn").addEventListener("click", async () => {
  try { await api("/api/logout", {method:"POST"}); } catch (_) {}
  clearInterval(timerInterval); currentUser = null; $("navUser").textContent = "";
  $("logoutBtn").classList.add("hidden"); showOnly("authView");
});
$("backBtn").addEventListener("click", () => { clearInterval(timerInterval); showOnly("dashboardView"); });
async function submitExam(auto = false) {
  if (!activeExam) return;
  clearInterval(timerInterval);
  const body = new URLSearchParams(); body.set("examId", activeExam.id);
  activeExam.questions.forEach(q => {
    const chosen = document.querySelector(`input[name="answer_${q.id}"]:checked`);
    if (chosen) body.set(`answer_${q.id}`, chosen.value);
  });
  try {
    const result = await api("/api/submit", {method:"POST", headers:{"Content-Type":"application/x-www-form-urlencoded"}, body:body.toString()});
    toast(`${auto ? "Time ended. " : ""}Submitted! Your score: ${result.score}`);
    activeExam = null; showOnly("dashboardView"); loadDashboard();
  } catch (e) { toast(e.message); }
}
$("examForm").addEventListener("submit", e => { e.preventDefault(); if (confirm("Submit this examination now?")) submitExam(false); });
$("createExamForm").addEventListener("submit", async e => {
  e.preventDefault();
  try {
    const result = await api("/api/admin/exams", {method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:formBody(e.target)});
    showMessage("createExamMessage", `Created examination #${result.id}.`, true); e.target.reset(); loadAdminExams();
  } catch (err) { showMessage("createExamMessage", err.message); }
});
$("addQuestionForm").addEventListener("submit", async e => {
  e.preventDefault();
  try {
    const result = await api("/api/admin/questions", {method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:formBody(e.target)});
    showMessage("questionMessage", result.message, true); e.target.reset();
  } catch (err) { showMessage("questionMessage", err.message); }
});
(async function restoreSession() {
  try { const user = await api("/api/me"); setSignedIn(user); }
  catch (_) { showOnly("authView"); }
})();
