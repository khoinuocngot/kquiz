"use strict";

(() => {
  const DB_NAME = "kquiz_web_full_v1";
  const DB_VERSION = 2;
  const APP_VERSION = "web-1.1.0";
  const STORE_NAMES = [
    "studySets",
    "flashcards",
    "quizHistory",
    "studyStats",
    "folders",
    "tags",
    "settings",
    "achievements",
    "unlockSessions",
    "adRewards"
  ];

  const CDN = {
    pdfjs: "https://cdn.jsdelivr.net/npm/pdfjs-dist@4.10.38/build/pdf.min.mjs",
    pdfWorker: "https://cdn.jsdelivr.net/npm/pdfjs-dist@4.10.38/build/pdf.worker.min.mjs",
    tesseract: "https://cdn.jsdelivr.net/npm/tesseract.js@5.1.1/dist/tesseract.min.js",
    pdfLib: "https://cdn.jsdelivr.net/npm/pdf-lib@1.17.1/dist/pdf-lib.min.js",
    qrcode: "https://cdn.jsdelivr.net/npm/qrcodejs@1.0.0/qrcode.min.js",
    jsqr: "https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.min.js",
    mammoth: "https://cdn.jsdelivr.net/npm/mammoth@1.8.0/mammoth.browser.min.js"
  };

  const defaultSettings = {
    theme: "kquiz_blue",
    dailyGoal: 10,
    voiceEnabled: true,
    sfxEnabled: true,
    sfxVolume: 0.7,
    effects: {
      correct: true,
      wrong: true,
      flip: true,
      complete: true,
      select: true
    },
    aiEndpoint: "",
    ads: {
      enabled: false,
      networkCode: "",
      adUnitPath: "",
      timeoutMs: 15000
    }
  };

  const state = {
    db: null,
    route: "home",
    params: {},
    settings: { ...defaultSettings },
    studySets: [],
    cards: [],
    folders: [],
    history: [],
    stats: {},
    achievements: {},
    adRewards: [],
    modules: {},
    quick: {
      type: "MULTIPLE_CHOICE",
      termDelimiter: "tab",
      cardDelimiter: "line",
      title: "",
      description: "",
      raw: "",
      preview: null
    },
    studyFilters: {
      query: "",
      sort: "smart",
      folder: "all",
      tag: "all"
    },
    cardSelection: new Set(),
    importPreview: null,
    pendingUnlock: null,
    importFile: {
      mode: "FREE",
      file: null,
      text: "",
      generated: null,
      status: ""
    },
    smartScan: {
      files: [],
      mode: "HIGH_CONTRAST_BW",
      pdfBlob: null,
      pageCount: 0,
      busy: false
    },
    quizConfig: {
      count: 10,
      difficulty: "Trung bình",
      source: "all",
      direction: "front_to_back",
      shuffle: true,
      timerMinutes: 0,
      autoSubmit: false
    },
    quiz: null,
    flash: { index: 0, flipped: false, mode: "normal", cards: [] },
    learn: null,
    test: null,
    qr: { stream: null, scanning: false },
    processingNext: null,
    testTimer: null
  };

  const $ = (selector) => document.querySelector(selector);
  const $$ = (selector) => Array.from(document.querySelectorAll(selector));
  const app = $("#app");
  const modalRoot = $("#modal-root");
  const toastRoot = $("#toast-root");

  const icons = {
    home: svg("M3 10.5 12 3l9 7.5V21a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1v-10.5Z"),
    cards: svg("M5 4h14a2 2 0 0 1 2 2v10H7a2 2 0 0 0-2 2V4Zm0 14a2 2 0 0 0 2 2h14"),
    history: svg("M3 12a9 9 0 1 0 3-6.7M3 4v6h6M12 7v6l4 2"),
    info: svg("M12 8h.01M11 12h1v4h1M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"),
    volume: svg("M11 5 6 9H3v6h3l5 4V5Zm4 4a4 4 0 0 1 0 6m3-9a8 8 0 0 1 0 12"),
    plus: svg("M12 5v14M5 12h14"),
    file: svg("M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9l-6-6Zm0 0v6h6M8 13h8M8 17h8"),
    scan: svg("M4 7V5a1 1 0 0 1 1-1h2m10 0h2a1 1 0 0 1 1 1v2M4 17v2a1 1 0 0 0 1 1h2m10 0h2a1 1 0 0 0 1-1v-2M8 12h8"),
    folder: svg("M3 7a2 2 0 0 1 2-2h5l2 2h7a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7Z"),
    star: svg("m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1L12 16.9 6.6 19.8l1-6.1-4.4-4.3 6.1-.9L12 3Z"),
    pin: svg("M14 4 20 10l-4 1-4 7-2-2-4 4-2-2 4-4-2-2 7-4 1-4Z"),
    edit: svg("M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5Z"),
    copy: svg("M8 8h11v11H8zM5 16H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h11a1 1 0 0 1 1 1v1"),
    trash: svg("M3 6h18M8 6V4h8v2m-9 0 1 15h8l1-15"),
    back: svg("M19 12H5m7-7-7 7 7 7"),
    menu: svg("M4 7h16M4 12h16M4 17h10"),
    more: svg("M12 5h.01M12 12h.01M12 19h.01"),
    search: svg("M21 21l-4.3-4.3M10.5 18a7.5 7.5 0 1 1 0-15 7.5 7.5 0 0 1 0 15Z"),
    layers: svg("M12 3 3 8l9 5 9-5-9-5Zm-7 9 7 4 7-4M5 16l7 4 7-4"),
    learn: svg("M4 19.5A2.5 2.5 0 0 1 6.5 17H20M4 4.5A2.5 2.5 0 0 1 6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15Z"),
    test: svg("M9 11l2 2 4-5M7 3h10a2 2 0 0 1 2 2v14l-3-2-3 2-3-2-3 2V5a2 2 0 0 1 2-2Z"),
    qr: svg("M4 4h6v6H4V4Zm10 0h6v6h-6V4ZM4 14h6v6H4v-6Zm10 0h2v2h-2v-2Zm4 0h2v6h-4v-2h2v-4Zm-4 4h2v2h-2v-2Z"),
    pdf: svg("M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9l-6-6Zm0 0v6h6M7 16h10M8 13h2m2 0h4"),
    download: svg("M12 3v12m0 0 4-4m-4 4-4-4M4 21h16"),
    upload: svg("M12 21V9m0 0 4 4m-4-4-4 4M4 3h16"),
    camera: svg("M4 7h3l2-3h6l2 3h3v12H4V7Zm8 10a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"),
    spark: svg("M12 2l1.8 6.2L20 10l-6.2 1.8L12 18l-1.8-6.2L4 10l6.2-1.8L12 2Zm7 12 .9 3.1L23 18l-3.1.9L19 22l-.9-3.1L15 18l3.1-.9L19 14Z"),
    close: svg("M6 6l12 12M18 6 6 18"),
    check: svg("M20 6 9 17l-5-5"),
    wrong: svg("M18 6 6 18M6 6l12 12"),
    tag: svg("M20 13 13 20 4 11V4h7l9 9Z"),
    gear: svg("M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.1 2.1-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5V20h-3v-.1a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.9.3l-.1.1-2.1-2.1.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H4v-3h.1a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.9l-.1-.1 2.1-2.1.1.1a1.7 1.7 0 0 0 1.9.3 1.7 1.7 0 0 0 1-1.5V4h3v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1 2.1 2.1-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.5 1h.1v3h-.1a1.7 1.7 0 0 0-1.5 1Z")
  };

  function svg(path) {
    return `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${path}"/></svg>`;
  }

  function escapeHtml(value) {
    return String(value ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function now() {
    return Date.now();
  }

  function todayKey(date = new Date()) {
    return date.toISOString().slice(0, 10);
  }

  function formatDate(ts) {
    if (!ts) return "";
    return new Date(ts).toLocaleDateString("vi-VN", { day: "2-digit", month: "short", year: "numeric" });
  }

  function uid(prefix = "id") {
    return `${prefix}_${Math.random().toString(36).slice(2)}_${Date.now().toString(36)}`;
  }

  function clamp(value, min, max) {
    return Math.min(max, Math.max(min, value));
  }

  function shuffle(items) {
    const arr = [...items];
    for (let i = arr.length - 1; i > 0; i -= 1) {
      const j = Math.floor(Math.random() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
  }

  function normalizeText(text) {
    return String(text || "").trim().replace(/\s+/g, " ");
  }

  function safeFileName(text) {
    return normalizeText(text)
      .toLowerCase()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-|-$/g, "")
      .slice(0, 60) || "kquiz";
  }

  function toast(message, type = "default") {
    const node = document.createElement("div");
    node.className = `toast ${type}`;
    node.textContent = message;
    toastRoot.appendChild(node);
    setTimeout(() => node.remove(), 3200);
  }

  function openModal(html) {
    modalRoot.innerHTML = `<div class="modal-backdrop" onclick="KQuiz.closeModal(event)"><div class="modal" onclick="event.stopPropagation()">${html}</div></div>`;
  }

  function closeModal(event) {
    if (!event || event.target.classList.contains("modal-backdrop")) {
      modalRoot.innerHTML = "";
    }
  }

  function screenHeader(title, subtitle = "", right = "", backTo = "home") {
    return `
      <div class="top-row">
        <div class="screen-title">
          <button class="icon-btn" type="button" onclick="KQuiz.navigate('${backTo}')">${icons.back}</button>
          <div>
            <h1>${escapeHtml(title)}</h1>
            ${subtitle ? `<small>${escapeHtml(subtitle)}</small>` : ""}
          </div>
        </div>
        ${right}
      </div>
    `;
  }

  function bottomNav() {
    const route = state.route;
    const shell = state.modules.shell || {};
    const hubs = shell.HUBS || [
      { key: "home", route: "home", label: "Home", icon: "home" },
      { key: "create", route: "create-hub", label: "Tạo", icon: "plus" },
      { key: "study", route: "study-hub", label: "Học", icon: "learn" },
      { key: "review", route: "review-hub", label: "Ôn", icon: "spark" },
      { key: "tools", route: "tools-hub", label: "Công cụ", icon: "gear" }
    ];
    const groups = shell.ROUTE_GROUPS || {};
    const isActive = (hub) => (groups[hub.key] || [hub.route]).includes(route);
    return `
      <nav class="bottom-nav" aria-label="Dieu huong chinh">
        ${hubs.map((hub) => `<button class="nav-item ${isActive(hub) ? "active" : ""}" type="button" onclick="KQuiz.navigate('${hub.route}')">${icons[hub.icon] || icons.home}<span>${escapeHtml(hub.label)}</span></button>`).join("")}
      </nav>
    `;
  }

  function getStore(name, mode = "readonly") {
    return state.db.transaction(name, mode).objectStore(name);
  }

  function requestToPromise(request) {
    return new Promise((resolve, reject) => {
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }

  async function dbGet(store, key) {
    return requestToPromise(getStore(store).get(key));
  }

  async function dbGetAll(store) {
    return requestToPromise(getStore(store).getAll());
  }

  async function dbPut(store, value) {
    return requestToPromise(getStore(store, "readwrite").put(value));
  }

  async function dbAdd(store, value) {
    return requestToPromise(getStore(store, "readwrite").add(value));
  }

  async function dbDelete(store, key) {
    return requestToPromise(getStore(store, "readwrite").delete(key));
  }

  async function dbClear(store) {
    return requestToPromise(getStore(store, "readwrite").clear());
  }

  async function dbGetByIndex(store, index, value) {
    return requestToPromise(getStore(store).index(index).getAll(value));
  }

  async function initDB() {
    state.db = await new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
      request.onupgradeneeded = (event) => {
        const db = event.target.result;
        if (!db.objectStoreNames.contains("studySets")) {
          const store = db.createObjectStore("studySets", { keyPath: "id" });
          store.createIndex("updatedAt", "updatedAt");
          store.createIndex("folderId", "folderId");
        }
        if (!db.objectStoreNames.contains("flashcards")) {
          const store = db.createObjectStore("flashcards", { keyPath: "id" });
          store.createIndex("studySetId", "studySetId");
          store.createIndex("position", "position");
        }
        if (!db.objectStoreNames.contains("quizHistory")) {
          const store = db.createObjectStore("quizHistory", { keyPath: "id" });
          store.createIndex("createdAt", "createdAt");
        }
        if (!db.objectStoreNames.contains("studyStats")) db.createObjectStore("studyStats", { keyPath: "date" });
        if (!db.objectStoreNames.contains("folders")) db.createObjectStore("folders", { keyPath: "id" });
        if (!db.objectStoreNames.contains("tags")) db.createObjectStore("tags", { keyPath: "id" });
        if (!db.objectStoreNames.contains("settings")) db.createObjectStore("settings", { keyPath: "key" });
        if (!db.objectStoreNames.contains("achievements")) db.createObjectStore("achievements", { keyPath: "id" });
        if (!db.objectStoreNames.contains("unlockSessions")) db.createObjectStore("unlockSessions", { keyPath: "id" });
        if (!db.objectStoreNames.contains("adRewards")) db.createObjectStore("adRewards", { keyPath: "id" });
      };
    });
  }

  async function loadAll() {
    const settings = await dbGet("settings", "appSettings");
    state.settings = { ...defaultSettings, ...(settings?.value || {}) };
    state.studySets = (await dbGetAll("studySets")).sort(sortStudySets);
    state.cards = await dbGetAll("flashcards");
    state.folders = (await dbGetAll("folders")).sort((a, b) => b.createdAt - a.createdAt);
    state.history = (await dbGetAll("quizHistory")).sort((a, b) => b.createdAt - a.createdAt);
    state.stats = Object.fromEntries((await dbGetAll("studyStats")).map((item) => [item.date, item]));
    state.achievements = Object.fromEntries((await dbGetAll("achievements")).map((item) => [item.id, item]));
    state.adRewards = await dbGetAll("adRewards").catch(() => []);
    document.documentElement.dataset.theme = state.settings.theme === "kquiz_blue" ? "" : state.settings.theme;
  }

  async function saveSettings(patch = {}) {
    state.settings = { ...state.settings, ...patch };
    await dbPut("settings", { key: "appSettings", value: state.settings });
    document.documentElement.dataset.theme = state.settings.theme === "kquiz_blue" ? "" : state.settings.theme;
  }

  function sortStudySets(a, b) {
    if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;
    if (a.isFavorite !== b.isFavorite) return a.isFavorite ? -1 : 1;
    return (b.updatedAt || 0) - (a.updatedAt || 0);
  }

  async function seedIfNeeded() {
    const sets = await dbGetAll("studySets");
    if (sets.length) return;
    const setId = uid("set");
    const set = {
      id: setId,
      title: "Bộ mẫu KQuiz",
      description: "Bộ học mẫu để thử flashcard, học và kiểm tra.",
      cardCount: 6,
      sourceType: "GENERATED",
      sourceFileName: "",
      studySetType: "TERM_DEFINITION",
      isPinned: false,
      isFavorite: false,
      folderId: null,
      tags: [],
      createdAt: now(),
      updatedAt: now()
    };
    const sample = [
      ["CPU", "Bộ xử lý trung tâm của máy tính"],
      ["RAM", "Bộ nhớ tạm dùng khi máy tính đang chạy"],
      ["ROM", "Bộ nhớ chỉ đọc, lưu dữ liệu lâu dài hơn RAM"],
      ["GPU", "Bộ xử lý đồ họa, chuyên xử lý hình ảnh và video"],
      ["SSD", "Ổ lưu trữ thể rắn có tốc độ đọc ghi nhanh"],
      ["Mainboard", "Bo mạch chủ kết nối các linh kiện máy tính"]
    ].map(([term, definition], index) => createCard(setId, { term, definition, position: index }));
    await dbPut("studySets", set);
    for (const card of sample) await dbPut("flashcards", card);
  }

  function createCard(studySetId, data = {}) {
    return {
      id: data.id || uid("card"),
      studySetId,
      term: data.term || "",
      definition: data.definition || "",
      itemType: data.itemType || "TERM_DEFINITION",
      choices: data.choices || [],
      correctChoiceIndex: Number.isInteger(data.correctChoiceIndex) ? data.correctChoiceIndex : -1,
      explanation: data.explanation || "",
      sourceSnippet: data.sourceSnippet || "",
      sourcePageStart: data.sourcePageStart ?? null,
      sourcePageEnd: data.sourcePageEnd ?? null,
      isStarred: Boolean(data.isStarred),
      masteryLevel: data.masteryLevel || 0,
      timesReviewed: data.timesReviewed || 0,
      timesCorrect: data.timesCorrect || 0,
      lastReviewedAt: data.lastReviewedAt || null,
      createdAt: data.createdAt || now(),
      position: data.position || 0
    };
  }

  function getCardsForSet(setId) {
    return state.cards.filter((card) => card.studySetId === setId).sort((a, b) => a.position - b.position);
  }

  function getSet(setId) {
    return state.studySets.find((set) => set.id === setId);
  }

  function setHash(route, params = {}) {
    const qs = new URLSearchParams(params).toString();
    location.hash = qs ? `#/${route}?${qs}` : `#/${route}`;
  }

  function parseHash() {
    const raw = location.hash.replace(/^#\/?/, "") || "home";
    const [path, query] = raw.split("?");
    state.route = path || "home";
    state.params = Object.fromEntries(new URLSearchParams(query || ""));
  }

  async function navigate(route, params = {}) {
    setHash(route, params);
  }

  async function render() {
    parseHash();
    await loadAll();
    const route = state.route;
    if (state.qr.stream && route !== "import-studyset") stopQrScanner();
    if (state.testTimer && route !== "test") {
      clearInterval(state.testTimer);
      state.testTimer = null;
    }

    const map = {
      home: renderHome,
      "create-hub": renderCreateHub,
      "study-hub": renderStudyHub,
      "review-hub": renderReviewHub,
      "tools-hub": renderToolsHub,
      "quick-import": renderQuickImport,
      "import-file": renderImportFile,
      processing: renderProcessing,
      "quiz-config": renderQuizConfig,
      quiz: renderQuiz,
      result: renderResult,
      history: renderHistory,
      "study-sets": renderStudySets,
      "study-detail": renderStudyDetail,
      flashcard: renderFlashcard,
      learn: renderLearn,
      "learn-result": renderLearnResult,
      "test-config": renderTestConfig,
      test: renderTest,
      "test-result": renderTestResult,
      "review-wrong": renderReviewWrong,
      folders: renderFolders,
      "folder-detail": renderFolderDetail,
      "smart-review": renderSmartReview,
      "smart-scan": renderSmartScan,
      "import-studyset": renderImportStudySet,
      about: renderAbout
    };
    const html = (map[route] || renderHome)();
    app.innerHTML = html + bottomNav();
    attachRouteEvents();
  }

  function attachRouteEvents() {
    const quickForm = $("#quickForm");
    if (quickForm) quickForm.addEventListener("input", syncQuickForm);
    const fileInput = $("#fileInput");
    if (fileInput) fileInput.addEventListener("change", handleImportFileInput);
    const importStudyInput = $("#importStudyInput");
    if (importStudyInput) importStudyInput.addEventListener("change", handleStudySetFileInput);
    const backupInput = $("#backupInput");
    if (backupInput) backupInput.addEventListener("change", handleBackupInput);
    const smartInput = $("#smartScanInput");
    if (smartInput) smartInput.addEventListener("change", handleSmartScanInput);
    const cameraInput = $("#cameraInput");
    if (cameraInput) cameraInput.addEventListener("change", handleImportFileInput);
    const search = $("#studySearch");
    if (search) search.addEventListener("input", updateStudyFilters);
    ["studySort", "studyFolderFilter", "studyTagFilter"].forEach((id) => {
      const node = $("#" + id);
      if (node) node.addEventListener("change", updateStudyFilters);
    });
    const cardSearch = $("#cardSearch");
    if (cardSearch) cardSearch.addEventListener("input", () => renderCardList(cardSearch.value));
    const folderName = $("#folderName");
    if (folderName) folderName.focus();
  }

  function renderHome() {
    const totalCards = state.cards.length;
    const today = state.stats[todayKey()] || {};
    const needReview = state.cards.filter((card) => card.masteryLevel < 3).length;
    const mastered = state.cards.filter((card) => card.masteryLevel >= 4).length;
    const xp = state.settings.totalXp || 0;
    const level = Math.floor(xp / 100) + 1;
    const progress = xp % 100;
    const badges = getUnlockedBadges();
    const greeting = getGreeting();
    const dailyPercent = clamp(Math.round(((today.cardsReviewed || 0) / Math.max(1, state.settings.dailyGoal)) * 100), 0, 100);

    return `
      <section class="screen">
        <div class="top-row">
          <div class="title-stack">
            <h1 class="title">Kquiz</h1>
            <p class="subtitle">${greeting}</p>
          </div>
          <div class="actions-inline">
            <button class="icon-btn plain" type="button" onclick="KQuiz.showAudioSettings()">${icons.volume}</button>
            <button class="icon-btn plain" type="button" onclick="KQuiz.navigate('about')">${icons.info}</button>
          </div>
        </div>

        <div class="hero-card">
          <h2>Học nhanh. Nhớ lâu.</h2>
          <p>Dashboard gọn cho tạo bộ học, học, ôn và công cụ giống app Android.</p>
          <div class="metric-row">
            <div class="metric"><strong>${state.studySets.length}</strong><span>bộ học</span></div>
            <div class="metric"><strong>${totalCards}</strong><span>thẻ</span></div>
            <div class="metric"><strong>${state.history.length}</strong><span>quiz</span></div>
          </div>
        </div>

        <div class="section compact">
          <div class="banner" onclick="KQuiz.navigate('review-hub')">
            <div class="glyph">${icons.spark}</div>
            <div><strong>${needReview} thẻ cần ôn</strong><span>Smart Review ưu tiên thẻ yếu và bài sai.</span></div>
            <div class="chev">›</div>
          </div>
        </div>

        <div class="section compact">
          <div class="card pad daily-widget">
            <div class="section-head">
              <h2>Cấp ${level}</h2>
              <button class="section-link" onclick="KQuiz.configureDailyGoal()">Mục tiêu</button>
            </div>
            <p class="small-text">${progress} / 100 XP • Hôm nay ${today.cardsReviewed || 0} / ${state.settings.dailyGoal} thẻ • ${mastered} thẻ thành thạo</p>
            <div class="progress" style="--value:${progress}%"><span></span></div>
            <div class="mini-dashboard" style="margin-top:12px"><span>Mục tiêu ngày</span><strong>${dailyPercent}%</strong></div>
          </div>
        </div>

        ${badges.length ? `
          <div class="section compact">
            <div class="section-head"><h2>Huy hiệu</h2></div>
            <div class="chip-row">${badges.map((badge) => `<span class="chip active">${escapeHtml(badge.title)}</span>`).join("")}</div>
          </div>
        ` : ""}

        <div class="section">
          <h2>Bắt đầu theo chức năng</h2>
          <div class="action-grid compact-grid" style="margin-top:14px">
            ${actionCard("Tạo", "Nhập nhanh, file, scan", icons.plus, "KQuiz.navigate('create-hub')")}
            ${actionCard("Học", "Bộ học, folder, tag", icons.learn, "KQuiz.navigate('study-hub')","secondary")}
            ${actionCard("Ôn", "Smart Review, test, lịch sử", icons.spark, "KQuiz.navigate('review-hub')","warning")}
            ${actionCard("Công cụ", "Backup, theme, ads, AI", icons.gear, "KQuiz.navigate('tools-hub')")}
          </div>
        </div>
      </section>
    `;
  }

  function renderCreateHub() {
    return `
      <section class="screen">
        ${screenHeader("Tạo", "Nhập nội dung và tạo quiz", "", "home")}
        <div class="action-grid">
          ${actionCard("Nhập nhanh", "Dán văn bản thành bộ học", icons.file, "KQuiz.navigate('quick-import')")}
          ${actionCard("Nhập từ file", "PDF, TXT, DOCX, ảnh", icons.upload, "KQuiz.showImportNotice()","secondary")}
          ${actionCard("Smart Scan Pro", "Ghép ảnh thành PDF scan", icons.scan, "KQuiz.navigate('smart-scan')","wide warning")}
          ${actionCard("Nhập bộ học", ".studyset, JSON hoặc QR", icons.qr, "KQuiz.navigate('import-studyset')","wide secondary")}
        </div>
      </section>
    `;
  }

  function renderStudyHub() {
    const recent = state.studySets.slice(0, 3);
    return `
      <section class="screen">
        ${screenHeader("Học", `${state.studySets.length} bộ • ${state.cards.length} thẻ`, `<button class="icon-btn" onclick="KQuiz.navigate('folders')">${icons.folder}</button>`, "home")}
        <div class="action-grid compact-grid">
          ${actionCard("Bộ học", "Tìm, lọc, sắp xếp", icons.learn, "KQuiz.navigate('study-sets')","secondary")}
          ${actionCard("Thư mục", "Tổ chức bộ học", icons.folder, "KQuiz.navigate('folders')")}
          ${actionCard("Trộn test", "Chọn nhiều bộ học", icons.layers, "KQuiz.showMixedTestModal()","wide warning")}
          ${actionCard("Tạo mới", "Quick Import", icons.plus, "KQuiz.navigate('quick-import')","wide")}
        </div>
        ${recent.length ? `<div class="section"><div class="section-head"><h2>Gần đây</h2><button class="section-link" onclick="KQuiz.navigate('study-sets')">Xem tất cả</button></div><div class="list">${recent.map(renderStudySetCard).join("")}</div></div>` : ""}
      </section>
    `;
  }

  function renderReviewHub() {
    const needReview = state.cards.filter((card) => card.masteryLevel < 3).length;
    const weakSets = state.studySets.filter((set) => getCardsForSet(set.id).some((card) => card.masteryLevel < 3)).slice(0, 3);
    return `
      <section class="screen">
        ${screenHeader("Ôn", `${needReview} thẻ ưu tiên`, "", "home")}
        <div class="action-grid compact-grid">
          ${actionCard("Smart Review", "Ôn thẻ yếu trước", icons.spark, "KQuiz.navigate('smart-review')","warning")}
          ${actionCard("Lịch sử", "Xem kết quả quiz", icons.history, "KQuiz.navigate('history')")}
          ${actionCard("Test bộ học", "Vào danh sách để chọn bộ", icons.test, "KQuiz.navigate('study-sets')","wide secondary")}
        </div>
        ${weakSets.length ? `<div class="section"><div class="section-head"><h2>Bộ cần ôn</h2></div><div class="list">${weakSets.map(renderStudySetCard).join("")}</div></div>` : `<div class="section empty-state card"><div><strong>Chưa có thẻ yếu</strong><span>Làm test hoặc học flashcard để tạo dữ liệu ôn.</span></div></div>`}
      </section>
    `;
  }

  function renderToolsHub() {
    const ads = state.settings.ads || defaultSettings.ads;
    const adReady = ads.enabled && (ads.adUnitPath || ads.networkCode);
    return `
      <section class="screen">
        ${screenHeader("Công cụ", "Backup, theme, ads, AI", "", "home")}
        <div class="section card pad">
          <h2>Dữ liệu</h2>
          <div class="btn-row" style="margin-top:12px">
            <button class="btn secondary" onclick="KQuiz.exportBackup()">${icons.download} Sao lưu</button>
            <button class="btn secondary" onclick="document.getElementById('backupInput').click()">${icons.upload} Khôi phục</button>
          </div>
          <input id="backupInput" class="hidden" type="file" accept=".kquizbackup,.json,application/json">
        </div>
        <div class="section card pad">
          <h2>Theme màu nhẹ</h2>
          <div class="chip-row" style="margin-top:12px">
            ${themeChip("kquiz_blue","Xanh KQuiz")}
            ${themeChip("mint","Mint")}
            ${themeChip("lavender","Lavender")}
            ${themeChip("peach","Peach")}
          </div>
        </div>
        <div class="section card pad">
          <h2>Âm thanh & mục tiêu</h2>
          <div class="btn-row" style="margin-top:12px">
            <button class="btn" onclick="KQuiz.showAudioSettings()">${icons.volume} Âm thanh</button>
            <button class="btn" onclick="KQuiz.configureDailyGoal()">Mục tiêu</button>
          </div>
        </div>
        <div class="section card pad">
          <h2>AI Pro endpoint</h2>
          <p class="small-text">Không lưu API key trong frontend. Chỉ nhập URL proxy hợp lệ.</p>
          <input id="aiEndpoint" class="input" value="${escapeHtml(state.settings.aiEndpoint || "")}" placeholder="https://your-worker.workers.dev">
          <button class="btn primary" style="width:100%;margin-top:12px" onclick="KQuiz.saveAiEndpoint()">Lưu endpoint</button>
        </div>
        <div class="section card pad">
          <h2>Rewarded Ads Web</h2>
          <p class="small-text">${adReady ? "Đã có cấu hình Ad Manager. Pro chỉ mở sau khi nhận reward." : "Chưa có ad unit. Pro sẽ không unlock cho tới khi cấu hình Ad Manager."}</p>
          <label class="option-card"><input id="adsEnabled" type="checkbox" ${ads.enabled ? "checked" : ""}> Bật Google Ad Manager rewarded</label>
          <label class="form-row"><span class="field-label">Network code</span><input id="adsNetworkCode" class="input" value="${escapeHtml(ads.networkCode || "")}" placeholder="1234567"></label>
          <label class="form-row"><span class="field-label">Ad unit path</span><input id="adsAdUnitPath" class="input" value="${escapeHtml(ads.adUnitPath || "")}" placeholder="/1234567/kquiz_rewarded"></label>
          <button class="btn primary" style="width:100%;margin-top:12px" onclick="KQuiz.saveAdsSettings()">Lưu ads</button>
        </div>
        <button class="action-card wide" style="width:100%;margin-top:28px" onclick="KQuiz.navigate('about')"><span class="glyph">${icons.info}</span><span><strong>Giới thiệu</strong><span>Thông tin app và ủng hộ KQuiz</span></span></button>
      </section>
    `;
  }

  function actionCard(title, sub, icon, handler, kind = "") {
    return `
      <button class="action-card ${kind}" type="button" onclick="${handler}">
        <span class="glyph">${icon}</span>
        <span><strong>${escapeHtml(title)}</strong><span>${escapeHtml(sub)}</span></span>
      </button>
    `;
  }

  function getGreeting() {
    const hour = new Date().getHours();
    if (hour < 11) return "Chào buổi sáng!";
    if (hour < 14) return "Trưa vui vẻ!";
    if (hour < 18) return "Chiều vui vẻ!";
    return "Tối học nhẹ nhàng!";
  }

  function showImportNotice() {
    openModal(`
      <button class="icon-btn plain" style="float:right" onclick="KQuiz.closeModal()">${icons.close}</button>
      <div class="glyph" style="width:64px;height:64px;border-radius:22px;background:var(--info-soft);color:var(--info);display:grid;place-items:center">${icons.file}</div>
      <h2>Lưu ý khi nhập từ file</h2>
      <p>Phương thức này hoạt động tốt với PDF, TXT, Word và ảnh. OCR/AI có thể chậm hơn tùy thiết bị và kết nối.</p>
      <div class="card pad" style="background:var(--info-soft);box-shadow:none">Hỗ trợ: PDF, TXT, DOCX, ảnh. Bạn vẫn có thể tiếp tục nhập file bình thường.</div>
      <div class="btn-row" style="margin-top:18px">
        <button class="btn" onclick="KQuiz.closeModal()">Đóng</button>
        <button class="btn primary" onclick="KQuiz.closeModal();KQuiz.navigate('import-file')">Tiếp tục</button>
      </div>
    `);
  }

  function renderQuickImport() {
    const preview = state.quick.preview || parseQuickImport();
    state.quick.preview = preview;
    return `
      <section class="screen no-bottom">
        <form id="quickForm">
          <div class="top-row">
            <button class="btn" type="button" onclick="KQuiz.navigate('create-hub')">Hủy</button>
            <button id="quickCreateBtn" class="btn primary" type="button" onclick="KQuiz.createStudySetFromQuick()">Tạo bộ học luôn</button>
          </div>
          <div class="chip-row" style="justify-content:center;margin-top:-8px">
            <span class="chip active"></span><span class="chip"></span><span class="chip"></span><span class="chip"></span><span class="chip"></span>
          </div>
          <div class="section">
            <h2>Chọn loại</h2>
            <div class="form-grid" style="margin-top:18px">
              <input class="input" name="title" placeholder="Tên bộ học" value="${escapeHtml(state.quick.title)}">
              <input class="input" name="description" placeholder="Mô tả (không bắt buộc)" value="${escapeHtml(state.quick.description)}">
              <div class="form-row">
                <label>${quickTypeLabel(state.quick.type)}</label>
                <div class="segmented">
                  ${quickTypeChip("TERM_DEFINITION", "Thuật ngữ - Định nghĩa")}
                  ${quickTypeChip("QUESTION_ANSWER", "Câu hỏi - Đáp án")}
                  ${quickTypeChip("MULTIPLE_CHOICE", "Trắc nghiệm")}
                </div>
              </div>
              <div class="btn-row">
                <label class="form-row"><span class="field-label">Phân cách T-D</span>${select("termDelimiter", state.quick.termDelimiter, [["tab","Tab"],["comma","Dấu phẩy"],["colon","Dấu hai chấm"],["pipe","Dấu |"],["arrow","Mũi tên ->"],["equals","Dấu ="]])}</label>
                <label class="form-row"><span class="field-label">Phân cách thẻ</span>${select("cardDelimiter", state.quick.cardDelimiter, [["line","Mỗi dòng là 1 thẻ"],["blank","Dòng trống"],["semicolon","Dấu ;"]])}</label>
              </div>
              <div class="form-row">
                <label>Nội dung</label>
                <textarea class="textarea" name="raw" placeholder="Dán nội dung vào đây...">${escapeHtml(state.quick.raw)}</textarea>
              </div>
              <button class="btn secondary" type="button" onclick="KQuiz.copyPrompt()">Copy prompt hỗ trợ</button>
              <div id="quickPreview">${quickPreviewHtml(preview)}</div>
            </div>
          </div>
        </form>
      </section>
    `;
  }

  function quickTypeLabel(type) {
    return {
      TERM_DEFINITION: "Thuật ngữ - Định nghĩa",
      QUESTION_ANSWER: "Câu hỏi - Đáp án",
      MULTIPLE_CHOICE: "Trắc nghiệm"
    }[type] || "Trắc nghiệm";
  }

  function quickPreviewHtml(preview) {
    return `
      <div class="card pad">
        <strong>Preview: ${preview.validCards.length} thẻ hợp lệ</strong>
        <p class="small-text">${preview.invalidLines.length ? `${preview.invalidLines.length} dòng lỗi cần sửa.` : "Không có dòng lỗi."}</p>
        <div class="list" style="margin-top:12px">${preview.validCards.slice(0, 5).map((card) => `<div class="flash-row"><h3>${escapeHtml(card.term)}</h3><p>${escapeHtml(card.definition)}</p></div>`).join("") || `<p class="small-text">Dán nội dung để xem trước.</p>`}</div>
      </div>
    `;
  }

  function quickTypeChip(type, label) {
    return `<button class="chip ${state.quick.type === type ? "active" : ""}" type="button" onclick="KQuiz.setQuickType('${type}')">${label}</button>`;
  }

  function select(name, value, options) {
    return `<select id="${name}" class="select" name="${name}">${options.map(([v, label]) => `<option value="${v}" ${value === v ? "selected" : ""}>${label}</option>`).join("")}</select>`;
  }

  function syncQuickForm() {
    const form = $("#quickForm");
    if (!form) return;
    const data = new FormData(form);
    state.quick.title = data.get("title") || "";
    state.quick.description = data.get("description") || "";
    state.quick.raw = data.get("raw") || "";
    state.quick.termDelimiter = data.get("termDelimiter") || "tab";
    state.quick.cardDelimiter = data.get("cardDelimiter") || "line";
    state.quick.preview = parseQuickImport();
    const preview = $("#quickPreview");
    if (preview) preview.innerHTML = quickPreviewHtml(state.quick.preview);
  }

  function setQuickType(type) {
    syncQuickForm();
    state.quick.type = type;
    state.quick.preview = parseQuickImport();
    render();
  }

  function delimiterValue(type) {
    return {
      tab: "\t",
      comma: ",",
      colon: ":",
      pipe: "|",
      arrow: "->",
      equals: "="
    }[type] || "\t";
  }

  function parseQuickImport(config = state.quick) {
    const raw = stripCodeFence(config.raw || "").replace(/\r\n/g, "\n").replace(/\r/g, "\n");
    const chunks = config.cardDelimiter === "blank"
      ? raw.split(/\n\s*\n/g)
      : config.cardDelimiter === "semicolon"
        ? raw.split(";")
        : raw.split("\n");
    const validCards = [];
    const invalidLines = [];
    const delimiter = delimiterValue(config.termDelimiter);
    chunks.map((line) => line.trim()).filter(Boolean).forEach((line, index) => {
      const pos = line.indexOf(delimiter);
      if (pos < 0) {
        invalidLines.push({ line, error: "Thiếu dấu phân cách", index });
        return;
      }
      const termRaw = line.slice(0, pos).trim();
      const def = line.slice(pos + delimiter.length).trim();
      if (!termRaw || !def) {
        invalidLines.push({ line, error: "Thiếu mặt trước hoặc mặt sau", index });
        return;
      }
      if (config.type === "MULTIPLE_CHOICE") {
        const mcq = extractMcq(termRaw, def);
        validCards.push({ term: mcq.question, definition: def, itemType: "MULTIPLE_CHOICE", choices: mcq.choices, correctChoiceIndex: mcq.correctChoiceIndex, sourceSnippet: line });
      } else {
        validCards.push({ term: termRaw, definition: def, itemType: config.type, choices: [], correctChoiceIndex: -1, sourceSnippet: line });
      }
    });
    return { validCards, invalidLines, totalLines: chunks.filter((x) => x.trim()).length };
  }

  function stripCodeFence(text) {
    const lines = String(text || "").split(/\r?\n/);
    const start = lines.findIndex((line) => line.trim().startsWith("```"));
    if (start >= 0) {
      const end = lines.findIndex((line, index) => index > start && line.trim().startsWith("```"));
      if (end > start) return lines.slice(start + 1, end).join("\n");
    }
    return text;
  }

  function extractMcq(questionWithOptions, answerText) {
    const regex = /(^|\s)([A-E])[\.\)]\s+/g;
    const matches = [...questionWithOptions.matchAll(regex)];
    if (!matches.length) return { question: questionWithOptions, choices: [], correctChoiceIndex: -1 };
    const first = matches[0].index + matches[0][1].length;
    const question = questionWithOptions.slice(0, first).trim();
    const choices = matches.map((match, index) => {
      const start = match.index + match[0].length;
      const end = matches[index + 1] ? matches[index + 1].index : questionWithOptions.length;
      return `${match[2]}. ${questionWithOptions.slice(start, end).trim()}`;
    });
    return { question, choices, correctChoiceIndex: resolveCorrectIndex(choices, answerText) };
  }

  function resolveCorrectIndex(choices, answerText) {
    if (!choices.length) return -1;
    const answer = normalizeText(answerText).toUpperCase();
    const letter = answer.match(/[A-E]/)?.[0];
    if (letter) {
      const idx = letter.charCodeAt(0) - 65;
      if (idx >= 0 && idx < choices.length) return idx;
    }
    const normalized = normalizeChoice(answerText);
    return choices.findIndex((choice) => normalizeChoice(choice) === normalized);
  }

  function normalizeChoice(text) {
    return normalizeText(text).replace(/^[A-E][\.\)]\s*/i, "").toLowerCase();
  }

  async function createStudySetFromQuick() {
    syncQuickForm();
    const parsed = parseQuickImport();
    if (!parsed.validCards.length) return toast("Chưa có thẻ hợp lệ.", "warning");
    const id = uid("set");
    const title = normalizeText(state.quick.title) || `Bộ học ${formatDate(now())}`;
    const set = {
      id,
      title,
      description: state.quick.description || "",
      cardCount: parsed.validCards.length,
      sourceType: "QUICK_IMPORTED",
      sourceFileName: "",
      studySetType: state.quick.type,
      isPinned: false,
      isFavorite: false,
      folderId: null,
      tags: [],
      createdAt: now(),
      updatedAt: now()
    };
    await dbPut("studySets", set);
    for (const [index, parsedCard] of parsed.validCards.entries()) {
      await dbPut("flashcards", createCard(id, { ...parsedCard, position: index }));
    }
    state.quick = { ...state.quick, title: "", description: "", raw: "", preview: null };
    await addXp(10);
    toast("Đã tạo bộ học.", "success");
    navigate("study-detail", { id });
  }

  async function copyPrompt() {
    const prompt = `Hãy chuyển nội dung thành format cho KQuiz. Mỗi dòng 1 thẻ, vế trái và vế phải cách nhau bằng TAB. Loại bộ học: ${quickTypeLabel(state.quick.type)}. Không giải thích, chỉ trả về code block.`;
    await navigator.clipboard?.writeText(prompt);
    toast("Đã copy prompt.", "success");
  }

  function renderStudySets() {
    const totalCards = state.cards.length;
    const tags = getAllTags();
    return `
      <section class="screen">
        ${screenHeader("Bộ học của tớ", `${state.studySets.length} bộ • ${totalCards} thẻ`, `<button class="icon-btn" onclick="KQuiz.navigate('folders')">${icons.menu}</button>`, "study-hub")}
        <div class="filter-panel card pad">
          <div class="form-row">
            <input id="studySearch" class="input" placeholder="Tìm bộ học..." autocomplete="off" value="${escapeHtml(state.studyFilters.query)}">
          </div>
          <div class="btn-row" style="margin-top:12px">
            <label class="form-row"><span class="field-label">Sắp xếp</span>${select("studySort", state.studyFilters.sort, [["smart","Ghim + mới nhất"],["updated","Mới cập nhật"],["title","A-Z"],["cards","Nhiều thẻ"]])}</label>
            <label class="form-row"><span class="field-label">Thư mục</span>${select("studyFolderFilter", state.studyFilters.folder, [["all","Tất cả"],["root","Chưa phân loại"],...state.folders.map((folder) => [folder.id, folder.name])])}</label>
          </div>
          <label class="form-row" style="margin-top:12px"><span class="field-label">Tag</span>${select("studyTagFilter", state.studyFilters.tag, [["all","Tất cả"], ...tags.map((tag) => [tag, `#${tag}`])])}</label>
        </div>
        <div class="btn-row" style="margin-top:14px">
          <button class="btn secondary" onclick="KQuiz.showMixedTestModal()">${icons.layers} Trộn nhiều bộ học</button>
          <button class="btn primary" onclick="KQuiz.navigate('quick-import')">${icons.plus} Tạo mới</button>
        </div>
        <div id="studyList" class="list" style="margin-top:18px">${renderStudySetList()}</div>
      </section>
    `;
  }

  function renderStudySetList(query = "") {
    const filters = { ...state.studyFilters, query: query || state.studyFilters.query };
    const q = normalizeText(filters.query).toLowerCase();
    let sets = state.studySets.filter((set) => {
      const haystack = `${set.title} ${set.description} ${(set.tags || []).join(" ")}`.toLowerCase();
      const folderOk = filters.folder === "all" || (filters.folder === "root" ? !set.folderId : set.folderId === filters.folder);
      const tagOk = filters.tag === "all" || (set.tags || []).includes(filters.tag);
      return (!q || haystack.includes(q)) && folderOk && tagOk;
    });
    sets = sortStudySetList(sets, filters.sort);
    return sets.length ? sets.map(renderStudySetCard).join("") : `<div class="empty-state card"><div><strong>Chưa có bộ học phù hợp</strong><span>Thử đổi bộ lọc hoặc tạo bộ học mới.</span></div></div>`;
  }

  function renderStudySetsIntoList(query = state.studyFilters.query) {
    const list = $("#studyList");
    if (list) list.innerHTML = renderStudySetList(query);
  }

  function updateStudyFilters() {
    state.studyFilters = {
      query: $("#studySearch")?.value || "",
      sort: $("#studySort")?.value || "smart",
      folder: $("#studyFolderFilter")?.value || "all",
      tag: $("#studyTagFilter")?.value || "all"
    };
    renderStudySetsIntoList();
  }

  function sortStudySetList(sets, mode) {
    const copy = [...sets];
    if (mode === "title") return copy.sort((a, b) => a.title.localeCompare(b.title, "vi"));
    if (mode === "cards") return copy.sort((a, b) => getCardsForSet(b.id).length - getCardsForSet(a.id).length);
    if (mode === "updated") return copy.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0));
    return copy.sort(sortStudySets);
  }

  function getAllTags() {
    return [...new Set(state.studySets.flatMap((set) => set.tags || []))].filter(Boolean).sort((a, b) => a.localeCompare(b, "vi"));
  }

  function renderStudySetCard(set) {
    const cards = getCardsForSet(set.id);
    const mastery = cards.length ? Math.round(cards.reduce((sum, card) => sum + card.masteryLevel, 0) / (cards.length * 5) * 100) : 0;
    return `
      <article class="study-card" onclick="KQuiz.navigate('study-detail',{id:'${set.id}'})">
        <div class="study-card-head">
          <div>
            <h3>${escapeHtml(set.title)}</h3>
            <p>${escapeHtml(set.description || "Bộ học mẫu để thử flashcard, học và kiểm tra.")}</p>
            ${(set.tags || []).length ? `<div class="chip-row inline-tags">${set.tags.map((tag) => `<span class="tag-chip">#${escapeHtml(tag)}</span>`).join("")}</div>` : ""}
          </div>
          <div class="card-actions" onclick="event.stopPropagation()">
            <button class="mini-btn ${set.isPinned ? "primary" : ""}" onclick="KQuiz.toggleSet('${set.id}','isPinned')">${icons.pin}</button>
            <button class="mini-btn ${set.isFavorite ? "primary" : ""}" onclick="KQuiz.toggleSet('${set.id}','isFavorite')">${icons.star}</button>
            <button class="mini-btn" onclick="KQuiz.renameSet('${set.id}')">${icons.edit}</button>
            <button class="mini-btn" onclick="KQuiz.duplicateSet('${set.id}')">${icons.copy}</button>
            <button class="mini-btn danger" onclick="KQuiz.deleteSet('${set.id}')">${icons.trash}</button>
          </div>
        </div>
        <div class="study-card-head" style="margin-top:14px">
          <strong>${cards.length} thẻ</strong>
          <span class="small-text">${formatDate(set.updatedAt)}</span>
        </div>
        <div class="progress" style="--value:${mastery}%"><span></span></div>
      </article>
    `;
  }

  async function toggleSet(id, field) {
    const set = await dbGet("studySets", id);
    if (!set) return;
    set[field] = !set[field];
    set.updatedAt = now();
    await dbPut("studySets", set);
    play("select");
    render();
  }

  async function renameSet(id) {
    const set = getSet(id);
    const title = prompt("Tên mới", set?.title || "");
    if (!title) return;
    set.title = title.trim();
    set.updatedAt = now();
    await dbPut("studySets", set);
    render();
  }

  async function duplicateSet(id) {
    const set = getSet(id);
    if (!set) return;
    const newId = uid("set");
    const copy = { ...set, id: newId, title: `${set.title} (copy)`, isPinned: false, createdAt: now(), updatedAt: now() };
    const cards = getCardsForSet(id);
    await dbPut("studySets", copy);
    for (const [index, card] of cards.entries()) await dbPut("flashcards", createCard(newId, { ...card, id: uid("card"), studySetId: newId, position: index }));
    toast("Đã nhân bản bộ học.", "success");
    render();
  }

  async function deleteSet(id) {
    const set = getSet(id);
    if (!set || !confirm(`Xóa "${set.title}"?`)) return;
    await dbDelete("studySets", id);
    for (const card of getCardsForSet(id)) await dbDelete("flashcards", card.id);
    toast("Đã xóa bộ học.", "success");
    navigate("study-sets");
  }

  function renderStudyDetail() {
    const set = getSet(state.params.id);
    if (!set) return notFound("Không tìm thấy bộ học", "study-sets");
    const cards = getCardsForSet(set.id);
    state.cardSelection = new Set([...state.cardSelection].filter((id) => cards.some((card) => card.id === id)));
    const mastered = cards.filter((card) => card.masteryLevel >= 4).length;
    const need = cards.filter((card) => card.masteryLevel < 3).length;
    const mastery = cards.length ? Math.round(mastered / cards.length * 100) : 0;
    return `
      <section class="screen">
        ${screenHeader(set.title, `${cards.length} thẻ`, `<button class="icon-btn" onclick="KQuiz.showSetMenu('${set.id}')">${icons.more}</button>`, "study-sets")}
        <div class="progress" style="--value:${mastery}%"><span></span></div>
        <div class="study-card-head" style="margin-top:14px">
          <strong style="color:var(--success)">${mastered} thẻ đã thành thạo</strong>
          <strong style="color:var(--danger)">${need} thẻ cần ôn</strong>
        </div>
        <div class="section">
          <h2>Chọn chế độ học</h2>
          <div class="study-modes" style="margin-top:14px">
            <button class="study-mode" onclick="KQuiz.startFlashcard('${set.id}')"><span class="glyph">${icons.layers}</span><strong>Lật thẻ</strong><span class="small-text">Nhấn để lật thẻ</span></button>
            <button class="study-mode secondary" onclick="KQuiz.startLearn('${set.id}')"><span class="glyph">${icons.learn}</span><strong>Luyện hỏi</strong><span class="small-text">Luyện học</span></button>
            <button class="study-mode warning" onclick="KQuiz.navigate('test-config',{id:'${set.id}'})"><span class="glyph">${icons.test}</span><strong>Kiểm tra</strong><span class="small-text">Kiểm tra</span></button>
          </div>
        </div>
        <div class="section">
          <div class="card pad">
            <h2>Chia sẻ & xuất file</h2>
            <p class="small-text">${cards.length} thẻ • File, QR, PDF</p>
            <div class="btn-row three" style="margin-top:14px">
              <button class="btn secondary" onclick="KQuiz.exportStudySet('${set.id}','studyset')">${icons.download} File</button>
              <button class="btn secondary" onclick="KQuiz.showQr('${set.id}')">${icons.qr} QR</button>
              <button class="btn danger" onclick="KQuiz.showPdfExport('${set.id}')">${icons.pdf} PDF Pro</button>
            </div>
          </div>
        </div>
        <div class="section">
          <input id="cardSearch" class="input" placeholder="Tìm trong bộ học...">
        </div>
        <div class="section compact">
          <div class="bulk-toolbar card pad">
            <span><strong id="selectedCardCount">${state.cardSelection.size}</strong> thẻ đang chọn</span>
            <div class="actions-inline">
              <button class="btn secondary" onclick="KQuiz.selectAllCards('${set.id}')">Chọn tất cả</button>
              <button class="btn danger" onclick="KQuiz.bulkDeleteSelectedCards('${set.id}')">${icons.trash} Xóa chọn</button>
            </div>
          </div>
        </div>
        <div class="section">
          <div class="section-head">
            <h2>Xem trước thẻ (${cards.length})</h2>
            <button class="section-link" onclick="KQuiz.editCard('${set.id}')">+ Thêm thẻ mới</button>
          </div>
          <div id="cardList" class="list">${renderCardListHtml(cards)}</div>
        </div>
      </section>
    `;
  }

  function renderCardList(query = "") {
    const set = getSet(state.params.id);
    if (!set) return;
    const q = normalizeText(query).toLowerCase();
    const cards = getCardsForSet(set.id).filter((card) => !q || `${card.term} ${card.definition}`.toLowerCase().includes(q));
    $("#cardList").innerHTML = renderCardListHtml(cards);
    updateSelectedCardCount();
  }

  function renderCardListHtml(cards) {
    return cards.length ? cards.map((card) => `
      <article class="flash-row">
        <div class="study-card-head">
          <label class="card-check"><input type="checkbox" ${state.cardSelection.has(card.id) ? "checked" : ""} onchange="KQuiz.toggleCardSelection('${card.id}')"></label>
          <div>
            <h3>${escapeHtml(card.term)}</h3>
            <p>${escapeHtml(card.definition)}</p>
          </div>
          <div class="card-actions">
            <button class="mini-btn ${card.isStarred ? "primary" : ""}" onclick="KQuiz.toggleCardStar('${card.id}')">${icons.star}</button>
            <button class="mini-btn" onclick="KQuiz.editCard('${card.studySetId}','${card.id}')">${icons.edit}</button>
            <button class="mini-btn danger" onclick="KQuiz.deleteCard('${card.id}')">${icons.trash}</button>
          </div>
        </div>
      </article>`).join("") : `<div class="empty-state card"><div><strong>Chưa có thẻ</strong><span>Thêm thẻ mới để bắt đầu học.</span></div></div>`;
  }

  function toggleCardSelection(cardId) {
    if (state.cardSelection.has(cardId)) state.cardSelection.delete(cardId);
    else state.cardSelection.add(cardId);
    updateSelectedCardCount();
  }

  function updateSelectedCardCount() {
    const node = $("#selectedCardCount");
    if (node) node.textContent = String(state.cardSelection.size);
  }

  function selectAllCards(setId) {
    getCardsForSet(setId).forEach((card) => state.cardSelection.add(card.id));
    render();
  }

  async function bulkDeleteSelectedCards(setId) {
    const ids = [...state.cardSelection];
    if (!ids.length) return toast("Chưa chọn thẻ nào.", "warning");
    if (!confirm(`Xóa ${ids.length} thẻ đã chọn?`)) return;
    for (const id of ids) await dbDelete("flashcards", id);
    state.cardSelection.clear();
    await refreshSetCount(setId);
    toast("Đã xóa thẻ đã chọn.", "success");
    render();
  }

  function showSetMenu(id) {
    openModal(`
      <h2>Tùy chọn bộ học</h2>
      <div class="list">
        <button class="btn" onclick="KQuiz.closeModal();KQuiz.renameSet('${id}')">${icons.edit} Đổi tên</button>
        <button class="btn" onclick="KQuiz.closeModal();KQuiz.duplicateSet('${id}')">${icons.copy} Nhân bản</button>
        <button class="btn" onclick="KQuiz.closeModal();KQuiz.assignFolder('${id}')">${icons.folder} Chuyển thư mục</button>
        <button class="btn" onclick="KQuiz.closeModal();KQuiz.editTags('${id}')">${icons.tag} Gắn tag</button>
        <button class="btn danger" onclick="KQuiz.closeModal();KQuiz.deleteSet('${id}')">${icons.trash} Xóa</button>
      </div>
    `);
  }

  function editTags(id) {
    const set = getSet(id);
    if (!set) return;
    openModal(`
      <h2>Gắn tag</h2>
      <p>Nhập tag cách nhau bằng dấu phẩy để lọc bộ học nhanh hơn.</p>
      <input id="tagEditor" class="input" value="${escapeHtml((set.tags || []).join(", "))}" placeholder="english, exam, unit-1">
      <div class="btn-row" style="margin-top:16px">
        <button class="btn" onclick="KQuiz.closeModal()">Hủy</button>
        <button class="btn primary" onclick="KQuiz.saveSetTags('${id}')">Lưu tag</button>
      </div>
    `);
  }

  async function saveSetTags(id) {
    const set = await dbGet("studySets", id);
    if (!set) return;
    set.tags = ($("#tagEditor")?.value || "")
      .split(",")
      .map((tag) => normalizeText(tag).replace(/^#/, ""))
      .filter(Boolean);
    set.updatedAt = now();
    await dbPut("studySets", set);
    closeModal();
    toast("Đã lưu tag.", "success");
    render();
  }

  async function editCard(setId, cardId = "") {
    const card = cardId ? await dbGet("flashcards", cardId) : null;
    openModal(`
      <h2>${card ? "Sửa thẻ" : "Thêm thẻ mới"}</h2>
      <div class="form-grid">
        <input id="editTerm" class="input" placeholder="Mặt trước / câu hỏi" value="${escapeHtml(card?.term || "")}">
        <textarea id="editDefinition" class="textarea" placeholder="Mặt sau / đáp án">${escapeHtml(card?.definition || "")}</textarea>
        <input id="editChoices" class="input" placeholder="Lựa chọn A|B|C|D (nếu là trắc nghiệm)" value="${escapeHtml((card?.choices || []).join("|"))}">
        <input id="editCorrect" class="input" placeholder="Đáp án đúng: A, B, C, D hoặc nội dung" value="${escapeHtml(card?.definition || "")}">
      </div>
      <div class="btn-row" style="margin-top:18px">
        <button class="btn" onclick="KQuiz.closeModal()">Hủy</button>
        <button class="btn primary" onclick="KQuiz.saveCard('${setId}','${cardId}')">Lưu</button>
      </div>
    `);
  }

  async function saveCard(setId, cardId = "") {
    const term = $("#editTerm").value.trim();
    const definition = $("#editDefinition").value.trim();
    const choices = $("#editChoices").value.split("|").map((x) => x.trim()).filter(Boolean);
    if (!term || !definition) return toast("Cần nhập đủ mặt trước và mặt sau.", "warning");
    const existing = cardId ? await dbGet("flashcards", cardId) : null;
    const cards = getCardsForSet(setId);
    const card = createCard(setId, {
      ...(existing || {}),
      id: existing?.id || uid("card"),
      term,
      definition,
      itemType: choices.length ? "MULTIPLE_CHOICE" : (existing?.itemType || "TERM_DEFINITION"),
      choices: choices.map((choice, index) => /^[A-E]\./.test(choice) ? choice : `${String.fromCharCode(65 + index)}. ${choice}`),
      correctChoiceIndex: choices.length ? resolveCorrectIndex(choices, $("#editCorrect").value || definition) : -1,
      position: existing?.position ?? cards.length
    });
    await dbPut("flashcards", card);
    await refreshSetCount(setId);
    closeModal();
    toast("Đã lưu thẻ.", "success");
    render();
  }

  async function refreshSetCount(setId) {
    const set = await dbGet("studySets", setId);
    if (!set) return;
    const cards = await dbGetByIndex("flashcards", "studySetId", setId);
    set.cardCount = cards.length;
    set.updatedAt = now();
    await dbPut("studySets", set);
  }

  async function toggleCardStar(cardId) {
    const card = await dbGet("flashcards", cardId);
    card.isStarred = !card.isStarred;
    await dbPut("flashcards", card);
    render();
  }

  async function deleteCard(cardId) {
    const card = await dbGet("flashcards", cardId);
    if (!card || !confirm("Xóa thẻ này?")) return;
    await dbDelete("flashcards", cardId);
    await refreshSetCount(card.studySetId);
    render();
  }

  function renderImportFile() {
    return `
      <section class="screen">
        ${screenHeader("Nhập từ file", "PDF, TXT, Word, ảnh", "", "create-hub")}
        <div class="hero-card">
          <h2>Quăng file vào đây, app lo phần còn lại</h2>
          <p>Hỗ trợ PDF, TXT, Word và ảnh. App sẽ đọc nội dung rồi biến thành bộ câu hỏi.</p>
        </div>
        <div class="section">
          <h2>Chọn chế độ</h2>
          <div class="list" style="margin-top:14px">
            ${importModeCard("FREE", "Miễn phí", "Nhanh, offline, phù hợp file rõ.")}
            ${importModeCard("STANDARD", "Chuẩn", "OCR kỹ hơn, vẫn không cần cloud AI.")}
            ${importModeCard("AI_PRO", "AI Pro", "Dùng AI qua proxy endpoint, lỗi thì fallback local.")}
          </div>
        </div>
        <div class="section">
          <div class="btn-row">
            <button class="btn secondary" onclick="document.getElementById('fileInput').click()">${icons.upload} Chọn file nè</button>
            <button class="btn secondary" onclick="document.getElementById('cameraInput').click()">${icons.camera} Chụp tài liệu</button>
          </div>
          <input id="fileInput" class="hidden" type="file" accept=".txt,.pdf,.docx,.jpg,.jpeg,.png,.webp,text/plain,application/pdf,image/*">
          <input id="cameraInput" class="hidden" type="file" accept="image/*" capture="environment">
        </div>
        ${state.importFile.file ? `
          <div class="section compact">
            <div class="card pad">
              <h2>${escapeHtml(state.importFile.file.name)}</h2>
              <p class="small-text">${escapeHtml(state.importFile.file.type || "unknown")} • ${Math.round(state.importFile.file.size / 1024)} KB</p>
              <button class="btn primary" style="width:100%;margin-top:14px" onclick="KQuiz.processSelectedFile()">Tiếp tục tạo quiz</button>
            </div>
          </div>
        ` : ""}
      </section>
    `;
  }

  function importModeCard(mode, title, desc) {
    const active = state.importFile.mode === mode;
    return `<button class="option-card ${active ? "selected" : ""}" onclick="KQuiz.setImportMode('${mode}')"><span class="option-letter">${mode === "AI_PRO" ? "AI" : mode[0]}</span><span><strong>${title}</strong><br><span class="small-text">${desc}</span></span></button>`;
  }

  function setImportMode(mode) {
    state.importFile.mode = mode;
    render();
  }

  function handleImportFileInput(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    state.importFile.file = file;
    state.importFile.status = "";
    toast(`Đã chọn ${file.name}`, "success");
    render();
  }

  function hasActiveUnlock(feature, targetKey = "") {
    const key = `${feature}:${targetKey}`;
    return state.adRewards.some((reward) => reward.key === key && reward.expiresAt > now());
  }

  async function recordAdReward(feature, targetKey, result) {
    const key = `${feature}:${targetKey || ""}`;
    const reward = {
      id: uid("reward"),
      key,
      feature,
      targetKey: targetKey || "",
      result,
      grantedAt: now(),
      expiresAt: now() + 30 * 60 * 1000
    };
    await dbPut("adRewards", reward);
    await dbPut("unlockSessions", { id: key, feature, targetKey: targetKey || "", unlockedAt: reward.grantedAt, expiresAt: reward.expiresAt });
    await loadAll();
  }

  function proTargetKey(feature, payload = {}) {
    if (feature === "ai_pro" && state.importFile.file) return `${state.importFile.file.name}:${state.importFile.file.size}`;
    if (feature === "pdf_pro") return payload.setId || "";
    if (feature === "review_wrong") return state.test?.setId || "";
    return "";
  }

  async function requireProUnlock(feature, label, action, payload = {}) {
    const targetKey = proTargetKey(feature, payload);
    if (hasActiveUnlock(feature, targetKey)) {
      await action();
      return true;
    }
    const featureInfo = state.modules.shell?.PRO_FEATURES?.[feature] || { label, description: "" };
    state.pendingUnlock = { feature, label: featureInfo.label || label, targetKey, action };
    openModal(`
      <h2>${escapeHtml(featureInfo.label || label)}</h2>
      <p>${escapeHtml(featureInfo.description || "Tính năng Pro yêu cầu xem quảng cáo có tặng thưởng.")}</p>
      <div class="card pad" style="background:var(--info-soft);box-shadow:none">
        Pro chỉ mở khi Google Ad Manager gửi sự kiện reward. Nếu quảng cáo không sẵn sàng hoặc bạn đóng quảng cáo, tính năng sẽ không được mở.
      </div>
      <div class="btn-row" style="margin-top:16px">
        <button class="btn" onclick="KQuiz.closeModal()">Để sau</button>
        <button class="btn primary" onclick="KQuiz.confirmRewardedUnlock()">Xem quảng cáo</button>
      </div>
    `);
    return false;
  }

  async function confirmRewardedUnlock() {
    const pending = state.pendingUnlock;
    if (!pending) return;
    const adsConfig = state.settings.ads || defaultSettings.ads;
    try {
      const { requestRewardedAd } = await import("./modules/ads.js");
      toast("Đang gọi quảng cáo rewarded...", "warning");
      const result = await requestRewardedAd(adsConfig, { feature: pending.feature, targetKey: pending.targetKey });
      if (!result.granted) {
        toast(result.reason === "missing-config" ? "Chưa cấu hình Ad Manager rewarded." : "Chưa nhận reward, tính năng chưa mở.", "warning");
        return;
      }
      await recordAdReward(pending.feature, pending.targetKey, result);
      closeModal();
      const action = pending.action;
      state.pendingUnlock = null;
      toast("Đã mở khóa Pro cho phiên này.", "success");
      await action();
    } catch (error) {
      console.error(error);
      toast("Không tải được quảng cáo rewarded.", "error");
    }
  }

  async function processSelectedFile() {
    if (!state.importFile.file) return;
    if (state.importFile.mode === "AI_PRO" && !hasActiveUnlock("ai_pro", proTargetKey("ai_pro"))) {
      return requireProUnlock("ai_pro", "AI Pro", () => processSelectedFile());
    }
    state.processingNext = async () => {
      const file = state.importFile.file;
      const text = await extractTextFromFile(file, state.importFile.mode);
      state.importFile.text = text;
      const generated = await generateQuizFromText(text, state.quizConfig.count, state.quizConfig.difficulty, file.name);
      state.importFile.generated = generated;
      state.quiz = {
        title: file.name,
        questions: generated.questions,
        index: 0,
        answers: {},
        flags: {},
        startedAt: now(),
        source: "file"
      };
    };
    navigate("processing", { next: "quiz-config" });
  }

  function renderProcessing() {
    setTimeout(runProcessing, 220);
    return `
      <section class="screen no-bottom">
        ${screenHeader("Đọc tài liệu", "Processing", "", "import-file")}
        <div class="section">
          <div class="card pad" style="text-align:center">
            <div class="glyph" style="width:86px;height:86px;border-radius:28px;background:var(--primary-soft);color:var(--primary);display:grid;place-items:center;margin:0 auto 18px">${icons.spark}</div>
            <h2>Đang hiểu nội dung</h2>
            <p class="small-text">Đọc tài liệu, OCR nếu cần, lọc nội dung và tạo câu hỏi.</p>
            <div class="progress" style="--value:76%;margin-top:24px"><span></span></div>
          </div>
        </div>
      </section>
    `;
  }

  async function runProcessing() {
    if (!state.processingNext) return;
    try {
      const fn = state.processingNext;
      state.processingNext = null;
      await fn();
      toast("Đã tạo câu hỏi.", "success");
      navigate(state.params.next || "quiz-config");
    } catch (error) {
      console.error(error);
      toast(error.message || "Không xử lý được file.", "error");
      navigate("import-file");
    }
  }

  async function extractTextFromFile(file, mode = "FREE") {
    const name = file.name.toLowerCase();
    if (file.type.startsWith("text/") || name.endsWith(".txt")) return file.text();
    if (file.type === "application/pdf" || name.endsWith(".pdf")) return extractPdfText(file);
    if (name.endsWith(".docx")) return extractDocxText(file);
    if (file.type.startsWith("image/")) return extractImageText(file, mode);
    throw new Error("Định dạng file chưa được hỗ trợ.");
  }

  async function extractPdfText(file) {
    const pdfjs = await import(CDN.pdfjs);
    pdfjs.GlobalWorkerOptions.workerSrc = CDN.pdfWorker;
    const doc = await pdfjs.getDocument({ data: await file.arrayBuffer() }).promise;
    const pages = [];
    for (let i = 1; i <= doc.numPages; i += 1) {
      const page = await doc.getPage(i);
      const content = await page.getTextContent();
      pages.push(content.items.map((item) => item.str).join(" "));
    }
    const text = pages.join("\n\n").trim();
    if (text) return text;
    throw new Error("PDF không có text; hãy dùng ảnh/OCR hoặc Smart Scan.");
  }

  async function extractDocxText(file) {
    await loadScript(CDN.mammoth);
    const result = await window.mammoth.extractRawText({ arrayBuffer: await file.arrayBuffer() });
    return result.value.trim();
  }

  async function extractImageText(file, mode) {
    if (mode === "FREE") {
      return `Ảnh ${file.name}. OCR đầy đủ cần chế độ Chuẩn hoặc AI Pro. Hãy tạo câu hỏi thủ công từ nội dung ảnh nếu OCR chưa sẵn sàng.`;
    }
    await loadScript(CDN.tesseract);
    const result = await window.Tesseract.recognize(file, "vie+eng");
    return result.data.text.trim();
  }

  async function generateQuizFromText(text, count = 10, difficulty = "Trung bình", title = "Quiz") {
    if (state.importFile.mode === "AI_PRO" && state.settings.aiEndpoint) {
      try {
        const response = await fetch(`${state.settings.aiEndpoint.replace(/\/$/, "")}/generate-quiz`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ text, count, difficulty, title })
        });
        if (response.ok) {
          const data = await response.json();
          if (Array.isArray(data.questions) && data.questions.length) return { questions: data.questions, source: "AI_PRO" };
        }
      } catch (error) {
        console.warn("AI Pro fallback", error);
      }
    }
    return { questions: localGenerateQuestions(text, count), source: "LOCAL" };
  }

  function localGenerateQuestions(text, count = 10) {
    const sentences = normalizeText(text).split(/(?<=[.!?。！？])\s+|\n+/).map((x) => x.trim()).filter((x) => x.length > 24);
    const words = [...new Set(normalizeText(text).match(/[\p{L}\p{N}]{4,}/gu) || [])].slice(0, 120);
    const pool = sentences.length ? sentences : [normalizeText(text).slice(0, 180) || "KQuiz giúp tạo câu hỏi từ tài liệu."];
    return shuffle(pool).slice(0, Math.max(1, count)).map((sentence, index) => {
      const answer = pickKeyword(sentence, words) || `Ý chính ${index + 1}`;
      const distractors = shuffle(words.filter((word) => word.toLowerCase() !== answer.toLowerCase())).slice(0, 3);
      while (distractors.length < 3) distractors.push(`Phương án ${distractors.length + 1}`);
      const choices = shuffle([answer, ...distractors]).map((choice, idx) => `${String.fromCharCode(65 + idx)}. ${choice}`);
      return {
        id: uid("q"),
        question: `Theo tài liệu, ý nào phù hợp với câu: "${sentence.slice(0, 150)}${sentence.length > 150 ? "..." : ""}"?`,
        choices,
        correctChoiceIndex: choices.findIndex((choice) => normalizeChoice(choice) === normalizeChoice(answer)),
        explanation: sentence
      };
    });
  }

  function pickKeyword(sentence, words) {
    const candidates = sentence.match(/[\p{L}\p{N}]{4,}/gu) || [];
    return candidates.sort((a, b) => b.length - a.length).find((word) => words.includes(word)) || candidates[0];
  }

  function renderQuizConfig() {
    const title = state.quiz?.title || state.importFile.file?.name || "Quiz";
    return `
      <section class="screen">
        ${screenHeader("Cấu hình quiz", title, "", state.quiz?.source === "file" ? "import-file" : "study-sets")}
        <div class="form-grid">
          <label class="form-row"><span class="field-label">Số câu</span><input class="input" id="quizCount" type="number" min="1" value="${state.quizConfig.count}"></label>
          <div class="form-row">
            <span class="field-label">Độ khó</span>
            <div class="chip-row">${["Dễ","Trung bình","Khó"].map((d) => `<button class="chip ${state.quizConfig.difficulty === d ? "active" : ""}" onclick="KQuiz.setQuizDifficulty('${d}')">${d}</button>`).join("")}</div>
          </div>
          <button class="btn primary" onclick="KQuiz.startGeneratedQuiz()">Bắt đầu</button>
          <button class="btn secondary" onclick="KQuiz.saveGeneratedAsStudySet()">Lưu thành bộ học</button>
        </div>
      </section>
    `;
  }

  function setQuizDifficulty(value) {
    state.quizConfig.difficulty = value;
    render();
  }

  function startGeneratedQuiz() {
    if (!state.quiz?.questions?.length) return toast("Chưa có câu hỏi.", "warning");
    const count = Number($("#quizCount")?.value || state.quizConfig.count);
    state.quizConfig.count = count;
    state.quiz.questions = state.quiz.questions.slice(0, count);
    state.quiz.index = 0;
    state.quiz.answers = {};
    navigate("quiz");
  }

  async function saveGeneratedAsStudySet() {
    const questions = state.quiz?.questions || [];
    if (!questions.length) return toast("Chưa có câu hỏi để lưu.", "warning");
    const id = uid("set");
    const set = {
      id,
      title: state.quiz.title || "Bộ học từ file",
      description: "Tạo từ luồng nhập file trên KQuiz Web.",
      cardCount: questions.length,
      sourceType: "FILE_IMPORTED",
      sourceFileName: state.importFile.file?.name || "",
      studySetType: "MULTIPLE_CHOICE",
      isPinned: false,
      isFavorite: false,
      folderId: null,
      tags: [],
      createdAt: now(),
      updatedAt: now()
    };
    await dbPut("studySets", set);
    for (const [index, q] of questions.entries()) {
      await dbPut("flashcards", createCard(id, {
        term: q.question,
        definition: q.choices[q.correctChoiceIndex] || "",
        itemType: "MULTIPLE_CHOICE",
        choices: q.choices,
        correctChoiceIndex: q.correctChoiceIndex,
        explanation: q.explanation,
        position: index
      }));
    }
    toast("Đã lưu thành bộ học.", "success");
    navigate("study-detail", { id });
  }

  function renderQuiz() {
    const quiz = state.quiz;
    if (!quiz?.questions?.length) return notFound("Không có quiz đang chạy", "home");
    const q = quiz.questions[quiz.index];
    return renderQuestionScreen({
      title: "Làm quiz",
      subtitle: `${quiz.index + 1}/${quiz.questions.length}`,
      back: "quiz-config",
      questions: quiz.questions,
      index: quiz.index,
      answers: quiz.answers,
      onJump: "KQuiz.jumpQuiz",
      onSelect: "KQuiz.selectQuizAnswer",
      onPrev: "KQuiz.prevQuiz",
      onNext: "KQuiz.nextQuiz",
      onSubmit: "KQuiz.submitQuiz",
      question: q
    });
  }

  function renderQuestionScreen(cfg) {
    return `
      <section class="screen">
        ${screenHeader(cfg.title, cfg.subtitle, "", cfg.back)}
        <div class="question-strip">${cfg.questions.map((_, i) => `<button class="question-dot ${i === cfg.index ? "active" : ""} ${cfg.answers[i] != null ? "done" : ""}" onclick="${cfg.onJump}(${i})">${i + 1}</button>`).join("")}</div>
        <div class="section">
          <div class="card pad">
            <h2>${escapeHtml(cfg.question.question)}</h2>
            ${cfg.question.explanation ? `<p class="small-text">${escapeHtml(cfg.question.explanation).slice(0, 180)}</p>` : ""}
          </div>
        </div>
        <div class="section option-list">
          ${cfg.question.choices.map((choice, i) => `<button class="option-card ${cfg.answers[cfg.index] === i ? "selected" : ""}" onclick="${cfg.onSelect}(${i})"><span class="option-letter">${String.fromCharCode(65 + i)}</span><span>${escapeHtml(normalizeChoice(choice))}</span></button>`).join("")}
        </div>
        <div class="fixed-bottom">
          <div class="btn-row">
            <button class="btn" onclick="${cfg.onPrev}()">Trước</button>
            <button class="btn primary" onclick="${cfg.index === cfg.questions.length - 1 ? cfg.onSubmit : cfg.onNext}()">${cfg.index === cfg.questions.length - 1 ? "Nộp bài luôn" : "Tiếp theo"}</button>
          </div>
        </div>
      </section>
    `;
  }

  function selectQuizAnswer(idx) {
    state.quiz.answers[state.quiz.index] = idx;
    play("select");
    render();
  }

  function jumpQuiz(idx) { state.quiz.index = idx; render(); }
  function prevQuiz() { state.quiz.index = clamp(state.quiz.index - 1, 0, state.quiz.questions.length - 1); render(); }
  function nextQuiz() { state.quiz.index = clamp(state.quiz.index + 1, 0, state.quiz.questions.length - 1); render(); }

  async function submitQuiz() {
    const quiz = state.quiz;
    const correct = quiz.questions.filter((q, i) => quiz.answers[i] === q.correctChoiceIndex).length;
    quiz.result = { correct, total: quiz.questions.length, percent: Math.round(correct / quiz.questions.length * 100), submittedAt: now() };
    await dbPut("quizHistory", {
      id: uid("hist"),
      fileName: quiz.title || "Quiz",
      scorePercent: quiz.result.percent,
      correctCount: correct,
      totalQuestions: quiz.questions.length,
      difficulty: state.quizConfig.difficulty,
      questionType: "MULTIPLE_CHOICE",
      createdAt: now()
    });
    await addXp(10);
    await recordHighScore(quiz.result.percent);
    play("complete");
    navigate("result");
  }

  function renderResult() {
    const result = state.quiz?.result;
    if (!result) return notFound("Chưa có kết quả", "home");
    return `
      <section class="screen">
        ${screenHeader("Kết quả quiz", state.quiz.title || "", "", "create-hub")}
        <div class="section">
          <div class="score-ring" style="--score:${result.percent}%"><div class="score-ring-inner"><div><strong>${result.percent}%</strong><br><span class="small-text">${result.correct}/${result.total} đúng</span></div></div></div>
        </div>
        <div class="section list">
          <button class="btn primary" onclick="KQuiz.startReviewWrongFromQuiz()">Mở khóa Ôn Sai Siêu Tốc Pro</button>
          <button class="btn secondary" onclick="KQuiz.retryQuiz()">Làm lại phát nữa</button>
          <button class="btn" onclick="KQuiz.navigate('home')">Quay về Home</button>
        </div>
      </section>
    `;
  }

  function retryQuiz() {
    if (!state.quiz) return;
    state.quiz.index = 0;
    state.quiz.answers = {};
    state.quiz.result = null;
    navigate("quiz");
  }

  function startReviewWrongFromQuiz() {
    const wrong = state.quiz.questions.filter((q, i) => state.quiz.answers[i] !== q.correctChoiceIndex);
    if (!wrong.length) return toast("Không có câu sai để ôn.", "success");
    state.flash = { index: 0, flipped: false, mode: "wrong", cards: wrong.map((q) => ({ term: q.question, definition: q.choices[q.correctChoiceIndex], explanation: q.explanation })) };
    recordAchievement("wrong_review");
    navigate("review-wrong");
  }

  function renderHistory() {
    return `
      <section class="screen">
        ${screenHeader("Lịch sử", `${state.history.length} bài`, `<button class="icon-btn" onclick="KQuiz.clearHistory()">${icons.trash}</button>`, "review-hub")}
        <div class="list">
          ${state.history.length ? state.history.map((item) => `
            <article class="history-card">
              <div class="study-card-head">
                <div><h3>${escapeHtml(item.fileName)}</h3><p>${formatDate(item.createdAt)} • ${escapeHtml(item.difficulty)}</p></div>
                <strong>${item.scorePercent}%</strong>
              </div>
              <p>${item.correctCount}/${item.totalQuestions} câu đúng</p>
            </article>`).join("") : `<div class="empty-state card"><div><strong>Chưa có lịch sử</strong><span>Làm quiz xong kết quả sẽ nằm ở đây.</span></div></div>`}
        </div>
      </section>
    `;
  }

  async function clearHistory() {
    if (!confirm("Xóa toàn bộ lịch sử?")) return;
    await dbClear("quizHistory");
    render();
  }

  function startFlashcard(setId) {
    const cards = getCardsForSet(setId);
    if (!cards.length) return toast("Bộ học chưa có thẻ.", "warning");
    state.flash = { index: 0, flipped: false, mode: "set", setId, cards };
    navigate("flashcard", { id: setId });
  }

  function renderFlashcard() {
    const cards = state.flash.cards?.length ? state.flash.cards : getCardsForSet(state.params.id);
    if (!cards.length) return notFound("Chưa có thẻ", "study-detail?id=" + state.params.id);
    const card = cards[state.flash.index] || cards[0];
    return `
      <section class="screen">
        ${screenHeader("Lật thẻ", `${state.flash.index + 1}/${cards.length}`, `<button class="icon-btn" onclick="KQuiz.speakCurrentFlash()">${icons.volume}</button>`, `study-detail?id=${state.params.id || state.flash.setId}`)}
        <div class="flashcard-stage">
          <div class="flashcard ${state.flash.flipped ? "flipped" : ""}" onclick="KQuiz.flipFlashcard()">
            <div class="flash-side"><div><strong>${escapeHtml(card.term)}</strong><span>Nhấn để lật thẻ</span></div></div>
            <div class="flash-side back"><div><strong>${escapeHtml(card.definition)}</strong><span>${escapeHtml(card.explanation || "Mặt sau")}</span></div></div>
          </div>
        </div>
        <div class="fixed-bottom">
          <div class="btn-row">
            <button class="btn" onclick="KQuiz.prevFlashcard()">Trước</button>
            <button class="btn primary" onclick="KQuiz.nextFlashcard()">Đã nhớ</button>
          </div>
        </div>
      </section>
    `;
  }

  function flipFlashcard() {
    state.flash.flipped = !state.flash.flipped;
    play("flip");
    render();
  }

  function prevFlashcard() {
    state.flash.index = clamp(state.flash.index - 1, 0, state.flash.cards.length - 1);
    state.flash.flipped = false;
    render();
  }

  async function nextFlashcard() {
    const card = state.flash.cards[state.flash.index];
    if (card?.id) await reviewCard(card.id, true, 2);
    await addDailyProgress(1);
    await addXp(2);
    if (state.flash.index >= state.flash.cards.length - 1) {
      toast("Hoàn thành phiên lật thẻ.", "success");
      play("complete");
      navigate("study-detail", { id: state.flash.setId || state.params.id });
      return;
    }
    state.flash.index += 1;
    state.flash.flipped = false;
    render();
  }

  function speakCurrentFlash() {
    const card = state.flash.cards[state.flash.index];
    speak(`${card.term}. ${state.flash.flipped ? card.definition : ""}`);
  }

  function startLearn(setId) {
    const cards = getCardsForSet(setId);
    if (!cards.length) return toast("Bộ học chưa có thẻ.", "warning");
    state.learn = {
      setId,
      cards: shuffle(cards),
      index: 0,
      correct: 0,
      answers: []
    };
    navigate("learn", { id: setId });
  }

  function renderLearn() {
    const learn = state.learn;
    if (!learn?.cards?.length) return notFound("Chưa có phiên học", "study-sets");
    const card = learn.cards[learn.index];
    const q = cardToQuestion(card);
    return renderQuestionScreen({
      title: "Luyện hỏi",
      subtitle: `${learn.index + 1}/${learn.cards.length}`,
      back: `study-detail?id=${learn.setId}`,
      questions: learn.cards,
      index: learn.index,
      answers: Object.fromEntries(learn.answers.map((a, i) => [i, a.selected])),
      onJump: "KQuiz.jumpLearn",
      onSelect: "KQuiz.answerLearn",
      onPrev: "KQuiz.prevLearn",
      onNext: "KQuiz.nextLearn",
      onSubmit: "KQuiz.finishLearn",
      question: q
    });
  }

  function cardToQuestion(card) {
    if (card.choices?.length) {
      return { question: card.term, choices: card.choices, correctChoiceIndex: card.correctChoiceIndex >= 0 ? card.correctChoiceIndex : resolveCorrectIndex(card.choices, card.definition), explanation: card.explanation };
    }
    const others = shuffle(state.cards.filter((x) => x.id !== card.id).map((x) => x.definition)).slice(0, 3);
    while (others.length < 3) others.push(`Phương án ${others.length + 1}`);
    const choices = shuffle([card.definition, ...others]).map((choice, i) => `${String.fromCharCode(65 + i)}. ${choice}`);
    return { question: card.term, choices, correctChoiceIndex: choices.findIndex((choice) => normalizeChoice(choice) === normalizeChoice(card.definition)), explanation: "" };
  }

  async function answerLearn(idx) {
    const learn = state.learn;
    const card = learn.cards[learn.index];
    const q = cardToQuestion(card);
    const correct = idx === q.correctChoiceIndex;
    learn.answers[learn.index] = { selected: idx, correct };
    if (correct) learn.correct += 1;
    await reviewCard(card.id, correct, correct ? 1 : -1);
    await addDailyProgress(1);
    if (correct) await addXp(5);
    play(correct ? "correct" : "wrong");
    render();
  }

  function jumpLearn(idx) { state.learn.index = idx; render(); }
  function prevLearn() { state.learn.index = clamp(state.learn.index - 1, 0, state.learn.cards.length - 1); render(); }
  function nextLearn() {
    if (state.learn.index >= state.learn.cards.length - 1) finishLearn();
    else { state.learn.index += 1; render(); }
  }
  function finishLearn() { navigate("learn-result"); }

  function renderLearnResult() {
    const learn = state.learn;
    if (!learn) return notFound("Chưa có kết quả học", "study-sets");
    const total = learn.cards.length;
    const percent = Math.round((learn.correct || 0) / total * 100);
    return `
      <section class="screen">
        ${screenHeader("Tổng kết luyện hỏi", `${learn.correct}/${total} đúng`, "", `study-detail?id=${learn.setId}`)}
        <div class="section"><div class="score-ring" style="--score:${percent}%"><div class="score-ring-inner"><div><strong>${percent}%</strong><br><span class="small-text">hoàn thành</span></div></div></div></div>
        <div class="section list">
          <button class="btn primary" onclick="KQuiz.startLearn('${learn.setId}')">Luyện lại</button>
          <button class="btn" onclick="KQuiz.navigate('study-detail',{id:'${learn.setId}'})">Quay về bộ học</button>
        </div>
      </section>
    `;
  }

  function renderTestConfig() {
    const setId = state.params.id;
    const mixed = state.params.mixed ? state.params.mixed.split(",") : [];
    const cards = mixed.length ? mixed.flatMap(getCardsForSet) : getCardsForSet(setId);
    return `
      <section class="screen">
        ${screenHeader("Cấu hình bài kiểm tra", `${cards.length} thẻ hợp lệ`, "", setId === "mixed" ? "study-sets" : `study-detail?id=${setId}`)}
        <div class="form-grid">
          <label class="form-row"><span class="field-label">Nguồn câu hỏi</span>${select("testSource", state.quizConfig.source, [["all","Tất cả"],["starred","Đã ghim"],["review","Cần ôn"],["weak","Chưa thành thạo"]])}</label>
          <label class="form-row"><span class="field-label">Số câu hỏi</span><input id="testCount" class="input" type="number" min="1" max="${cards.length}" value="${Math.min(state.quizConfig.count, cards.length || 1)}"></label>
          <label class="form-row"><span class="field-label">Loại câu hỏi</span>${select("testDirection", state.quizConfig.direction, [["front_to_back","Hỏi -> Đáp"],["back_to_front","Đáp -> Hỏi"],["mixed","Hỗn hợp"]])}</label>
          <label class="form-row"><span class="field-label">Thời gian (phút)</span><input id="testTimerMinutes" class="input" type="number" min="0" max="180" value="${state.quizConfig.timerMinutes || 0}" placeholder="0 = không giới hạn"></label>
          <label class="option-card"><input id="testShuffle" type="checkbox" ${state.quizConfig.shuffle ? "checked" : ""}> Xáo trộn câu hỏi</label>
          <label class="option-card"><input id="testAuto" type="checkbox" ${state.quizConfig.autoSubmit ? "checked" : ""}> Tự động nộp khi hết giờ</label>
          <button class="btn primary" onclick="KQuiz.startTestSession('${setId}','${mixed.join(",")}')">Bắt đầu kiểm tra</button>
        </div>
      </section>
    `;
  }

  function startTestSession(setId, mixedIds = "") {
    const mixed = mixedIds ? mixedIds.split(",").filter(Boolean) : [];
    let cards = mixed.length ? mixed.flatMap(getCardsForSet) : getCardsForSet(setId);
    const source = $("[name='testSource']")?.value || "all";
    if (source === "starred") cards = cards.filter((card) => card.isStarred);
    if (source === "review") cards = cards.filter((card) => card.masteryLevel < 3);
    if (source === "weak") cards = cards.filter((card) => card.masteryLevel < 4);
    if (!cards.length) return toast("Không có thẻ phù hợp.", "warning");
    const count = clamp(Number($("#testCount").value || 10), 1, cards.length);
    state.quizConfig.source = source;
    state.quizConfig.direction = $("[name='testDirection']").value;
    state.quizConfig.shuffle = $("#testShuffle").checked;
    state.quizConfig.timerMinutes = clamp(Number($("#testTimerMinutes").value || 0), 0, 180);
    state.quizConfig.autoSubmit = $("#testAuto").checked;
    state.test = {
      setId,
      mixed,
      cards: (state.quizConfig.shuffle ? shuffle(cards) : cards).slice(0, count),
      questions: [],
      index: 0,
      answers: {},
      startedAt: now(),
      timerMinutes: state.quizConfig.timerMinutes,
      autoSubmit: state.quizConfig.autoSubmit
    };
    state.test.questions = state.test.cards.map((card) => cardToQuestion(card));
    navigate("test", { id: setId, mixed: mixed.join(",") });
  }

  function renderTest() {
    const test = state.test;
    if (!test?.questions?.length) return notFound("Chưa có bài kiểm tra", "study-sets");
    ensureTestTimer();
    return renderQuestionScreen({
      title: "Màn làm bài",
      subtitle: `${test.index + 1}/${test.questions.length}${test.timerMinutes ? " • " + formatRemainingTime(getTestRemainingMs()) : ""}`,
      back: `test-config?id=${test.setId}`,
      questions: test.questions,
      index: test.index,
      answers: test.answers,
      onJump: "KQuiz.jumpTest",
      onSelect: "KQuiz.selectTestAnswer",
      onPrev: "KQuiz.prevTest",
      onNext: "KQuiz.nextTest",
      onSubmit: "KQuiz.submitTest",
      question: test.questions[test.index]
    });
  }

  function getTestRemainingMs() {
    const test = state.test;
    if (!test?.timerMinutes) return 0;
    return Math.max(0, test.startedAt + test.timerMinutes * 60 * 1000 - now());
  }

  function formatRemainingTime(ms) {
    const total = Math.ceil(ms / 1000);
    const minutes = Math.floor(total / 60);
    const seconds = total % 60;
    return `${minutes}:${String(seconds).padStart(2, "0")}`;
  }

  function ensureTestTimer() {
    const test = state.test;
    if (!test?.timerMinutes || state.testTimer) return;
    state.testTimer = setInterval(() => {
      const remaining = getTestRemainingMs();
      const subtitle = $(".screen-title small");
      if (subtitle) subtitle.textContent = `${test.index + 1}/${test.questions.length} • ${formatRemainingTime(remaining)}`;
      if (remaining <= 0) {
        clearInterval(state.testTimer);
        state.testTimer = null;
        if (test.autoSubmit) submitTest();
        else toast("Đã hết giờ. Bạn vẫn có thể nộp thủ công.", "warning");
      }
    }, 1000);
  }

  function selectTestAnswer(idx) { state.test.answers[state.test.index] = idx; play("select"); render(); }
  function jumpTest(idx) { state.test.index = idx; render(); }
  function prevTest() { state.test.index = clamp(state.test.index - 1, 0, state.test.questions.length - 1); render(); }
  function nextTest() { state.test.index = clamp(state.test.index + 1, 0, state.test.questions.length - 1); render(); }

  async function submitTest() {
    const test = state.test;
    let correct = 0;
    for (const [i, q] of test.questions.entries()) {
      const ok = test.answers[i] === q.correctChoiceIndex;
      if (ok) correct += 1;
      await reviewCard(test.cards[i].id, ok, ok ? 1 : -1);
    }
    const percent = Math.round(correct / test.questions.length * 100);
    test.result = { correct, total: test.questions.length, percent };
    await dbPut("quizHistory", {
      id: uid("hist"),
      fileName: test.mixed.length ? "Bài test trộn nhiều bộ" : getSet(test.setId)?.title || "Bài test",
      scorePercent: percent,
      correctCount: correct,
      totalQuestions: test.questions.length,
      difficulty: "Bộ học",
      questionType: "MULTIPLE_CHOICE",
      createdAt: now()
    });
    await addXp(10);
    await recordHighScore(percent);
    play("complete");
    navigate("test-result", { id: test.setId });
  }

  function renderTestResult() {
    const result = state.test?.result;
    if (!result) return notFound("Chưa có kết quả", "study-sets");
    return `
      <section class="screen">
        ${screenHeader("Kết quả kiểm tra", `${result.correct}/${result.total} đúng`, "", state.test.setId === "mixed" ? "study-sets" : `study-detail?id=${state.test.setId}`)}
        <div class="section"><div class="score-ring" style="--score:${result.percent}%"><div class="score-ring-inner"><div><strong>${result.percent}%</strong><br><span class="small-text">điểm</span></div></div></div></div>
        <div class="section list">
          <button class="btn primary" onclick="KQuiz.startReviewWrong()">Mở khóa Ôn Sai Siêu Tốc Pro</button>
          <button class="btn secondary" onclick="KQuiz.retakeTest()">Làm lại phát nữa</button>
          <button class="btn" onclick="KQuiz.navigate('${state.test.setId === "mixed" ? "study-sets" : "study-detail"}',{id:'${state.test.setId}'})">Quay về bộ học</button>
        </div>
      </section>
    `;
  }

  function retakeTest() {
    state.test.index = 0;
    state.test.answers = {};
    state.test.result = null;
    navigate("test", { id: state.test.setId });
  }

  function startReviewWrong() {
    return requireProUnlock("review_wrong", "Ôn sai siêu tốc Pro", () => startReviewWrongUnlocked());
  }

  function startReviewWrongUnlocked() {
    const wrong = state.test.questions
      .map((q, i) => ({ q, i }))
      .filter(({ q, i }) => state.test.answers[i] !== q.correctChoiceIndex)
      .map(({ q }) => ({ term: q.question, definition: q.choices[q.correctChoiceIndex], explanation: q.explanation }));
    if (!wrong.length) return toast("Không có câu sai để ôn.", "success");
    state.flash = { index: 0, flipped: false, mode: "wrong", cards: wrong };
    recordAchievement("wrong_review");
    navigate("review-wrong");
  }

  function renderReviewWrong() {
    if (!state.flash.cards?.length) return notFound("Không có câu sai", "study-sets");
    return renderFlashcard().replace("Lật thẻ", "Ôn sai siêu tốc").replace("Quay về bộ học", "Quay về");
  }

  async function reviewCard(cardId, correct, delta) {
    const card = await dbGet("flashcards", cardId);
    if (!card) return;
    card.timesReviewed = (card.timesReviewed || 0) + 1;
    if (correct) card.timesCorrect = (card.timesCorrect || 0) + 1;
    card.masteryLevel = clamp((card.masteryLevel || 0) + delta, 0, 5);
    card.lastReviewedAt = now();
    await dbPut("flashcards", card);
    const date = todayKey();
    const stat = (await dbGet("studyStats", date)) || { date, cardsReviewed: 0, correct: 0 };
    stat.cardsReviewed = (stat.cardsReviewed || 0) + 1;
    if (correct) stat.correct = (stat.correct || 0) + 1;
    await dbPut("studyStats", stat);
  }

  async function addDailyProgress(count) {
    const key = todayKey();
    const stats = (await dbGet("studyStats", key)) || { date: key, cardsReviewed: 0, studySetsCount: 0, streakCount: 1 };
    stats.cardsReviewed += count;
    await dbPut("studyStats", stats);
    if (stats.cardsReviewed >= state.settings.dailyGoal) toast("Hoàn thành mục tiêu hằng ngày!", "success");
    await syncAchievements();
  }

  async function addXp(amount) {
    await saveSettings({ totalXp: (state.settings.totalXp || 0) + amount });
  }

  function getUnlockedBadges() {
    const defs = badgeDefinitions();
    return defs.filter((badge) => state.achievements[badge.id]);
  }

  function badgeDefinitions() {
    return [
      { id: "first_session", title: "Ngày đầu học", subtitle: "Hoàn thành phiên học đầu tiên" },
      { id: "streak_3", title: "3 ngày liên tục", subtitle: "Giữ nhiệt học 3 ngày" },
      { id: "streak_7", title: "7 ngày liên tục", subtitle: "Bền bỉ suốt 1 tuần" },
      { id: "review_100", title: "100 thẻ đã ôn", subtitle: "Chạm mốc 100 thẻ" },
      { id: "high_score", title: "Điểm 90%+", subtitle: "Đạt điểm test từ 90%" },
      { id: "create_5_sets", title: "Tạo 5 bộ học", subtitle: "Có 5 bộ học riêng" },
      { id: "wrong_review", title: "Ôn sai chăm chỉ", subtitle: "Đã ôn lại câu sai" }
    ];
  }

  async function recordAchievement(id) {
    await dbPut("achievements", { id, unlockedAt: now() });
    await loadAll();
  }

  async function recordHighScore(percent) {
    if (percent >= 90) await recordAchievement("high_score");
  }

  async function syncAchievements() {
    const reviewed = state.cards.reduce((sum, card) => sum + (card.timesReviewed || 0), 0);
    if (reviewed > 0) await recordAchievement("first_session");
    if (reviewed >= 100) await recordAchievement("review_100");
    if (state.studySets.length >= 5) await recordAchievement("create_5_sets");
  }

  function renderFolders() {
    return `
      <section class="screen">
        ${screenHeader("Tổ chức bộ học", `${state.folders.length} thư mục`, `<button class="icon-btn" onclick="KQuiz.showCreateFolder()">${icons.plus}</button>`, "study-hub")}
        <div class="list">
          <article class="folder-card" onclick="KQuiz.navigate('folder-detail',{id:'root'})"><h3>Chưa phân loại</h3><p>${state.studySets.filter((s) => !s.folderId).length} bộ học</p></article>
          ${state.folders.map((folder) => `<article class="folder-card" onclick="KQuiz.navigate('folder-detail',{id:'${folder.id}'})"><div class="study-card-head"><div><h3>${escapeHtml(folder.name)}</h3><p>${state.studySets.filter((s) => s.folderId === folder.id).length} bộ học</p></div><button class="mini-btn danger" onclick="event.stopPropagation();KQuiz.deleteFolder('${folder.id}')">${icons.trash}</button></div></article>`).join("")}
        </div>
      </section>
    `;
  }

  function showCreateFolder() {
    openModal(`
      <h2>Tạo thư mục</h2>
      <input id="folderName" class="input" placeholder="Tên thư mục">
      <div class="btn-row" style="margin-top:16px"><button class="btn" onclick="KQuiz.closeModal()">Hủy</button><button class="btn primary" onclick="KQuiz.createFolder()">Tạo</button></div>
    `);
  }

  async function createFolder() {
    const name = $("#folderName").value.trim();
    if (!name) return;
    await dbPut("folders", { id: uid("folder"), name, color: "#5b6cff", createdAt: now() });
    closeModal();
    render();
  }

  async function deleteFolder(id) {
    if (!confirm("Xóa thư mục? Bộ học sẽ chuyển về chưa phân loại.")) return;
    for (const set of state.studySets.filter((s) => s.folderId === id)) await dbPut("studySets", { ...set, folderId: null, updatedAt: now() });
    await dbDelete("folders", id);
    render();
  }

  function renderFolderDetail() {
    const id = state.params.id;
    const folder = id === "root" ? { name: "Chưa phân loại" } : state.folders.find((x) => x.id === id);
    const sets = state.studySets.filter((set) => id === "root" ? !set.folderId : set.folderId === id);
    return `
      <section class="screen">
        ${screenHeader(folder?.name || "Thư mục", `${sets.length} bộ học`, "", "folders")}
        <div class="list">${sets.length ? sets.map(renderStudySetCard).join("") : `<div class="empty-state card"><div><strong>Thư mục trống</strong><span>Chuyển bộ học vào đây từ menu bộ học.</span></div></div>`}</div>
      </section>
    `;
  }

  function assignFolder(setId) {
    openModal(`
      <h2>Chuyển thư mục</h2>
      <div class="list">
        <button class="btn" onclick="KQuiz.setFolder('${setId}','')">Chưa phân loại</button>
        ${state.folders.map((folder) => `<button class="btn" onclick="KQuiz.setFolder('${setId}','${folder.id}')">${icons.folder} ${escapeHtml(folder.name)}</button>`).join("")}
      </div>
    `);
  }

  async function setFolder(setId, folderId) {
    const set = await dbGet("studySets", setId);
    set.folderId = folderId || null;
    set.updatedAt = now();
    await dbPut("studySets", set);
    closeModal();
    render();
  }

  function renderSmartReview() {
    const cards = state.cards.filter((card) => card.masteryLevel < 4).sort((a, b) => a.masteryLevel - b.masteryLevel).slice(0, 30);
    if (cards.length && (!state.flash.cards.length || state.flash.mode !== "smart")) {
      state.flash = { index: 0, flipped: false, mode: "smart", cards };
    }
    return `
      <section class="screen">
        ${screenHeader("Smart Review", `${cards.length} thẻ ưu tiên`, "", "review-hub")}
        ${cards.length ? `
          <div class="flashcard-stage">
            <div class="flashcard ${state.flash.flipped ? "flipped" : ""}" onclick="KQuiz.flipFlashcard()">
              <div class="flash-side"><div><strong>${escapeHtml(state.flash.cards[state.flash.index]?.term || "")}</strong><span>Thẻ yếu cần ôn</span></div></div>
              <div class="flash-side back"><div><strong>${escapeHtml(state.flash.cards[state.flash.index]?.definition || "")}</strong><span>Đánh giá mức nhớ</span></div></div>
            </div>
          </div>
          <div class="fixed-bottom"><div class="btn-row"><button class="btn danger" onclick="KQuiz.rateSmart(false)">Chưa nhớ</button><button class="btn primary" onclick="KQuiz.rateSmart(true)">Đã nhớ</button></div></div>
        ` : `<div class="empty-state card"><div><strong>Không có thẻ cần ôn</strong><span>Bạn đang giữ nhịp rất tốt.</span></div></div>`}
      </section>
    `;
  }

  async function rateSmart(remembered) {
    const card = state.flash.cards[state.flash.index];
    await reviewCard(card.id, remembered, remembered ? 1 : -1);
    await addXp(3);
    await addDailyProgress(1);
    if (state.flash.index >= state.flash.cards.length - 1) {
      toast("Hoàn thành Smart Review.", "success");
      navigate("home");
    } else {
      state.flash.index += 1;
      state.flash.flipped = false;
      render();
    }
  }

  function renderImportStudySet() {
    const preview = state.importPreview;
    return `
      <section class="screen">
        ${screenHeader("Nhập bộ học", ".studyset, .json hoặc QR", "", "create-hub")}
        <div class="action-grid">
          ${actionCard("Chọn file", ".studyset / .json", icons.upload, "document.getElementById('importStudyInput').click()","wide")}
          ${actionCard("Quét QR", "Camera", icons.qr, "KQuiz.startQrScanner()","wide secondary")}
        </div>
        <input id="importStudyInput" class="hidden" type="file" accept=".studyset,.json,application/json">
        ${preview ? `
          <div class="section card pad">
            <h2>Preview import</h2>
            <p><strong>${escapeHtml(preview.title)}</strong></p>
            <p class="small-text">${preview.cardCount} thẻ • ${escapeHtml(preview.description || "Không có mô tả")}</p>
            <div class="btn-row" style="margin-top:14px">
              <button class="btn" onclick="KQuiz.clearImportPreview()">Hủy</button>
              <button class="btn primary" onclick="KQuiz.confirmImportStudySet()">Nhập vào app</button>
            </div>
          </div>
        ` : ""}
        <div id="qrScanner" class="section hidden">
          <div class="canvas-box"><video id="qrVideo" autoplay playsinline></video><canvas id="qrCanvas" class="hidden"></canvas></div>
          <button class="btn danger" style="width:100%;margin-top:12px" onclick="KQuiz.stopQrScanner()">Dừng quét</button>
        </div>
      </section>
    `;
  }

  async function handleStudySetFileInput(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    const text = await file.text();
    await importStudySetPayload(text);
  }

  async function importStudySetPayload(text) {
    try {
      const payload = parseQrPayload(text);
      const data = JSON.parse(payload);
      const setInfo = data.studySet || data;
      const cardsInfo = data.flashcards || data.cards || [];
      if (!setInfo.title || !cardsInfo.length) throw new Error("Invalid study set");
      state.importPreview = { data, title: setInfo.title, description: setInfo.description || "", cardCount: cardsInfo.length };
      toast("Đã đọc file/QR. Kiểm tra preview rồi nhập.", "success");
      render();
    } catch (error) {
      console.error(error);
      toast("File/QR không hợp lệ.", "error");
    }
  }

  function clearImportPreview() {
    state.importPreview = null;
    render();
  }

  async function confirmImportStudySet() {
    if (!state.importPreview?.data) return;
    const imported = await saveImportedStudySet(state.importPreview.data);
    state.importPreview = null;
    toast("Import bộ học thành công.", "success");
    navigate("study-detail", { id: imported.id });
  }

  function parseQrPayload(text) {
    const trimmed = String(text || "").trim();
    if (trimmed.startsWith("KQUIZ_STUDYSET_V1:")) return decodeURIComponent(escape(atob(trimmed.slice("KQUIZ_STUDYSET_V1:".length))));
    return trimmed;
  }

  async function saveImportedStudySet(data) {
    const setInfo = data.studySet || data;
    const cardsInfo = data.flashcards || data.cards || [];
    if (!setInfo.title || !cardsInfo.length) throw new Error("Invalid study set");
    const id = uid("set");
    const set = {
      id,
      title: `${setInfo.title}${state.studySets.some((s) => s.title === setInfo.title) ? " (import)" : ""}`,
      description: setInfo.description || "",
      cardCount: cardsInfo.length,
      sourceType: "IMPORTED",
      sourceFileName: setInfo.sourceFileName || "",
      studySetType: setInfo.studySetType || "TERM_DEFINITION",
      isPinned: false,
      isFavorite: false,
      folderId: null,
      tags: setInfo.tags || [],
      createdAt: now(),
      updatedAt: now()
    };
    await dbPut("studySets", set);
    for (const [index, item] of cardsInfo.entries()) {
      await dbPut("flashcards", createCard(id, {
        term: item.term || item.question || "",
        definition: item.definition || item.answer || "",
        choices: normalizeChoices(item.choices),
        correctChoiceIndex: item.correctChoiceIndex ?? -1,
        explanation: item.explanation || "",
        isStarred: item.isStarred || false,
        masteryLevel: 0,
        position: index
      }));
    }
    return set;
  }

  function normalizeChoices(choices) {
    if (Array.isArray(choices)) return choices;
    if (typeof choices === "string" && choices.trim().startsWith("[")) {
      try { return JSON.parse(choices); } catch { return []; }
    }
    return [];
  }

  async function exportStudySet(setId, format = "studyset") {
    const set = getSet(setId);
    const cards = getCardsForSet(setId);
    const data = buildStudySetExport(set, cards);
    const content = JSON.stringify(data, null, 2);
    downloadBlob(new Blob([content], { type: "application/json" }), `${safeFileName(set.title)}.${format === "json" ? "json" : "studyset"}`);
    if (navigator.share && navigator.canShare) {
      try {
        const file = new File([content], `${safeFileName(set.title)}.studyset`, { type: "application/json" });
        await navigator.share({ files: [file], title: set.title });
      } catch {}
    }
  }

  function buildStudySetExport(set, cards) {
    return {
      formatVersion: 1,
      appVersion: APP_VERSION,
      appName: "KQuiz",
      exportedAt: now(),
      studySet: {
        title: set.title,
        description: set.description || "",
        sourceType: set.sourceType || "IMPORTED",
        sourceFileName: set.sourceFileName || "",
        studySetType: set.studySetType || "TERM_DEFINITION",
        createdAt: set.createdAt || now(),
        tags: set.tags || []
      },
      flashcards: cards.map((card) => ({
        term: card.term,
        definition: card.definition,
        explanation: card.explanation || "",
        choices: Array.isArray(card.choices) ? JSON.stringify(card.choices) : card.choices || "",
        correctChoiceIndex: card.correctChoiceIndex ?? -1,
        sourceSnippet: card.sourceSnippet || "",
        sourcePageStart: card.sourcePageStart ?? null,
        sourcePageEnd: card.sourcePageEnd ?? null,
        isStarred: Boolean(card.isStarred),
        masteryLevel: card.masteryLevel || 0,
        createdAt: card.createdAt || now()
      }))
    };
  }

  function showQr(setId) {
    const set = getSet(setId);
    const payload = `KQUIZ_STUDYSET_V1:${btoa(unescape(encodeURIComponent(JSON.stringify(buildStudySetExport(set, getCardsForSet(setId))))))}`;
    openModal(`
      <h2>QR bộ học</h2>
      <div id="qrOut" class="qr-box"><span>Đang tạo QR...</span></div>
      <textarea class="textarea" readonly style="min-height:80px;margin-top:12px">${escapeHtml(payload)}</textarea>
      <button class="btn primary" style="width:100%;margin-top:12px" onclick="KQuiz.copyText(\`${payload.replaceAll("`", "\\`")}\`)">Copy payload</button>
    `);
    setTimeout(async () => {
      await loadScript(CDN.qrcode);
      const box = $("#qrOut");
      box.innerHTML = "";
      try {
        new window.QRCode(box, {
          text: payload,
          width: 250,
          height: 250,
          colorDark: "#111827",
          colorLight: "#ffffff",
          correctLevel: window.QRCode.CorrectLevel.L
        });
      } catch (error) {
        box.textContent = "Payload quá lớn để tạo QR. Hãy dùng Copy payload.";
      }
    }, 20);
  }

  async function copyText(text) {
    await navigator.clipboard?.writeText(text);
    toast("Đã copy.", "success");
  }

  async function startQrScanner() {
    $("#qrScanner")?.classList.remove("hidden");
    const video = $("#qrVideo");
    const canvas = $("#qrCanvas");
    const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } });
    state.qr.stream = stream;
    state.qr.scanning = true;
    video.srcObject = stream;
    const ctx = canvas.getContext("2d");
    await loadScript(CDN.jsqr);
    const tick = async () => {
      if (!state.qr.scanning) return;
      if (video.readyState === video.HAVE_ENOUGH_DATA) {
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const code = window.jsQR(imageData.data, imageData.width, imageData.height);
        if (code?.data) {
          stopQrScanner();
          await importStudySetPayload(code.data);
          return;
        }
      }
      requestAnimationFrame(tick);
    };
    tick();
  }

  function stopQrScanner() {
    state.qr.scanning = false;
    state.qr.stream?.getTracks().forEach((track) => track.stop());
    state.qr.stream = null;
    $("#qrScanner")?.classList.add("hidden");
  }

  function renderSmartScan() {
    return `
      <section class="screen">
        ${screenHeader("Smart Scan Pro", "Tạo PDF scan từ nhiều ảnh", "", "create-hub")}
        <div class="hero-card"><h2>Màn hình scan tài liệu nhiều ảnh</h2><p>Chọn nhiều ảnh, giữ màu hoặc tăng tương phản trắng đen để OCR đọc tốt hơn.</p></div>
        <div class="section card pad">
          <h2>Bước 1: Chọn ảnh</h2>
          <p class="small-text">${state.smartScan.files.length ? `Đã chọn ${state.smartScan.files.length} ảnh.` : "Chọn nhiều ảnh tài liệu từ máy."}</p>
          <input id="smartScanInput" class="hidden" type="file" accept="image/*" multiple>
          <button class="btn primary" style="width:100%;margin-top:12px" onclick="document.getElementById('smartScanInput').click()">${icons.scan} Chọn nhiều ảnh</button>
        </div>
        <div class="section card pad">
          <h2>Bước 2: Chốt tạo PDF</h2>
          <div class="chip-row" style="margin-top:12px">
            <button class="chip ${state.smartScan.mode === "COLOR" ? "active" : ""}" onclick="KQuiz.setSmartScanMode('COLOR')">Giữ màu ảnh</button>
            <button class="chip ${state.smartScan.mode === "HIGH_CONTRAST_BW" ? "active" : ""}" onclick="KQuiz.setSmartScanMode('HIGH_CONTRAST_BW')">Trắng đen rõ chữ hơn</button>
          </div>
          <button class="btn primary" style="width:100%;margin-top:14px" ${state.smartScan.files.length ? "" : "disabled"} onclick="KQuiz.buildSmartScanPdf()">Chốt tạo PDF</button>
        </div>
        ${state.smartScan.pdfBlob ? `
          <div class="section card pad">
            <h2>Bước 3: PDF đã sẵn sàng</h2>
            <p class="small-text">Đã tạo PDF từ ${state.smartScan.pageCount} ảnh.</p>
            <div class="btn-row" style="margin-top:12px">
              <button class="btn secondary" onclick="KQuiz.saveSmartScanPdf()">Lưu file PDF</button>
              <button class="btn primary" onclick="KQuiz.useSmartScanPdf()">Qua nhập từ file</button>
            </div>
          </div>
        ` : ""}
      </section>
    `;
  }

  function handleSmartScanInput(event) {
    state.smartScan.files = Array.from(event.target.files || []);
    state.smartScan.pdfBlob = null;
    render();
  }

  function setSmartScanMode(mode) {
    state.smartScan.mode = mode;
    render();
  }

  async function buildSmartScanPdf() {
    if (!state.smartScan.files.length) return;
    if (!hasActiveUnlock("smart_scan", "")) {
      return requireProUnlock("smart_scan", "Smart Scan Pro", () => buildSmartScanPdf());
    }
    await loadScript(CDN.pdfLib);
    const pdf = await window.PDFLib.PDFDocument.create();
    for (const file of state.smartScan.files) {
      const bytes = await imageFileToPdfBytes(file, state.smartScan.mode);
      const img = file.type.includes("png")
        ? await pdf.embedPng(bytes)
        : await pdf.embedJpg(bytes);
      const page = pdf.addPage([img.width, img.height]);
      page.drawImage(img, { x: 0, y: 0, width: img.width, height: img.height });
    }
    state.smartScan.pdfBlob = new Blob([await pdf.save()], { type: "application/pdf" });
    state.smartScan.pageCount = state.smartScan.files.length;
    toast("Đã tạo PDF scan.", "success");
    render();
  }

  async function imageFileToPdfBytes(file, mode) {
    if (mode === "COLOR") return new Uint8Array(await file.arrayBuffer());
    const dataUrl = await preprocessImage(file);
    return dataUrlToBytes(dataUrl);
  }

  async function preprocessImage(file) {
    const bitmap = await createImageBitmap(file);
    const canvas = document.createElement("canvas");
    canvas.width = bitmap.width;
    canvas.height = bitmap.height;
    const ctx = canvas.getContext("2d");
    ctx.drawImage(bitmap, 0, 0);
    const image = ctx.getImageData(0, 0, canvas.width, canvas.height);
    for (let i = 0; i < image.data.length; i += 4) {
      const gray = image.data[i] * 0.299 + image.data[i + 1] * 0.587 + image.data[i + 2] * 0.114;
      const high = gray > 160 ? 255 : 0;
      image.data[i] = image.data[i + 1] = image.data[i + 2] = high;
    }
    ctx.putImageData(image, 0, 0);
    return canvas.toDataURL("image/jpeg", 0.92);
  }

  function dataUrlToBytes(dataUrl) {
    const raw = atob(dataUrl.split(",")[1]);
    const bytes = new Uint8Array(raw.length);
    for (let i = 0; i < raw.length; i += 1) bytes[i] = raw.charCodeAt(i);
    return bytes;
  }

  function saveSmartScanPdf() {
    downloadBlob(state.smartScan.pdfBlob, `kquiz-smart-scan-${Date.now()}.pdf`);
  }

  function useSmartScanPdf() {
    const file = new File([state.smartScan.pdfBlob], `kquiz-smart-scan-${Date.now()}.pdf`, { type: "application/pdf" });
    state.importFile.file = file;
    navigate("import-file");
  }

  function showPdfExport(setId) {
    openModal(`
      <h2>Xuất đề PDF Pro</h2>
      <p>Chọn kiểu PDF để xuất từ bộ học hiện tại.</p>
      <div class="list">
        <button class="btn" onclick="KQuiz.exportExamPdf('${setId}','exam')">Đề thi gọn</button>
        <button class="btn" onclick="KQuiz.exportExamPdf('${setId}','answers')">Đề + đáp án</button>
        <button class="btn" onclick="KQuiz.exportExamPdf('${setId}','review')">Phiếu ôn tập</button>
      </div>
    `);
  }

  async function exportExamPdf(setId, mode) {
    if (!hasActiveUnlock("pdf_pro", setId)) {
      return requireProUnlock("pdf_pro", "PDF Pro", () => exportExamPdf(setId, mode), { setId });
    }
    closeModal();
    await loadScript(CDN.pdfLib);
    const set = getSet(setId);
    const cards = getCardsForSet(setId);
    const pdf = await window.PDFLib.PDFDocument.create();
    let page = pdf.addPage([595, 842]);
    let y = 790;
    const font = await pdf.embedFont(window.PDFLib.StandardFonts.Helvetica);
    const draw = (text, size = 12) => {
      const clean = String(text || "").normalize("NFKD").replace(/[^\x00-\x7F]/g, "");
      if (y < 60) { page = pdf.addPage([595, 842]); y = 790; }
      page.drawText(clean.slice(0, 96), { x: 42, y, size, font, color: window.PDFLib.rgb(0.12, 0.16, 0.24) });
      y -= size + 10;
    };
    draw(`KQuiz - ${set.title}`, 20);
    draw(`Mode: ${mode} - Total: ${cards.length}`, 12);
    cards.forEach((card, index) => {
      draw(`${index + 1}. ${card.term}`, 13);
      const choices = card.choices?.length ? card.choices : [card.definition];
      choices.forEach((choice) => draw(`   ${choice}`, 11));
      if (mode !== "exam") draw(`Answer: ${card.definition}`, 11);
      if (mode === "review") { draw("Notes: ______________________________", 11); draw("______________________________", 11); }
      y -= 4;
    });
    const blob = new Blob([await pdf.save()], { type: "application/pdf" });
    downloadBlob(blob, `${safeFileName(set.title)}-${mode}.pdf`);
    toast("Đã xuất PDF.", "success");
  }

  function renderAbout() {
    return `
      <section class="screen">
        ${screenHeader("Giới thiệu", "KQuiz Web ${APP_VERSION}", "", "tools-hub")}
        <div class="hero-card"><h2>KQuiz</h2><p>App học quiz thông minh, offline-first, tạo câu hỏi từ file, ảnh, PDF và bộ học.</p></div>
        <div class="section card pad">
          <h2>Theme màu nhẹ</h2>
          <div class="chip-row" style="margin-top:12px">
            ${themeChip("kquiz_blue","Xanh KQuiz")}
            ${themeChip("mint","Mint")}
            ${themeChip("lavender","Lavender")}
            ${themeChip("peach","Peach")}
          </div>
        </div>
        <div class="section card pad">
          <h2>Công nghệ</h2>
          <div class="chip-row" style="flex-wrap:wrap;margin-top:12px">
            ${["IndexedDB","PDF.js","Tesseract.js","pdf-lib","QR Share","PWA","Web Speech"].map((x) => `<span class="chip">${x}</span>`).join("")}
          </div>
        </div>
        <div class="section card pad">
          <h2>AI Pro endpoint</h2>
          <p class="small-text">Không lưu API key trong frontend. Nhập URL proxy nếu đã deploy backend hợp lệ.</p>
          <input id="aiEndpoint" class="input" value="${escapeHtml(state.settings.aiEndpoint || "")}" placeholder="https://your-worker.workers.dev">
          <button class="btn primary" style="width:100%;margin-top:12px" onclick="KQuiz.saveAiEndpoint()">Lưu endpoint</button>
        </div>
        <button class="action-card wide" style="width:100%;margin-top:28px" onclick="KQuiz.showDonate()"><span class="glyph">${icons.qr}</span><span><strong>Ủng hộ KQuiz</strong><span>Mở thông tin MBank và mã QR</span></span></button>
      </section>
    `;
  }

  function themeChip(key, label) {
    return `<button class="chip ${state.settings.theme === key ? "active" : ""}" onclick="KQuiz.setTheme('${key}')">${label}</button>`;
  }

  async function setTheme(theme) {
    await saveSettings({ theme });
    render();
  }

  async function saveAiEndpoint() {
    await saveSettings({ aiEndpoint: $("#aiEndpoint").value.trim() });
    toast("Đã lưu endpoint.", "success");
  }

  async function saveAdsSettings() {
    const ads = {
      enabled: Boolean($("#adsEnabled")?.checked),
      networkCode: normalizeText($("#adsNetworkCode")?.value || ""),
      adUnitPath: normalizeText($("#adsAdUnitPath")?.value || ""),
      timeoutMs: 15000
    };
    await saveSettings({ ads });
    toast("Đã lưu cấu hình rewarded ads.", "success");
    render();
  }

  function showDonate() {
    openModal(`
      <h2>Ủng hộ KQuiz</h2>
      <p>MBank • NGO DINH KHOI</p>
      <div class="card pad"><strong>Số tài khoản</strong><p>0961697407</p></div>
      <button class="btn primary" style="width:100%;margin-top:12px" onclick="KQuiz.copyText('0961697407')">Copy số tài khoản</button>
    `);
  }

  function showAudioSettings() {
    const effects = state.settings.effects || defaultSettings.effects;
    openModal(`
      <h2>Cài đặt âm thanh</h2>
      <label class="option-card"><input type="checkbox" id="voiceEnabled" ${state.settings.voiceEnabled ? "checked" : ""}> Giọng đọc TTS</label>
      <label class="option-card"><input type="checkbox" id="sfxEnabled" ${state.settings.sfxEnabled ? "checked" : ""}> Hiệu ứng âm thanh</label>
      <label class="form-row"><span class="field-label">Âm lượng</span><input id="sfxVolume" type="range" min="0" max="1" step="0.05" value="${state.settings.sfxVolume}" class="input"></label>
      <div class="chip-row" style="flex-wrap:wrap;margin-top:12px">
        ${Object.keys(effects).map((key) => `<label class="chip"><input type="checkbox" data-effect="${key}" ${effects[key] ? "checked" : ""}> ${key}</label>`).join("")}
      </div>
      <button class="btn primary" style="width:100%;margin-top:16px" onclick="KQuiz.saveAudioSettings()">Xong</button>
    `);
  }

  async function saveAudioSettings() {
    const effects = {};
    $$("[data-effect]").forEach((input) => effects[input.dataset.effect] = input.checked);
    await saveSettings({
      voiceEnabled: $("#voiceEnabled").checked,
      sfxEnabled: $("#sfxEnabled").checked,
      sfxVolume: Number($("#sfxVolume").value),
      effects
    });
    closeModal();
  }

  function configureDailyGoal() {
    openModal(`
      <h2>Mục tiêu hằng ngày</h2>
      <div class="chip-row">${[5,10,20,30].map((goal) => `<button class="chip ${state.settings.dailyGoal === goal ? "active" : ""}" onclick="KQuiz.setDailyGoal(${goal})">${goal} thẻ</button>`).join("")}</div>
    `);
  }

  async function setDailyGoal(goal) {
    await saveSettings({ dailyGoal: goal });
    closeModal();
    render();
  }

  function speak(text) {
    if (!state.settings.voiceEnabled || !("speechSynthesis" in window)) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = "vi-VN";
    utterance.volume = 0.85;
    window.speechSynthesis.speak(utterance);
  }

  let audioCtx = null;
  function play(effect) {
    if (!state.settings.sfxEnabled || state.settings.effects?.[effect] === false) return;
    audioCtx ||= new (window.AudioContext || window.webkitAudioContext)();
    const freq = { correct: 660, wrong: 220, flip: 880, complete: 523, select: 440 }[effect] || 440;
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.frequency.value = freq;
    osc.type = effect === "wrong" ? "sawtooth" : "sine";
    gain.gain.value = 0.08 * (state.settings.sfxVolume ?? 0.7);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.16);
    osc.stop(audioCtx.currentTime + 0.18);
  }

  async function showMixedTestModal() {
    const eligible = state.studySets.filter((set) => getCardsForSet(set.id).length);
    openModal(`
      <h2>Trộn nhiều bộ học</h2>
      <p>Chọn từ 2 bộ trở lên để tạo một bài test chung.</p>
      <div class="list">${eligible.map((set) => `<label class="option-card"><input type="checkbox" data-mixed="${set.id}"> ${escapeHtml(set.title)} (${getCardsForSet(set.id).length} thẻ)</label>`).join("")}</div>
      <button class="btn primary" style="width:100%;margin-top:14px" onclick="KQuiz.startMixedTest()">Bắt đầu</button>
    `);
  }

  function startMixedTest() {
    const ids = $$("[data-mixed]:checked").map((input) => input.dataset.mixed);
    if (ids.length < 2) return toast("Cần chọn ít nhất 2 bộ.", "warning");
    closeModal();
    navigate("test-config", { id: "mixed", mixed: ids.join(",") });
  }

  async function exportBackup() {
    await loadAll();
    const backup = {
      formatVersion: 1,
      appName: "KQuiz",
      exportedAt: now(),
      studySets: state.studySets,
      flashcards: state.cards,
      quizHistory: state.history,
      folders: state.folders,
      tags: getAllTags(),
      settings: state.settings,
      achievements: Object.values(state.achievements),
      adRewards: state.adRewards
    };
    downloadBlob(new Blob([JSON.stringify(backup, null, 2)], { type: "application/json" }), `kquiz-${Date.now()}.kquizbackup`);
  }

  async function handleBackupInput(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    const data = JSON.parse(await file.text());
    for (const set of data.studySets || []) await dbPut("studySets", { ...set, id: set.id || uid("set") });
    for (const card of data.flashcards || []) await dbPut("flashcards", { ...card, id: card.id || uid("card") });
    for (const item of data.quizHistory || []) await dbPut("quizHistory", { ...item, id: item.id || uid("hist") });
    for (const folder of data.folders || []) await dbPut("folders", { ...folder, id: folder.id || uid("folder") });
    for (const reward of data.adRewards || []) await dbPut("adRewards", { ...reward, id: reward.id || uid("reward") });
    if (data.settings) await saveSettings({ ...state.settings, ...data.settings });
    toast("Đã khôi phục dữ liệu, không xóa dữ liệu cũ.", "success");
    render();
  }

  function downloadBlob(blob, fileName) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  function loadScript(src) {
    return new Promise((resolve, reject) => {
      const existing = document.querySelector(`script[src="${src}"]`);
      if (existing) {
        existing.addEventListener("load", resolve, { once: true });
        if (existing.dataset.loaded) resolve();
        return;
      }
      const script = document.createElement("script");
      script.src = src;
      script.async = true;
      script.onload = () => { script.dataset.loaded = "true"; resolve(); };
      script.onerror = reject;
      document.head.appendChild(script);
    });
  }

  function notFound(title, back = "home") {
    return `<section class="screen">${screenHeader(title, "", "", back)}<div class="empty-state card"><div><strong>${escapeHtml(title)}</strong><span>Quay lại để tiếp tục.</span></div></div></section>`;
  }

  async function init() {
    state.modules.shell = await import("./modules/app-shell.js");
    await initDB();
    await seedIfNeeded();
    await loadAll();
    const isLocalPreview = ["localhost", "127.0.0.1", "::1"].includes(location.hostname);
    if ("serviceWorker" in navigator && !isLocalPreview) {
      navigator.serviceWorker.register("./sw.js").catch((error) => console.warn("SW register failed", error));
    }
    window.addEventListener("hashchange", render);
    await render();
  }

  window.KQuiz = {
    navigate,
    closeModal: (event) => closeModal(event),
    showImportNotice,
    showAudioSettings,
    saveAudioSettings,
    configureDailyGoal,
    setDailyGoal,
    setQuickType,
    copyPrompt,
    createStudySetFromQuick,
    setImportMode,
    processSelectedFile,
    setQuizDifficulty,
    startGeneratedQuiz,
    saveGeneratedAsStudySet,
    selectQuizAnswer,
    jumpQuiz,
    prevQuiz,
    nextQuiz,
    submitQuiz,
    retryQuiz,
    startReviewWrongFromQuiz,
    clearHistory,
    toggleSet,
    renameSet,
    duplicateSet,
    deleteSet,
    showMixedTestModal,
    startMixedTest,
    startFlashcard,
    flipFlashcard,
    prevFlashcard,
    nextFlashcard,
    speakCurrentFlash,
    startLearn,
    answerLearn,
    jumpLearn,
    prevLearn,
    nextLearn,
    finishLearn,
    startTestSession,
    selectTestAnswer,
    jumpTest,
    prevTest,
    nextTest,
    submitTest,
    retakeTest,
    startReviewWrong,
    toggleCardStar,
    editCard,
    saveCard,
    deleteCard,
    showSetMenu,
    exportStudySet,
    showQr,
    copyText,
    startQrScanner,
    stopQrScanner,
    clearImportPreview,
    confirmImportStudySet,
    setSmartScanMode,
    buildSmartScanPdf,
    saveSmartScanPdf,
    useSmartScanPdf,
    showPdfExport,
    exportExamPdf,
    showCreateFolder,
    createFolder,
    deleteFolder,
    assignFolder,
    setFolder,
    editTags,
    saveSetTags,
    toggleCardSelection,
    selectAllCards,
    bulkDeleteSelectedCards,
    rateSmart,
    setTheme,
    saveAiEndpoint,
    saveAdsSettings,
    confirmRewardedUnlock,
    showDonate,
    exportBackup,
    syncQuickForm
  };

  init().catch((error) => {
    console.error(error);
    app.innerHTML = `<section class="screen"><div class="empty-state card"><div><strong>Không khởi động được KQuiz Web</strong><span>${escapeHtml(error.message)}</span></div></div></section>`;
  });
})();
