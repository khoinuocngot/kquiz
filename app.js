'use strict';

// ============================================================
// STORAGE - IndexedDB
// ============================================================
const DB_NAME = 'kquiz_db_v2';
const DB_VERSION = 2;
let db = null;

async function initDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onerror = () => reject(request.error);
    request.onsuccess = () => { db = request.result; resolve(db); };
    request.onupgradeneeded = (e) => {
      const database = e.target.result;
      if (!database.objectStoreNames.contains('study_sets')) {
        const ss = database.createObjectStore('study_sets', { keyPath: 'id', autoIncrement: true });
        ss.createIndex('createdAt', 'createdAt');
      }
      if (!database.objectStoreNames.contains('flashcards')) {
        const fc = database.createObjectStore('flashcards', { keyPath: 'id', autoIncrement: true });
        fc.createIndex('studySetId', 'studySetId');
      }
      if (!database.objectStoreNames.contains('quiz_history')) {
        const qh = database.createObjectStore('quiz_history', { keyPath: 'id', autoIncrement: true });
        qh.createIndex('createdAt', 'createdAt');
      }
      if (!database.objectStoreNames.contains('study_stats')) {
        database.createObjectStore('study_stats', { keyPath: 'date' });
      }
      if (!database.objectStoreNames.contains('user_data')) {
        const ud = database.createObjectStore('user_data', { keyPath: 'key' });
      }
    };
  });
}

async function dbGet(store, key) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(store, 'readonly');
    const req = tx.objectStore(store).get(key);
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

async function dbGetAll(store) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(store, 'readonly');
    const req = tx.objectStore(store).getAll();
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

async function dbPut(store, data) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(store, 'readwrite');
    const req = tx.objectStore(store).put(data);
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

async function dbDelete(store, key) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(store, 'readwrite');
    const req = tx.objectStore(store).delete(key);
    req.onsuccess = () => resolve();
    req.onerror = () => reject(req.error);
  });
}

async function dbClear(store) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(store, 'readwrite');
    const req = tx.objectStore(store).clear();
    req.onsuccess = () => resolve();
    req.onerror = () => reject(req.error);
  });
}

async function dbGetByIndex(store, indexName, value) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(store, 'readonly');
    const idx = tx.objectStore(store).index(indexName);
    const req = idx.getAll(value);
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

// ============================================================
// UTILITIES
// ============================================================
function $(selector) { return document.querySelector(selector); }
function $$(selector) { return [...document.querySelectorAll(selector)]; }

function showToast(message, type = 'default', duration = 3000) {
  const container = $('#toast-container');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => {
    toast.classList.add('leaving');
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

function formatDate(ts) {
  const d = new Date(ts);
  const now = new Date();
  const diff = now - d;
  if (diff < 60000) return 'Vừa xong';
  if (diff < 3600000) return `${Math.floor(diff / 60000)} phút trước`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} giờ trước`;
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} ngày trước`;
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatDateShort(ts) {
  return new Date(ts).toLocaleDateString('vi-VN', { day: '2-digit', month: 'short' });
}

function escapeHtml(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

function shuffle(arr) {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

function clamp(v, min, max) { return Math.min(Math.max(v, min), max); }

function getGreeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Sáng tốt lành!';
  if (hour < 17) return 'Chiều vui vẻ!';
  if (hour < 21) return 'Tối an lành!';
  return 'Đêm muộn rồi nè!';
}

// ============================================================
// ICONS (SVG inline)
// ============================================================
const icons = {
  home: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>`,
  book: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>`,
  clock: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>`,
  plus: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>`,
  back: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>`,
  check: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>`,
  x: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`,
  trash: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>`,
  flag: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>`,
  refresh: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>`,
  edit: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>`,
  file: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>`,
  folder: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>`,
  upload: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>`,
  text: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="17" y1="10" x2="3" y2="10"/><line x1="21" y1="6" x2="3" y2="6"/><line x1="21" y1="14" x2="3" y2="14"/><line x1="17" y1="18" x2="3" y2="18"/></svg>`,
  star: `<svg viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>`,
  starOutline: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>`,
  chevronRight: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>`,
  chevronDown: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>`,
  volume: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>`,
  flame: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/></svg>`,
  zap: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>`,
  award: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="7"/><polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88"/></svg>`,
  brain: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9.5 2A2.5 2.5 0 0 1 12 4.5v15a2.5 2.5 0 0 1-4.96.44 2.5 2.5 0 0 1-2.96-3.08 3 3 0 0 1-.34-5.58 2.5 2.5 0 0 1 1.32-4.24 2.5 2.5 0 0 1 1.98-3A2.5 2.5 0 0 1 9.5 2Z"/><path d="M14.5 2A2.5 2.5 0 0 0 12 4.5v15a2.5 2.5 0 0 0 4.96.44 2.5 2.5 0 0 0 2.96-3.08 3 3 0 0 0 .34-5.58 2.5 2.5 0 0 0-1.32-4.24 2.5 2.5 0 0 0-1.98-3A2.5 2.5 0 0 0 14.5 2Z"/></svg>`,
  settings: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>`,
  info: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>`,
  search: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>`,
  layers: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>`,
  copy: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>`,
  flashcard: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="M10 4v4"/><path d="M14 4v4"/><path d="M10 14v4"/><path d="M14 14v4"/></svg>`,
  school: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c0 2 2 3 6 3s6-1 6-3v-5"/></svg>`,
  target: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/></svg>`,
  sparkles: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l1.5 4.5L18 9l-4.5 1.5L12 15l-1.5-4.5L6 9l4.5-1.5z"/><path d="M19 13l.5 1.5L21 15l-1.5.5L19 17l-.5-1.5L17 15l1.5-.5z"/><path d="M6 15l.5 1.5L8 17l-1.5.5L6 19l-.5-1.5L4 17l1.5-.5z"/></svg>`,
};

// ============================================================
// STATE
// ============================================================
const state = {
  currentScreen: 'home',
  history: [],
  studySets: [],
  currentStudySet: null,
  currentStudySetCards: [],
  currentCardIndex: 0,
  currentFlashcardFlipped: false,
  quizSession: null,
  userAnswers: {},
  currentQuestionIndex: 0,
  quizConfig: { questionCount: 10, difficulty: 'Trung bình' },
  extractedContent: null,
  learnSession: null,
  learnAnswers: {},
  testSession: null,
  testAnswers: {},
  flaggedQuestions: new Set(),
  smartReviewCards: [],
  dailyStats: null,
  // Gamification
  totalXp: 0,
  currentLevel: 1,
  xpProgressInLevel: 0,
  dailyGoalCurrent: 0,
  dailyGoalTarget: 10,
  currentStreak: 0,
  maxStreak: 0,
  todayCards: 0,
  needsReviewCount: 0,
};

// ============================================================
// XP & LEVEL SYSTEM
// ============================================================
const XP_PER_LEVEL = 100;

function calculateLevel(totalXp) {
  return Math.floor(totalXp / XP_PER_LEVEL) + 1;
}

function getXpForNextLevel(level) {
  return level * XP_PER_LEVEL;
}

function getXpInCurrentLevel(totalXp) {
  return totalXp % XP_PER_LEVEL;
}

async function addXp(amount) {
  state.totalXp += amount;
  const newLevel = calculateLevel(state.totalXp);
  const leveledUp = newLevel > state.currentLevel;
  state.currentLevel = newLevel;
  state.xpProgressInLevel = getXpInCurrentLevel(state.totalXp);
  await dbPut('user_data', { key: 'totalXp', value: state.totalXp });
  await dbPut('user_data', { key: 'currentLevel', value: state.currentLevel });
  return leveledUp;
}

async function loadUserData() {
  const totalXp = await dbGet('user_data', 'totalXp');
  const currentLevel = await dbGet('user_data', 'currentLevel');
  const dailyGoal = await dbGet('user_data', 'dailyGoalTarget');
  const maxStreak = await dbGet('user_data', 'maxStreak');

  if (totalXp) state.totalXp = totalXp.value;
  if (currentLevel) state.currentLevel = currentLevel.value;
  if (dailyGoal) state.dailyGoalTarget = dailyGoal.value;
  if (maxStreak) state.maxStreak = maxStreak.value;
  state.xpProgressInLevel = getXpInCurrentLevel(state.totalXp);
}

async function loadDailyStats() {
  const today = new Date().toISOString().split('T')[0];
  const todayStats = await dbGet('study_stats', today);
  const yesterday = new Date(Date.now() - 86400000).toISOString().split('T')[0];
  const yStats = await dbGet('study_stats', yesterday);

  if (todayStats) {
    state.todayCards = todayStats.cardsReviewed || 0;
    state.currentStreak = todayStats.streakCount || 0;
  }
  state.dailyGoalCurrent = state.todayCards;

  // Calculate needs review
  const allSets = await dbGetAll('study_sets');
  let needsReview = 0;
  for (const ss of allSets) {
    const cards = await dbGetByIndex('flashcards', 'studySetId', ss.id);
    needsReview += cards.filter(c => c.masteryLevel < 4).length;
  }
  state.needsReviewCount = needsReview;
}

async function saveDailyStats() {
  const today = new Date().toISOString().split('T')[0];
  const yesterday = new Date(Date.now() - 86400000).toISOString().split('T')[0];
  const yStats = await dbGet('study_stats', yesterday);

  let stats = await dbGet('study_stats', today) || { date: today, cardsReviewed: 0, studySetsCount: 0, streakCount: 0 };
  stats.cardsReviewed = state.todayCards;
  stats.streakCount = state.currentStreak;
  await dbPut('study_stats', stats);

  // Update max streak
  if (state.currentStreak > state.maxStreak) {
    state.maxStreak = state.currentStreak;
    await dbPut('user_data', { key: 'maxStreak', value: state.maxStreak });
  }
}

async function incrementDailyCards(count = 1) {
  const today = new Date().toISOString().split('T')[0];
  const yesterday = new Date(Date.now() - 86400000).toISOString().split('T')[0];
  const yStats = await dbGet('study_stats', yesterday);

  let stats = await dbGet('study_stats', today) || { date: today, cardsReviewed: 0, studySetsCount: 0, streakCount: 0 };
  stats.cardsReviewed = (stats.cardsReviewed || 0) + count;
  await dbPut('study_stats', stats);

  state.todayCards = stats.cardsReviewed;
  state.dailyGoalCurrent = state.todayCards;
  state.currentStreak = yStats ? (yStats.streakCount || 0) + 1 : 1;
  await saveDailyStats();

  // Check daily goal
  if (state.todayCards >= state.dailyGoalTarget) {
    showToast('Chuc mung ban da hoan thanh muc tieu hom nay!', 'success');
  }
}

// ============================================================
// ROUTER
// ============================================================
async function __navigateInternal(screen, data) {
  state.currentScreen = screen;
  const app = $('#app');
  if (!app) return;

  let html;
  switch (screen) {
    case 'home': html = await renderHome(); break;
    case 'import': html = renderImport(); break;
    case 'processing': html = renderProcessing(); break;
    case 'quiz-config': html = await renderQuizConfig(); break;
    case 'quiz': html = renderQuiz(); break;
    case 'result': html = renderResult(); break;
    case 'history': html = renderHistory(); break;
    case 'study-sets': html = renderStudySets(); break;
    case 'study-set-detail': html = renderStudySetDetail(); break;
    case 'flashcard': html = renderFlashcard(); break;
    case 'learn': html = renderLearn(); break;
    case 'learn-result': html = renderLearnResult(); break;
    case 'test-config': html = renderTestConfig(); break;
    case 'test': html = renderTest(); break;
    case 'test-result': html = renderTestResult(); break;
    case 'review-wrong': html = renderReviewWrong(); break;
    case 'smart-review': html = renderSmartReview(); break;
    case 'smart-review-config': html = renderSmartReviewConfig(); break;
    case 'quick-text': html = renderQuickText(); break;
    case 'create-study-set': html = renderCreateStudySet(); break;
    case 'about': html = renderAbout(); break;
    default: html = await renderHome();
  }

  app.innerHTML = html;
  attachEventListeners();
  window.scrollTo(0, 0);
}

function navigate(screen, data) {
  __navigateInternal(screen, data);
}

// ============================================================
// CONTENT CLEANER
// ============================================================
function cleanText(rawText) {
  let text = rawText
    .replace(/\f/g, '\n')
    .replace(/\u00A0/g, ' ')
    .replace(/[\u200B-\u200D\uFEFF]/g, '')
    .replace(/((https?:\/\/|www\.)[^\s]+)/gi, '')
    .replace(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g, '')
    .replace(/(\*{1,3}|_{1,3}|~{1,3})([^*~\n]+)(\*{1,3}|_{1,3}|~{1,3})/g, '$2')
    .replace(/`{1,3}[^`]+`{1,3}/g, '')
    .replace(/!\[.*?\]\(.*?\)/g, '')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/#{1,6}\s*([^\n]+)/g, '$1')
    .replace(/^\s*[-*+]\s+/gm, '')
    .replace(/^\s*\d+\.\s+/gm, '')
    .replace(/^\s*>\s+/gm, '')
    .replace(/\|[^\n]+\|/g, '')
    .replace(/^[-=]{3,}$/gm, '')
    .replace(/\n{3,}/g, '\n\n')
    .replace(/^\s+$/gm, '')
    .trim();
  return text;
}

function extractSentences(text) {
  return text
    .split(/[.!?]+/)
    .map(s => s.trim())
    .filter(s => s.length > 15 && s.length < 300 && /[a-zA-Zàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]{5,}/i.test(s))
    .map(s => s.replace(/^["'""«»『』「」『』]/g, '').replace(/["'""«»『』「」『』]$/g, '').trim());
}

// ============================================================
// QUIZ GENERATOR
// ============================================================
const QUESTION_TEMPLATES = [
  { template: (c, w) => `${w} là gì?`, keywordPos: 'start' },
  { template: (c, w) => `Thuật ngữ "${w}" có nghĩa là gì?`, keywordPos: 'mid' },
  { template: (c, w) => `"${w}" được định nghĩa như thế nào?`, keywordPos: 'mid' },
  { template: (c, w) => `${w} là một dạng của loại nào?`, keywordPos: 'start' },
  { template: (c, w) => `Đâu không phải là đặc điểm của ${w}?`, keywordPos: 'mid' },
  { template: (c, w) => `Mục đích chính của ${w} là gì?`, keywordPos: 'start' },
  { template: (c, w) => `${w} khác với đối tượng nào?`, keywordPos: 'start' },
  { template: (c, w) => `"${w}" thuộc nhóm nào?`, keywordPos: 'mid' },
  { template: (c, w) => `Yếu tố nào ảnh hưởng đến ${w}?`, keywordPos: 'end' },
  { template: (c, w) => `${w} hoạt động theo cơ chế nào?`, keywordPos: 'start' },
];

const BLACKLIST_SENTENCES = /^(bảng|mục|chương|hình|ảnh|bài|phần|vídụ|ghi chú|tài liệu|tham khảo|mục lục|lời nói đầu|tóm tắt|kết luận|bài tập|câu hỏi|lời giải|đáp án|bước|cách|phương pháp|công thức|bằng|có thể|được sử dụng|được gọi|được chia)/i;
const BLACKLIST_ANSWERS = /^(không có|n\/a|không|xem|trên|dưới|ở đây|tùy|theo|khác|nói chung|nói cách|vídụ|có thể|bảng|hình)/i;

function extractKeywords(text) {
  const words = text.split(/\s+/).filter(w => w.length > 3 && /[a-zA-Zàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/i.test(w));
  const freq = {};
  words.forEach(w => {
    const lw = w.toLowerCase().replace(/[^a-zA-Zàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/g, '');
    if (lw.length > 3) freq[lw] = (freq[lw] || 0) + 1;
  });
  return Object.entries(freq)
    .filter(([_, v]) => v >= 2)
    .sort((a, b) => b[1] - a[1])
    .map(([w]) => w);
}

function extractKeywordsFromSentences(sentences) {
  const all = sentences.join(' ');
  return extractKeywords(all);
}

function pickCorrectSentence(sentences, difficulty) {
  const filtered = sentences.filter(s => {
    if (BLACKLIST_SENTENCES.test(s.trim())) return false;
    const len = s.trim().length;
    if (difficulty === 'Dễ') return len >= 20 && len <= 120;
    if (difficulty === 'Khó') return len >= 30 && len <= 250;
    return len >= 25 && len <= 180;
  });
  if (filtered.length === 0) return sentences[Math.floor(Math.random() * sentences.length)];
  return filtered[Math.floor(Math.random() * filtered.length)];
}

function pickKeyword(sentence) {
  const words = sentence.split(/\s+/).filter(w => w.length > 4 && /^[a-zA-Zàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/i.test(w));
  if (words.length === 0) return sentence.split(/\s+/)[Math.floor(Math.random() * 5)];
  const candidates = words.filter(w => w.length < 20);
  if (candidates.length === 0) return words[0];
  return candidates[Math.floor(Math.random() * candidates.length)];
}

function buildDistractors(correctAnswer, allKeywords) {
  const distractors = [];
  const seen = new Set([correctAnswer.toLowerCase()]);
  const shuffled = shuffle(allKeywords.filter(k => k.toLowerCase() !== correctAnswer.toLowerCase()));
  for (const kw of shuffled) {
    if (BLACKLIST_ANSWERS.test(kw)) continue;
    if (!seen.has(kw.toLowerCase())) {
      distractors.push(kw);
      seen.add(kw.toLowerCase());
    }
    if (distractors.length >= 3) break;
  }
  while (distractors.length < 3) {
    distractors.push(`Phương án ${distractors.length + 2}`);
  }
  return distractors.slice(0, 3);
}

function generateQuiz(text, questionCount, difficulty) {
  const cleaned = cleanText(text);
  const sentences = extractSentences(cleaned);
  if (sentences.length === 0) return { questions: [], warning: 'Không tìm thấy câu hỏi nào trong nội dung này.' };

  const keywords = extractKeywordsFromSentences(sentences);
  const maxQ = Math.min(questionCount, sentences.length);
  const selected = shuffle(sentences).slice(0, maxQ * 2);

  const questions = [];
  const usedSnippets = new Set();

  for (const sentence of selected) {
    if (questions.length >= maxQ) break;
    const snip = sentence.toLowerCase().trim().substring(0, 50);
    if (usedSnippets.has(snip)) continue;

    const correct = pickCorrectSentence([sentence], difficulty);
    const keyword = pickKeyword(correct);
    if (!keyword || keyword.length < 4) continue;

    const tmpl = QUESTION_TEMPLATES[Math.floor(Math.random() * QUESTION_TEMPLATES.length)];
    let questionText = tmpl.template(correct, keyword);

    const distractors = buildDistractors(keyword, keywords);
    if (distractors.length < 3) continue;

    const allOptions = shuffle([keyword, ...distractors]);
    const correctIndex = allOptions.indexOf(keyword);
    if (correctIndex === -1) continue;

    usedSnippets.add(snip);
    questions.push({
      id: Math.random().toString(36).substr(2, 9),
      question: questionText,
      options: allOptions,
      correctAnswerIndex: correctIndex,
      explanation: correct,
      sourceSnippet: correct.substring(0, 150),
    });
  }

  if (questions.length === 0) return { questions: [], warning: 'Không đủ nội dung để tạo quiz. Hãy thử với nội dung dài hơn.' };
  return { questions: shuffle(questions).slice(0, maxQ), warning: null };
}

// ============================================================
// SCREEN: HOME
// ============================================================
async function renderHome() {
  const studySets = await dbGetAll('study_sets');
  const history = await dbGetAll('quiz_history');

  await loadUserData();
  await loadDailyStats();

  const totalCards = studySets.reduce((sum, ss) => sum + (ss.cardCount || 0), 0);
  const recentSets = studySets.sort((a, b) => b.updatedAt - a.updatedAt).slice(0, 5);
  const recentHistory = history.sort((a, b) => b.createdAt - a.createdAt).slice(0, 3);

  const xpNeeded = getXpForNextLevel(state.currentLevel);
  const xpPercent = Math.round((state.xpProgressInLevel / xpNeeded) * 100);
  const goalPercent = Math.min(100, Math.round((state.dailyGoalCurrent / state.dailyGoalTarget) * 100));
  const circumference = 2 * Math.PI * 26;
  const dashOffset = circumference - (goalPercent / 100) * circumference;

  const sourceColor = (type) => {
    if (type === 'quick_import' || type === 'quick_import') return '#22C55E';
    if (type === 'file_import') return '#3B82F6';
    return '#6B7280';
  };

  return `
  <div class="screen">
    <div class="screen-header">
      <div class="screen-header-title">
        <div class="title">Kquiz</div>
        <div class="greeting">${getGreeting()}</div>
      </div>
      <button class="btn btn-icon" onclick="navigate('about')">${icons.info}</button>
    </div>

    <div class="screen-content">

      <!-- Hero -->
      <div class="hero-section">
        <div class="hero-inner">
          <div class="hero-title">Học nhanh.</div>
          <div class="hero-title" style="opacity:0.85">Nhớ lâu.</div>
          <div class="hero-subtitle">Tạo quiz thông minh từ tài liệu của bạn</div>
          <div class="hero-stats">
            <div class="hero-stat">
              <div class="value">${studySets.length}</div>
              <div class="label">bộ học</div>
            </div>
            <div class="hero-stat">
              <div class="value">${totalCards}</div>
              <div class="label">thẻ</div>
            </div>
          </div>
          <button class="hero-create-btn" onclick="showQuickImportModal()">
            ${icons.plus} Tạo bộ học mới
          </button>
        </div>
      </div>

      <!-- XP & Level -->
      ${state.totalXp > 0 ? `
      <div class="xp-level-widget">
        <div class="xp-level-header">
          <div class="xp-level-badge">${state.currentLevel}</div>
          <div class="xp-level-info">
            <div class="xp-level-title">Cấp ${state.currentLevel}</div>
            <div class="xp-level-sub">${state.xpProgressInLevel} / ${xpNeeded} XP</div>
          </div>
        </div>
        <div class="xp-bar">
          <div class="xp-bar-track">
            <div class="xp-bar-fill" style="width:${xpPercent}%"></div>
          </div>
          <div class="xp-bar-labels">
            <span>Level ${state.currentLevel}</span>
            <span>Level ${state.currentLevel + 1}</span>
          </div>
        </div>
      </div>
      ` : ''}

      <!-- Daily Goal -->
      <div class="daily-goal-widget">
        <div class="daily-goal-header">
          <div class="daily-goal-title">
            ${icons.target} Mục tiêu hàng ngày
          </div>
          <button class="daily-goal-config" onclick="showDailyGoalModal()">Cài đặt</button>
        </div>
        <div class="daily-goal-body">
          <div class="daily-goal-ring">
            <svg width="64" height="64" viewBox="0 0 64 64">
              <circle cx="32" cy="32" r="26" fill="none" stroke-width="5" class="daily-goal-ring-bg" stroke="#EEF2FF"/>
              <circle cx="32" cy="32" r="26" fill="none" stroke-width="5" class="daily-goal-ring-fill"
                stroke="#14B8A6" stroke-dasharray="${circumference}" stroke-dashoffset="${dashOffset}"/>
            </svg>
            <div class="daily-goal-ring-text">${state.dailyGoalCurrent}</div>
          </div>
          <div class="daily-goal-info">
            <div class="label">${state.dailyGoalCurrent} / ${state.dailyGoalTarget} thẻ</div>
            <div class="sub">Mục tiêu: ${state.dailyGoalTarget} thẻ mỗi ngày</div>
          </div>
        </div>
      </div>

      <!-- Streak & Smart Review -->
      ${(state.currentStreak > 0 || state.todayCards > 0 || state.needsReviewCount > 0) ? `
      <div style="display:flex;flex-direction:column;gap:12px">
        ${state.currentStreak > 0 ? `
        <div class="streak-widget" onclick="navigate('smart-review-config')">
          <div class="streak-inner">
            <div class="streak-icon">🔥</div>
            <div class="streak-info">
              <div class="streak-title">${state.currentStreak} ngày liên tiếp</div>
              <div class="streak-sub">Best: ${state.maxStreak} ngày · Hôm nay: ${state.todayCards} thẻ</div>
            </div>
            ${icons.chevronRight}
          </div>
        </div>
        ` : ''}
        ${state.needsReviewCount > 0 ? `
        <div class="smart-review-banner" onclick="navigate('smart-review-config')">
          <div class="smart-review-inner">
            <div class="smart-review-icon">✨</div>
            <div class="smart-review-info">
              <div class="smart-review-title">${state.needsReviewCount} thẻ cần ôn</div>
              <div class="smart-review-sub">Smart Review giúp bạn ôn lại thẻ yếu</div>
            </div>
            ${icons.chevronRight}
          </div>
        </div>
        ` : ''}
        <div class="organize-banner" onclick="navigate('study-sets')">
          <div class="organize-inner">
            <div class="organize-icon-wrap">${icons.folder}</div>
            <div class="organize-info">
              <div class="organize-title">Tổ chức bộ học</div>
              <div class="organize-sub">Sắp xếp theo thư mục, gắn tag</div>
            </div>
            ${icons.chevronRight}
          </div>
        </div>
      </div>
      ` : ''}

      <!-- Quick Actions -->
      <div>
        <div class="section-header">
          <div class="section-title">Bắt đầu học tập</div>
        </div>
        <div class="quick-actions">
          <div class="quick-action" onclick="showQuickTextModal()">
            <div class="quick-action-icon-wrap indigo">${icons.text}</div>
            <div>
              <div class="quick-action-title">Nhập văn bản</div>
              <div class="quick-action-sub">Từ văn bản</div>
            </div>
          </div>
          <div class="quick-action" onclick="navigate('import')">
            <div class="quick-action-icon-wrap teal">${icons.file}</div>
            <div>
              <div class="quick-action-title">Nhập file</div>
              <div class="quick-action-sub">PDF, TXT, Ảnh</div>
            </div>
          </div>
          <div class="quick-action" onclick="navigate('study-sets')">
            <div class="quick-action-icon-wrap green">${icons.school}</div>
            <div>
              <div class="quick-action-title">Bộ thẻ</div>
              <div class="quick-action-sub">Xem tất cả</div>
            </div>
          </div>
          <div class="quick-action" onclick="navigate('history')">
            <div class="quick-action-icon-wrap orange">${icons.clock}</div>
            <div>
              <div class="quick-action-title">Lịch sử</div>
              <div class="quick-action-sub">Xem kết quả quiz</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Recent Sets -->
      ${recentSets.length > 0 ? `
      <div>
        <div class="section-header">
          <div class="section-title">Bộ học gần đây</div>
          <button class="see-all-chip" onclick="navigate('study-sets')">
            Xem tất cả ${icons.chevronRight}
          </button>
        </div>
        <div class="recent-sets-scroll">
          ${recentSets.map(ss => `
          <div class="recent-set-card" onclick="openStudySetDetail(${ss.id})">
            <div class="recent-set-strip" style="background:${sourceColor(ss.sourceType)}"></div>
            <div class="recent-set-body">
              ${(ss.isPinned || ss.isFavorite) ? `
              <div style="display:flex;gap:4px;margin-bottom:4px">
                ${ss.isPinned ? '<span style="font-size:11px">📌</span>' : ''}
                ${ss.isFavorite ? '<span style="font-size:11px;color:#F59E0B">⭐</span>' : ''}
              </div>` : ''}
              <div class="recent-set-title">${escapeHtml(ss.title)}</div>
              ${ss.description ? `<div class="recent-set-desc">${escapeHtml(ss.description)}</div>` : ''}
              <div class="recent-set-footer">
                <div class="recent-set-cards">${ss.cardCount || 0} thẻ</div>
                <div class="recent-set-date">${formatDateShort(ss.updatedAt)}</div>
              </div>
            </div>
          </div>
          `).join('')}
        </div>
      </div>
      ` : `
      <div class="card" style="text-align:center;padding:32px">
        <div style="font-size:40px;margin-bottom:12px;opacity:0.5">📚</div>
        <div style="font-size:15px;font-weight:700;color:#1F2937;margin-bottom:4px">Chưa có bộ thẻ nào</div>
        <div style="font-size:13px;color:#6B7280;margin-bottom:16px">Tạo bộ thẻ đầu tiên để bắt đầu học</div>
        <button class="btn btn-primary btn-sm" onclick="showQuickImportModal()">
          ${icons.plus} Tạo bộ thẻ
        </button>
      </div>
      `}

      ${recentHistory.length > 0 ? `
      <div>
        <div class="section-header">
          <div class="section-title">Quiz gần đây</div>
          <button class="see-all-chip" onclick="navigate('history')">
            Xem tất cả ${icons.chevronRight}
          </button>
        </div>
        <div style="display:flex;flex-direction:column;gap:8px">
          ${recentHistory.map(h => {
            const cls = h.scorePercent >= 80 ? 'excellent' : h.scorePercent >= 50 ? 'medium' : 'low';
            return `
            <div class="history-item">
              <div class="history-score ${cls}">${h.scorePercent}%</div>
              <div class="history-info">
                <div class="history-name">${escapeHtml(h.fileName)}</div>
                <div class="history-date">${h.correctCount}/${h.totalQuestions} đúng · ${formatDate(h.createdAt)}</div>
              </div>
            </div>`;
          }).join('')}
        </div>
      </div>
      ` : ''}

    </div>

    ${bottomNav()}
    ${quickImportModal()}
    ${quickTextModal()}
    ${dailyGoalModal()}
  </div>`;
}

// ============================================================
// MODALS
// ============================================================
function quickImportModal() {
  return `
  <div id="quickImportModal" class="modal-overlay hidden" onclick="closeQuickImportModal(event)">
    <div class="modal" onclick="event.stopPropagation()">
      <div class="modal-title">Tạo bộ học mới</div>
      <div style="display:flex;flex-direction:column;gap:12px">
        <div class="quick-action" onclick="closeQuickImport();navigate('quick-text')">
          <div class="quick-action-icon-wrap indigo">${icons.text}</div>
          <div>
            <div class="quick-action-title">Từ văn bản</div>
            <div class="quick-action-sub">Nhập trực tiếp vào ô</div>
          </div>
          ${icons.chevronRight}
        </div>
        <div class="quick-action" onclick="closeQuickImport();navigate('import')">
          <div class="quick-action-icon-wrap teal">${icons.file}</div>
          <div>
            <div class="quick-action-title">Từ file</div>
            <div class="quick-action-sub">PDF, TXT, JPG, PNG</div>
          </div>
          ${icons.chevronRight}
        </div>
      </div>
      <div style="margin-top:16px;text-align:center">
        <button class="btn btn-ghost btn-sm" onclick="closeQuickImport()">Hủy</button>
      </div>
    </div>
  </div>`;
}

function quickTextModal() {
  return `
  <div id="quickTextModal" class="modal-overlay hidden" onclick="closeQuickTextModal(event)">
    <div class="modal" onclick="event.stopPropagation()">
      <div class="modal-title">Nhập văn bản nhanh</div>
      <div class="input-group" style="margin-bottom:12px">
        <label>Tiêu đề</label>
        <input type="text" class="input" id="qtTitle" placeholder="Ví dụ: Chương 1 - Sinh học tế bào">
      </div>
      <div class="input-group" style="margin-bottom:16px">
        <label>Nội dung</label>
        <textarea class="input" id="qtContent" rows="8" placeholder="Dán nội dung bài học, tài liệu, hoặc ghi chú vào đây..."></textarea>
      </div>
      <button class="btn btn-primary" style="width:100%" onclick="submitQuickTextModal()">
        Tạo Quiz
      </button>
      <div style="margin-top:8px;text-align:center">
        <button class="btn btn-ghost btn-sm" onclick="closeQuickTextModal()">Hủy</button>
      </div>
    </div>
  </div>`;
}

function dailyGoalModal() {
  const goals = [5, 10, 15, 20, 30, 50];
  return `
  <div id="dailyGoalModal" class="modal-overlay hidden" onclick="closeDailyGoalModal(event)">
    <div class="modal" onclick="event.stopPropagation()">
      <div class="modal-title">Mục tiêu hàng ngày</div>
      <div style="font-size:14px;color:#6B7280;margin-bottom:16px;text-align:center">
        Bạn muốn ôn bao nhiêu thẻ mỗi ngày?
      </div>
      <div style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:16px">
        ${goals.map(g => `
        <button class="chip ${state.dailyGoalTarget === g ? 'active' : ''}" onclick="setDailyGoal(${g})">${g} thẻ</button>
        `).join('')}
      </div>
      <div style="text-align:center">
        <button class="btn btn-ghost btn-sm" onclick="closeDailyGoalModal()">Đóng</button>
      </div>
    </div>
  </div>`;
}

function showQuickImportModal() {
  $('#quickImportModal')?.classList.remove('hidden');
}

function closeQuickImportModal(e) {
  if (e && e.target !== e.currentTarget) return;
  $('#quickImportModal')?.classList.add('hidden');
}

function closeQuickImport() {
  $('#quickImportModal')?.classList.add('hidden');
}

function showQuickTextModal() {
  closeQuickImport();
  $('#quickTextModal')?.classList.remove('hidden');
}

function closeQuickTextModal(e) {
  if (e && e.target !== e.currentTarget) return;
  $('#quickTextModal')?.classList.add('hidden');
}

async function submitQuickTextModal() {
  const title = $('#qtTitle')?.value.trim();
  const content = $('#qtContent')?.value.trim();
  if (!content || content.length < 50) {
    showToast('Nội dung quá ngắn (cần ít nhất 50 ký tự)', 'error');
    return;
  }
  closeQuickTextModal();
  const cleaned = cleanText(content);
  state.extractedContent = {
    fileName: title || 'Văn bản nhanh',
    mimeType: 'text/plain',
    rawText: content,
    cleanedText: cleaned,
    charCount: cleaned.length,
    status: 'ready'
  };
  navigate('quiz-config');
}

function showDailyGoalModal() {
  $('#dailyGoalModal')?.classList.remove('hidden');
}

function closeDailyGoalModal(e) {
  if (e && e.target !== e.currentTarget) return;
  $('#dailyGoalModal')?.classList.add('hidden');
}

async function setDailyGoal(goal) {
  state.dailyGoalTarget = goal;
  await dbPut('user_data', { key: 'dailyGoalTarget', value: goal });
  $$('.chip').forEach(c => c.classList.remove('active'));
  event.target.classList.add('active');
  setTimeout(closeDailyGoalModal, 300);
}

// ============================================================
// BOTTOM NAV
// ============================================================
function bottomNav() {
  const studySetScreens = ['study-sets','study-set-detail','flashcard','learn','learn-result','test-config','test','test-result','review-wrong','smart-review','smart-review-config'];
  return `
  <nav class="bottom-nav">
    <button class="nav-item ${state.currentScreen === 'home' ? 'active' : ''}" onclick="navigate('home')">
      ${icons.home}
      <span>Trang chủ</span>
    </button>
    <button class="nav-item ${studySetScreens.includes(state.currentScreen) ? 'active' : ''}" onclick="navigate('study-sets')">
      ${icons.book}
      <span>Học tập</span>
    </button>
    <button class="nav-item ${state.currentScreen === 'history' ? 'active' : ''}" onclick="navigate('history')">
      ${icons.clock}
      <span>Lịch sử</span>
    </button>
  </nav>`;
}

// ============================================================
// SCREEN: IMPORT
// ============================================================
function renderImport() {
  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('home')">${icons.back}</button>
      <h1>Nhập file</h1>
    </div>
    <div class="screen-content">
      <p style="font-size:14px;color:#6B7280;margin-bottom:16px">
        Hỗ trợ file <strong>TXT</strong>, <strong>PDF</strong>, <strong>JPG</strong>, <strong>PNG</strong>. Nội dung sẽ được trích xuất để tạo câu hỏi quiz.
      </p>
      <div class="drop-zone" id="dropZone" onclick="document.getElementById('fileInput').click()">
        <div class="drop-zone-icon">${icons.upload}</div>
        <h3>Nhấn hoặc kéo file vào đây</h3>
        <p>TXT, PDF, JPG, PNG · Tối đa 20MB</p>
        <input type="file" id="fileInput" class="hidden" accept=".txt,.pdf,.jpg,.jpeg,.png,.webp" onchange="handleFileSelect(event)">
      </div>
      <div class="divider"></div>
      <button class="btn btn-secondary btn-lg" style="width:100%" onclick="navigate('quick-text')">
        ${icons.text} Nhập văn bản trực tiếp
      </button>
    </div>
  </div>`;
}

// ============================================================
// FILE HANDLING
// ============================================================
function handleFileSelect(event) {
  const file = event.target.files[0];
  if (!file) return;
  processFile(file);
}

function handleFileDrop(event) {
  event.preventDefault();
  $('#dropZone').classList.remove('drag-over');
  const file = event.dataTransfer.files[0];
  if (file) processFile(file);
}

async function processFile(file) {
  const validTypes = ['text/plain', 'application/pdf', 'image/jpeg', 'image/png', 'image/webp'];
  if (!validTypes.includes(file.type)) {
    showToast('Định dạng file không được hỗ trợ', 'error');
    return;
  }
  if (file.size > 20 * 1024 * 1024) {
    showToast('File quá lớn (tối đa 20MB)', 'error');
    return;
  }

  state.extractedContent = { fileName: file.name, mimeType: file.type, rawText: '', status: 'loading' };
  navigate('processing');

  try {
    let text = '';
    if (file.type === 'text/plain') {
      text = await file.text();
    } else if (file.type.startsWith('image/')) {
      text = await extractTextFromImage(file);
    } else if (file.type === 'application/pdf') {
      text = await extractTextFromPDF(file);
    }

    const cleaned = cleanText(text);
    state.extractedContent = {
      fileName: file.name,
      mimeType: file.type,
      rawText: text,
      cleanedText: cleaned,
      charCount: cleaned.length,
      status: 'ready'
    };
    navigate('quiz-config');
  } catch (err) {
    console.error(err);
    showToast('Lỗi khi đọc file: ' + err.message, 'error');
    navigate('import');
  }
}

async function extractTextFromImage(file) {
  return new Promise((resolve, reject) => {
    const img = new Image();
    const url = URL.createObjectURL(file);
    img.onload = async () => {
      const canvas = document.createElement('canvas');
      canvas.width = img.width;
      canvas.height = img.height;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(img, 0, 0);

      if (!window.Tesseract) {
        const script = document.createElement('script');
        script.src = 'https://cdn.jsdelivr.net/npm/tesseract.js@5/dist/tesseract.min.js';
        script.onload = async () => {
          URL.revokeObjectURL(url);
          try {
            const result = await Tesseract.recognize(canvas, 'vie+eng', { logger: () => {} });
            resolve(result.data.text);
          } catch (e) { reject(e); }
        };
        script.onerror = () => { URL.revokeObjectURL(url); reject(new Error('Không thể tải OCR engine')); };
        document.head.appendChild(script);
      }
    };
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('Không thể đọc hình ảnh')); };
    img.src = url;
  });
}

async function extractTextFromPDF(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = async (e) => {
      try {
        const typedArray = new Uint8Array(e.target.result);
        const pdfjs = window.pdfjsLib;
        if (!pdfjs) {
          const script = document.createElement('script');
          script.src = 'https://cdn.jsdelivr.net/npm/pdfjs-dist@3.11.174/build/pdf.min.js';
          script.onload = async () => {
            pdfjs.GlobalWorkerOptions.workerSrc = 'https://cdn.jsdelivr.net/npm/pdfjs-dist@3.11.174/build/pdf.worker.min.js';
            const pdf = await pdfjs.getDocument(typedArray).promise;
            let text = '';
            for (let i = 1; i <= pdf.numPages; i++) {
              const page = await pdf.getPage(i);
              const content = await page.getTextContent();
              text += content.items.map(item => item.str).join(' ') + '\n\n';
            }
            resolve(text);
          };
          script.onerror = () => reject(new Error('Không thể tải PDF reader'));
          document.head.appendChild(script);
        } else {
          const pdf = await pdfjs.getDocument(typedArray).promise;
          let text = '';
          for (let i = 1; i <= pdf.numPages; i++) {
            const page = await pdf.getPage(i);
            const content = await page.getTextContent();
            text += content.items.map(item => item.str).join(' ') + '\n\n';
          }
          resolve(text);
        }
      } catch (err) { reject(err); }
    };
    reader.onerror = () => reject(new Error('Lỗi đọc file PDF'));
    reader.readAsArrayBuffer(file);
  });
}

// ============================================================
// SCREEN: QUICK TEXT
// ============================================================
function renderQuickText() {
  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('home')">${icons.back}</button>
      <h1>Nhập văn bản</h1>
    </div>
    <div class="screen-content">
      <p style="font-size:14px;color:#6B7280;margin-bottom:16px">
        Dán nội dung vào đây. App sẽ tự động tạo câu hỏi trắc nghiệm từ nội dung.
      </p>
      <div class="input-group" style="margin-bottom:16px">
        <label>Tiêu đề</label>
        <input type="text" class="input" id="quickTitle" placeholder="Ví dụ: Chương 1 - Sinh học tế bào">
      </div>
      <div class="input-group" style="margin-bottom:20px">
        <label>Nội dung</label>
        <textarea class="input" id="quickContent" rows="10" placeholder="Dán nội dung bài học, tài liệu, hoặc ghi chú vào đây..."></textarea>
      </div>
      <button class="btn btn-primary btn-lg" style="width:100%" onclick="submitQuickText()">
        Tạo Quiz
      </button>
    </div>
  </div>`;
}

async function submitQuickText() {
  const title = $('#quickTitle')?.value.trim();
  const content = $('#quickContent')?.value.trim();
  if (!content || content.length < 50) {
    showToast('Nội dung quá ngắn (cần ít nhất 50 ký tự)', 'error');
    return;
  }
  const cleaned = cleanText(content);
  state.extractedContent = {
    fileName: title || 'Văn bản nhanh',
    mimeType: 'text/plain',
    rawText: content,
    cleanedText: cleaned,
    charCount: cleaned.length,
    status: 'ready'
  };
  navigate('quiz-config');
}

// ============================================================
// SCREEN: QUIZ CONFIG
// ============================================================
function renderQuizConfig() {
  const ec = state.extractedContent;
  if (!ec) { navigate('home'); return ''; }
  const wordCount = ec.cleanedText.split(/\s+/).length;
  const estQuestions = Math.min(20, Math.max(5, Math.floor(wordCount / 30)));

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('home')">${icons.back}</button>
      <h1>Cấu hình Quiz</h1>
    </div>
    <div class="screen-content">
      <div class="quiz-config-card">
        <div class="quiz-config-file">File</div>
        <div class="quiz-config-name">${escapeHtml(ec.fileName)}</div>
        <div class="quiz-config-meta">${ec.charCount.toLocaleString()} ký tự · ~${wordCount} từ · ~${estQuestions} câu</div>
      </div>

      <div class="input-group" style="margin-bottom:20px">
        <label>Số câu hỏi: <strong id="qCountVal">${state.quizConfig.questionCount}</strong></label>
        <input type="range" id="qCount" min="5" max="${Math.min(20, estQuestions + 5)}" value="${state.quizConfig.questionCount}"
          oninput="document.getElementById('qCountVal').textContent = this.value; state.quizConfig.questionCount = parseInt(this.value)">
        <div style="display:flex;justify-content:space-between;font-size:12px;color:#9CA3AF">
          <span>5</span><span>${Math.min(20, estQuestions + 5)}</span>
        </div>
      </div>

      <div class="input-group" style="margin-bottom:20px">
        <label>Độ khó</label>
        <div class="chip-group">
          ${['Dễ', 'Trung bình', 'Khó'].map(d => `
          <button class="chip ${state.quizConfig.difficulty === d ? 'active' : ''}" onclick="selectDifficulty('${d}')">${d}</button>
          `).join('')}
        </div>
      </div>

      <button class="btn btn-primary btn-lg" style="width:100%;margin-bottom:12px" onclick="startQuiz()">
        Bắt đầu Quiz
      </button>
      <button class="btn btn-secondary" style="width:100%" onclick="saveAsStudySet()">
        ${icons.book} Lưu thành bộ thẻ học
      </button>
    </div>
  </div>`;
}

function selectDifficulty(d) {
  state.quizConfig.difficulty = d;
  $$('.chip').forEach(c => c.classList.remove('active'));
  event.target.classList.add('active');
}

async function startQuiz() {
  const ec = state.extractedContent;
  if (!ec || ec.charCount < 50) {
    showToast('Nội dung quá ngắn để tạo quiz', 'error');
    return;
  }
  showToast('Đang tạo quiz...', 'default', 5000);
  const { questions, warning } = generateQuiz(ec.cleanedText, state.quizConfig.questionCount, state.quizConfig.difficulty);
  if (questions.length === 0) {
    showToast(warning || 'Không tạo được quiz', 'error');
    return;
  }
  state.quizSession = { fileName: ec.fileName, questions, difficulty: state.quizConfig.difficulty, questionCount: questions.length };
  state.userAnswers = {};
  state.currentQuestionIndex = 0;
  state.flaggedQuestions = new Set();
  navigate('quiz');
}

async function saveAsStudySet() {
  const ec = state.extractedContent;
  if (!ec) return;
  const sentences = extractSentences(ec.cleanedText);
  const cards = [];
  for (const s of sentences.slice(0, 50)) {
    const words = s.split(/\s+/);
    if (words.length >= 4) {
      const mid = Math.floor(words.length / 2);
      const term = words.slice(0, mid).join(' ');
      const definition = words.slice(mid).join(' ');
      if (term.length > 5 && definition.length > 5) {
        cards.push({ studySetId: 0, term, definition, itemType: 'term_definition', masteryLevel: 0, timesReviewed: 0, timesCorrect: 0, isStarred: false, createdAt: Date.now() });
      }
    }
  }
  if (cards.length === 0) { showToast('Không tạo được thẻ từ nội dung này', 'error'); return; }

  const studySet = {
    title: ec.fileName, description: '', cardCount: cards.length,
    sourceType: 'file_import', sourceFileName: ec.fileName, studySetType: 'term_definition',
    isPinned: false, isFavorite: false, createdAt: Date.now(), updatedAt: Date.now(),
  };
  const ssId = await dbPut('study_sets', studySet);
  for (const card of cards) { card.studySetId = ssId; await dbPut('flashcards', card); }
  showToast(`Đã lưu ${cards.length} thẻ!`, 'success');
  navigate('study-sets');
}

// ============================================================
// SCREEN: QUIZ
// ============================================================
function renderQuiz() {
  const qs = state.quizSession;
  if (!qs) { navigate('home'); return ''; }
  const idx = state.currentQuestionIndex;
  const q = qs.questions[idx];
  const answered = state.userAnswers[idx] !== undefined;
  const answeredCount = Object.keys(state.userAnswers).length;
  const progress = ((idx + 1) / qs.questions.length) * 100;
  const letters = ['A', 'B', 'C', 'D'];

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="confirmExitQuiz()">${icons.back}</button>
      <div class="quiz-header-progress">
        <div class="quiz-header-count">Câu ${idx + 1}/${qs.questions.length}</div>
        <div class="progress-bar" style="margin-top:6px;height:4px">
          <div class="progress-bar-fill" style="width:${progress}%"></div>
        </div>
      </div>
      <button class="btn btn-icon" onclick="toggleFlagQuestion()" title="Đánh dấu"
        style="${state.flaggedQuestions.has(idx) ? 'border-color:#8B5CF6;color:#8B5CF6' : ''}">
        ${state.flaggedQuestions.has(idx) ? icons.flag : icons.flag}
      </button>
    </div>
    <div class="screen-content">
      <div class="question-card">
        <div class="question-number">Câu hỏi ${idx + 1}</div>
        <div class="question-text">${escapeHtml(q.question)}</div>
      </div>

      <div class="quiz-options">
        ${q.options.map((opt, i) => {
          const isSelected = state.userAnswers[idx] === i;
          return `
          <button class="quiz-option ${isSelected ? 'selected' : ''}" onclick="selectAnswer(${i})">
            <div class="quiz-option-letter">${letters[i]}</div>
            <div class="quiz-option-text">${escapeHtml(opt)}</div>
          </button>`;
        }).join('')}
      </div>

      <div class="palette" style="justify-content:center;margin-top:12px">
        ${qs.questions.map((_, i) => {
          const isAnswered = state.userAnswers[i] !== undefined;
          const isFlagged = state.flaggedQuestions.has(i);
          return `<button class="palette-item ${i === idx ? 'current' : ''} ${isAnswered ? 'answered' : ''} ${isFlagged ? 'flagged' : ''}"
            onclick="jumpToQuestion(${i})">${i + 1}</button>`;
        }).join('')}
      </div>

      ${answeredCount === qs.questions.length ? `
      <button class="btn btn-primary btn-lg" style="width:100%;margin-top:16px" onclick="submitQuiz()">
        Nộp bài
      </button>
      ` : `
      <button class="btn btn-primary btn-lg" style="width:100%;margin-top:16px" onclick="nextQuestion()">
        ${idx < qs.questions.length - 1 ? 'Câu tiếp theo' : 'Xem kết quả'}
      </button>
      `}
    </div>
  </div>`;
}

function selectAnswer(optIndex) {
  state.userAnswers[state.currentQuestionIndex] = optIndex;
  renderQuiz();
}

function nextQuestion() {
  if (state.currentQuestionIndex < state.quizSession.questions.length - 1) {
    state.currentQuestionIndex++;
    navigate('quiz');
  } else {
    submitQuiz();
  }
}

function toggleFlagQuestion() {
  const idx = state.currentQuestionIndex;
  if (state.flaggedQuestions.has(idx)) state.flaggedQuestions.delete(idx);
  else state.flaggedQuestions.add(idx);
  renderQuiz();
}

function jumpToQuestion(idx) {
  state.currentQuestionIndex = idx;
  navigate('quiz');
}

function confirmExitQuiz() {
  if (confirm('Thoát quiz? Tiến độ sẽ bị mất.')) navigate('home');
}

async function submitQuiz() {
  const qs = state.quizSession;
  const correctCount = qs.questions.filter((q, i) => state.userAnswers[i] === q.correctAnswerIndex).length;
  const scorePercent = Math.round((correctCount / qs.questions.length) * 100);

  const history = {
    fileName: qs.fileName, scorePercent, correctCount, totalQuestions: qs.questions.length,
    difficulty: qs.difficulty, questionType: 'Trắc nghiệm 4 đáp án', createdAt: Date.now(),
  };
  await dbPut('quiz_history', history);

  // Award XP
  const xpEarned = Math.round(correctCount * 10 + (scorePercent >= 80 ? 20 : 0));
  await addXp(xpEarned);
  showToast(`+${xpEarned} XP`, 'success', 2000);

  navigate('result');
}

// ============================================================
// SCREEN: RESULT
// ============================================================
function renderResult() {
  const qs = state.quizSession;
  if (!qs) { navigate('home'); return ''; }
  const correctCount = qs.questions.filter((q, i) => state.userAnswers[i] === q.correctAnswerIndex).length;
  const scorePercent = Math.round((correctCount / qs.questions.length) * 100);

  let emoji, message;
  if (scorePercent >= 90) { emoji = '🏆'; message = 'Xuất sắc! Bạn nắm vững kiến thức rồi!'; }
  else if (scorePercent >= 70) { emoji = '🎉'; message = 'Tốt lắm! Tiếp tục phát huy nhé!'; }
  else if (scorePercent >= 50) { emoji = '💪'; message = 'Khá ổn đấy! Cần ôn tập thêm một chút.'; }
  else if (scorePercent >= 30) { emoji = '📚'; message = 'Cần học kỹ hơn. Đừng nản lòng!'; }
  else { emoji = '🤔'; message = 'Thử lại lần nữa nhé. Kiến thức cần thời gian.'; }

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('home')">${icons.back}</button>
      <h1>Kết quả</h1>
    </div>
    <div class="screen-content">
      <div class="result-hero">
        <div class="result-emoji">${emoji}</div>
        <div class="result-score">${scorePercent}%</div>
        <div class="result-label">${correctCount}/${qs.questions.length} câu đúng</div>
        <div class="result-message">${message}</div>
      </div>

      <div class="card" style="margin-bottom:16px">
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;text-align:center">
          <div>
            <div style="font-size:24px;font-weight:800;color:#22C55E">${correctCount}</div>
            <div style="font-size:12px;color:#6B7280">Đúng</div>
          </div>
          <div>
            <div style="font-size:24px;font-weight:800;color:#EF4444">${qs.questions.length - correctCount}</div>
            <div style="font-size:12px;color:#6B7280">Sai</div>
          </div>
        </div>
      </div>

      <div class="tabs">
        <button class="tab active" onclick="switchResultTab('review', this)">Đáp án</button>
        <button class="tab" onclick="switchResultTab('detail', this)">Chi tiết</button>
      </div>

      <div id="reviewTab">
        ${qs.questions.map((q, i) => {
          const userAns = state.userAnswers[i];
          const isCorrect = userAns === q.correctAnswerIndex;
          return `
          <div class="card" style="margin-bottom:12px;padding:16px">
            <div style="display:flex;align-items:flex-start;gap:10px;margin-bottom:10px">
              <div style="width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;flex-shrink:0;${isCorrect ? 'background:#EAFBF0;color:#22C55E' : 'background:#FEECEC;color:#EF4444'}">
                ${isCorrect ? icons.check : icons.x}
              </div>
              <div style="font-size:14px;font-weight:600;flex:1;color:#1F2937">${escapeHtml(q.question)}</div>
            </div>
            ${q.options.map((opt, oi) => {
              const isUser = userAns === oi;
              const isCorrectOpt = q.correctAnswerIndex === oi;
              let style = 'font-size:13px;color:#6B7280;padding:4px 0;';
              if (isCorrectOpt) style = 'font-size:13px;color:#22C55E;font-weight:600;padding:4px 0;';
              if (isUser && !isCorrectOpt) style = 'font-size:13px;color:#EF4444;font-weight:600;padding:4px 0;';
              return `<div style="${style}">${['A','B','C','D'][oi]}. ${escapeHtml(opt)} ${isCorrectOpt ? '✓' : ''} ${isUser && !isCorrectOpt ? '(Bạn chọn)' : ''}</div>`;
            }).join('')}
            <div style="margin-top:8px;font-size:12px;color:#9CA3AF;font-style:italic;padding:8px;background:#F6F8FC;border-radius:8px">
              📝 ${escapeHtml(q.explanation.substring(0, 100))}${q.explanation.length > 100 ? '...' : ''}
            </div>
          </div>`;
        }).join('')}
      </div>

      <div id="detailTab" class="hidden">
        <div class="card">
          <div style="font-size:14px;color:#6B7280;line-height:2">
            <p><strong style="color:#1F2937">File:</strong> ${escapeHtml(qs.fileName)}</p>
            <p><strong style="color:#1F2937">Độ khó:</strong> ${qs.difficulty}</p>
            <p><strong style="color:#1F2937">Số câu:</strong> ${qs.questionCount}</p>
          </div>
        </div>
      </div>

      <div class="result-actions" style="margin-top:16px">
        <button class="btn btn-primary btn-lg" style="width:100%" onclick="navigate('home')">
          ${icons.home} Về trang chủ
        </button>
        <button class="btn btn-secondary" style="width:100%" onclick="retryQuizFromResult()">
          ${icons.refresh} Làm lại
        </button>
      </div>
    </div>
  </div>`;
}

function switchResultTab(tab, btn) {
  $$('.tab').forEach(t => t.classList.remove('active'));
  btn.classList.add('active');
  $('#reviewTab')?.classList.toggle('hidden', tab !== 'review');
  $('#detailTab')?.classList.toggle('hidden', tab !== 'detail');
}

function retryQuizFromResult() {
  if (state.quizSession) {
    state.userAnswers = {};
    state.currentQuestionIndex = 0;
    state.flaggedQuestions = new Set();
    navigate('quiz');
  }
}

// ============================================================
// SCREEN: HISTORY
// ============================================================
async function renderHistory() {
  const history = (await dbGetAll('quiz_history')).sort((a, b) => b.createdAt - a.createdAt);
  const totalQuizzes = history.length;
  const totalCorrect = history.reduce((s, h) => s + h.correctCount, 0);
  const totalQuestions = history.reduce((s, h) => s + h.totalQuestions, 0);
  const avgScore = totalQuestions > 0 ? Math.round((totalCorrect / totalQuestions) * 100) : 0;

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('home')">${icons.back}</button>
      <h1>Lịch sử Quiz</h1>
      ${history.length > 0 ? `<button class="btn btn-ghost btn-sm" onclick="clearHistory()">${icons.trash} Xóa</button>` : ''}
    </div>
    <div class="screen-content">
      ${history.length > 0 ? `
      <div class="stats-row" style="margin-bottom:20px">
        <div class="stat-card">
          <div class="value">${totalQuizzes}</div>
          <div class="label">Quiz</div>
        </div>
        <div class="stat-card">
          <div class="value">${avgScore}%</div>
          <div class="label">Điểm TB</div>
        </div>
        <div class="stat-card">
          <div class="value">${totalCorrect}</div>
          <div class="label">Câu đúng</div>
        </div>
      </div>
      ` : ''}

      ${history.length === 0 ? `
      <div class="empty-state">
        <div class="empty-state-icon-wrap">${icons.clock}</div>
        <h3>Chưa có lịch sử</h3>
        <p>Làm quiz để xem kết quả ở đây.</p>
      </div>
      ` : history.map(h => {
        const cls = h.scorePercent >= 80 ? 'excellent' : h.scorePercent >= 50 ? 'medium' : 'low';
        return `
        <div class="history-item">
          <div class="history-score ${cls}">${h.scorePercent}%</div>
          <div class="history-info" style="flex:1">
            <div class="history-name">${escapeHtml(h.fileName)}</div>
            <div class="history-date">${h.correctCount}/${h.totalQuestions} đúng · ${h.difficulty} · ${formatDate(h.createdAt)}</div>
          </div>
          <button class="btn btn-icon" style="width:36px;height:36px;min-height:36px;padding:0" onclick="deleteHistoryItem(${h.id})">${icons.trash}</button>
        </div>`;
      }).join('')}
    </div>
    ${bottomNav()}
  </div>`;
}

async function clearHistory() {
  if (!confirm('Xóa toàn bộ lịch sử?')) return;
  await dbClear('quiz_history');
  showToast('Đã xóa lịch sử', 'success');
  navigate('history');
}

async function deleteHistoryItem(id) {
  await dbDelete('quiz_history', id);
  navigate('history');
}

// ============================================================
// SCREEN: STUDY SETS
// ============================================================
async function renderStudySets() {
  const studySets = (await dbGetAll('study_sets')).sort((a, b) => b.updatedAt - a.updatedAt);

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('home')">${icons.back}</button>
      <h1>Bộ thẻ học</h1>
      <button class="btn btn-icon" onclick="navigate('create-study-set')">${icons.plus}</button>
    </div>
    <div class="screen-content">
      <div class="search-bar" style="margin-bottom:16px">
        ${icons.search}
        <input type="text" id="searchInput" placeholder="Tìm kiếm bộ thẻ..." oninput="filterStudySets(this.value)">
      </div>

      <div id="studySetList">
        ${studySets.length === 0 ? `
        <div class="empty-state">
          <div class="empty-state-icon-wrap">${icons.book}</div>
          <h3>Chưa có bộ thẻ nào</h3>
          <p>Tạo bộ thẻ từ file hoặc nhập thủ công.</p>
          <button class="btn btn-primary" onclick="navigate('create-study-set')">${icons.plus} Tạo bộ thẻ</button>
        </div>
        ` : studySets.map(ss => renderStudySetItem(ss)).join('')}
      </div>
    </div>
    ${bottomNav()}
  </div>`;
}

function renderStudySetItem(ss) {
  const color = ss.sourceType === 'quick_import' ? '#22C55E' : ss.sourceType === 'file_import' ? '#3B82F6' : '#6B7280';
  return `
  <div class="study-set-item" onclick="openStudySetDetail(${ss.id})">
    <div class="study-set-icon">${icons.book}</div>
    <div class="study-set-info">
      <div class="study-set-title">${escapeHtml(ss.title)}</div>
      <div class="study-set-meta">${ss.cardCount || 0} thẻ · ${formatDate(ss.updatedAt)}</div>
    </div>
    ${icons.chevronRight}
  </div>`;
}

async function filterStudySets(query) {
  const studySets = await dbGetAll('study_sets');
  const filtered = studySets.filter(ss => ss.title.toLowerCase().includes(query.toLowerCase())).sort((a, b) => b.updatedAt - a.updatedAt);
  const list = $('#studySetList');
  if (list) {
    if (filtered.length === 0) {
      list.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon-wrap">${icons.search}</div>
        <h3>Không tìm thấy</h3>
        <p>Thử từ khóa khác.</p>
      </div>`;
    } else {
      list.innerHTML = filtered.map(ss => renderStudySetItem(ss)).join('');
    }
  }
}

async function openStudySetDetail(id) {
  const ss = await dbGet('study_sets', id);
  if (!ss) return;
  const cards = await dbGetByIndex('flashcards', 'studySetId', id);
  state.currentStudySet = ss;
  state.currentStudySetCards = cards;
  state.currentCardIndex = 0;
  navigate('study-set-detail');
}

// ============================================================
// SCREEN: STUDY SET DETAIL
// ============================================================
async function renderStudySetDetail() {
  const ss = state.currentStudySet;
  const cards = state.currentStudySetCards;
  if (!ss) { navigate('study-sets'); return ''; }

  const mastered = cards.filter(c => c.masteryLevel >= 4).length;
  const needsReview = cards.filter(c => c.masteryLevel < 4).length;
  const masteryPercent = cards.length > 0 ? Math.round((mastered / cards.length) * 100) : 0;

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('study-sets')">${icons.back}</button>
      <h1 style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${escapeHtml(ss.title)}</h1>
      <button class="btn btn-icon" onclick="deleteStudySet(${ss.id})">${icons.trash}</button>
    </div>
    <div class="screen-content">
      <div class="stats-row" style="margin-bottom:16px">
        <div class="stat-card">
          <div class="value">${cards.length}</div>
          <div class="label">Tổng thẻ</div>
        </div>
        <div class="stat-card">
          <div class="value" style="color:#22C55E">${mastered}</div>
          <div class="label">Đã thuộc</div>
        </div>
        <div class="stat-card">
          <div class="value" style="color:#F59E0B">${needsReview}</div>
          <div class="label">Cần ôn</div>
        </div>
      </div>

      ${cards.length > 0 ? `
      <div class="mastery-bar-wrap">
        <div class="progress-bar">
          <div class="progress-bar-fill" style="width:${masteryPercent}%"></div>
        </div>
        <div class="mastery-bar-label">
          <span>${mastered}/${cards.length} thẻ đã thuộc</span>
          <span>${masteryPercent}%</span>
        </div>
      </div>

      <div class="action-grid">
        <div class="action-card" onclick="startFlashcards()">
          <div class="action-card-icon">${icons.flashcard}</div>
          <div class="action-card-title">Flashcard</div>
        </div>
        <div class="action-card" onclick="startLearn()">
          <div class="action-card-icon">${icons.zap}</div>
          <div class="action-card-title">Học</div>
        </div>
        <div class="action-card" onclick="navigate('test-config')">
          <div class="action-card-icon">${icons.award}</div>
          <div class="action-card-title">Kiểm tra</div>
        </div>
        <div class="action-card" onclick="startSmartReviewForCurrentSet()">
          <div class="action-card-icon">${icons.brain}</div>
          <div class="action-card-title">Smart Review</div>
        </div>
      </div>

      <div>
        <div class="section-header">
          <div class="section-title">Xem trước thẻ</div>
        </div>
        ${cards.slice(0, 5).map(c => `
        <div class="card" style="margin-bottom:8px;padding:14px">
          <div style="font-size:13px;color:#6B7280;margin-bottom:4px">${escapeHtml(c.term)}</div>
          <div style="font-size:14px;font-weight:500;color:#1F2937">${escapeHtml(c.definition)}</div>
        </div>
        `).join('')}
        ${cards.length > 5 ? `<p style="font-size:13px;color:#9CA3AF;text-align:center;padding:8px">+${cards.length - 5} thẻ khác</p>` : ''}
      </div>
      ` : `
      <div class="empty-state">
        <div class="empty-state-icon-wrap">${icons.book}</div>
        <h3>Không có thẻ</h3>
        <p>Bộ thẻ này chưa có thẻ nào.</p>
      </div>
      `}
    </div>
  </div>`;
}

async function startSmartReviewForCurrentSet() {
  const needsReview = state.currentStudySetCards.filter(c => c.masteryLevel < 4);
  if (needsReview.length === 0) {
    showToast('Không có thẻ nào cần ôn!', 'success');
    return;
  }
  state.smartReviewCards = shuffle(needsReview).slice(0, 20);
  state.currentCardIndex = 0;
  state.currentFlashcardFlipped = false;
  navigate('smart-review');
}

async function deleteStudySet(id) {
  if (!confirm('Xóa bộ thẻ này?')) return;
  const cards = await dbGetByIndex('flashcards', 'studySetId', id);
  for (const card of cards) await dbDelete('flashcards', card.id);
  await dbDelete('study_sets', id);
  showToast('Đã xóa bộ thẻ', 'success');
  navigate('study-sets');
}

// ============================================================
// SCREEN: FLASHCARD
// ============================================================
function startFlashcards() {
  if (state.currentStudySetCards.length === 0) { showToast('Không có thẻ để học', 'error'); return; }
  state.currentCardIndex = 0;
  state.currentFlashcardFlipped = false;
  navigate('flashcard');
}

function renderFlashcard() {
  const cards = state.currentStudySetCards;
  if (cards.length === 0) { navigate('study-set-detail'); return ''; }
  const card = cards[state.currentCardIndex];
  const flipped = state.currentFlashcardFlipped;

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('study-set-detail')">${icons.back}</button>
      <div style="flex:1;text-align:center">
        <div style="font-size:13px;font-weight:700">${state.currentCardIndex + 1}/${cards.length}</div>
      </div>
      <button class="btn btn-icon" onclick="toggleFlashcardStar(${card.id})">
        ${card.isStarred ? icons.star : icons.starOutline}
      </button>
    </div>
    <div class="screen-content">
      <div class="flashcard-container" onclick="flipFlashcard()">
        <div class="flashcard ${flipped ? 'flipped' : ''}">
          <div class="flashcard-face">
            <div class="flashcard-label">Thuật ngữ</div>
            <div class="flashcard-content">${escapeHtml(card.term)}</div>
            <div class="flashcard-hint">Nhấn để lật</div>
          </div>
          <div class="flashcard-face flashcard-back">
            <div class="flashcard-label">Định nghĩa</div>
            <div class="flashcard-content">${escapeHtml(card.definition)}</div>
            <div class="flashcard-hint">Nhấn để lật lại</div>
          </div>
        </div>
      </div>

      <div class="progress-bar" style="margin:20px 0">
        <div class="progress-bar-fill" style="width:${((state.currentCardIndex + 1) / cards.length) * 100}%"></div>
      </div>

      <div style="display:flex;gap:12px">
        <button class="btn btn-secondary" style="flex:1" onclick="prevCard()" ${state.currentCardIndex === 0 ? 'disabled' : ''}>
          ${icons.back} Trước
        </button>
        <button class="btn btn-primary" style="flex:1" onclick="nextCard()">
          ${state.currentCardIndex === cards.length - 1 ? 'Xong' : 'Sau'} ${icons.chevronRight}
        </button>
      </div>
    </div>
  </div>`;
}

function flipFlashcard() {
  state.currentFlashcardFlipped = !state.currentFlashcardFlipped;
  $$('.flashcard')[0]?.classList.toggle('flipped', state.currentFlashcardFlipped);
}

function prevCard() {
  if (state.currentCardIndex > 0) {
    state.currentCardIndex--;
    state.currentFlashcardFlipped = false;
    navigate('flashcard');
  }
}

async function nextCard() {
  if (state.currentCardIndex < state.currentStudySetCards.length - 1) {
    state.currentCardIndex++;
    state.currentFlashcardFlipped = false;
    navigate('flashcard');
  } else {
    showToast('Đã xem hết tất cả thẻ!', 'success');
    navigate('study-set-detail');
  }
}

async function toggleFlashcardStar(cardId) {
  const card = state.currentStudySetCards.find(c => c.id === cardId);
  if (card) {
    card.isStarred = !card.isStarred;
    await dbPut('flashcards', card);
    renderFlashcard();
  }
}

// ============================================================
// SCREEN: LEARN
// ============================================================
function startLearn() {
  if (state.currentStudySetCards.length === 0) { showToast('Không có thẻ để học', 'error'); return; }
  const shuffled = shuffle([...state.currentStudySetCards]);
  state.learnSession = { cards: shuffled, current: 0, correctCount: 0, wrongCount: 0 };
  state.learnAnswers = {};
  navigate('learn');
}

function renderLearn() {
  const session = state.learnSession;
  if (!session) { navigate('study-set-detail'); return ''; }
  const card = session.cards[session.current];
  const total = session.cards.length;
  const idx = session.current;
  const letters = ['A', 'B', 'C', 'D'];

  const otherDefs = state.currentStudySetCards.filter(c => c.id !== card.id).map(c => c.definition);
  const options = shuffle([card.definition, ...shuffle(otherDefs).slice(0, 3)]);

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="confirmExitLearn()">${icons.back}</button>
      <div class="quiz-header-progress">
        <div class="quiz-header-count">Câu ${idx + 1}/${total}</div>
        <div class="progress-bar" style="margin-top:6px;height:4px">
          <div class="progress-bar-fill" style="width:${((idx + 1) / total) * 100}%"></div>
        </div>
      </div>
      <div class="quiz-header-score">
        <span class="quiz-score-correct">✓ ${session.correctCount}</span>
        <span class="quiz-score-wrong">✗ ${session.wrongCount}</span>
      </div>
    </div>
    <div class="screen-content">
      <div class="question-card">
        <div class="question-number">Học</div>
        <div class="question-text">${escapeHtml(card.term)}</div>
      </div>

      <div class="quiz-options">
        ${options.map((def, i) => `
        <button class="quiz-option" onclick="answerLearn('${escapeHtml(def).replace(/'/g, "\\'")}', '${escapeHtml(card.definition).replace(/'/g, "\\'")}')">
          <div class="quiz-option-letter">${letters[i]}</div>
          <div class="quiz-option-text">${escapeHtml(def)}</div>
        </button>
        `).join('')}
      </div>
    </div>
  </div>`;
}

async function answerLearn(selected, correct) {
  const session = state.learnSession;
  const isCorrect = selected.trim() === correct.trim();
  if (isCorrect) session.correctCount++; else session.wrongCount++;

  const card = session.cards[session.current];
  card.timesReviewed = (card.timesReviewed || 0) + 1;
  if (isCorrect) card.timesCorrect = (card.timesCorrect || 0) + 1;
  card.masteryLevel = Math.min(5, Math.max(0, card.masteryLevel + (isCorrect ? 1 : -1)));
  card.lastReviewedAt = Date.now();
  await dbPut('flashcards', card);
  await incrementDailyCards();

  showToast(isCorrect ? 'Đúng! ✓' : 'Sai. Đáp án: ' + correct.substring(0, 50), isCorrect ? 'success' : 'error', 2000);

  setTimeout(() => {
    if (session.current < session.cards.length - 1) {
      session.current++;
      navigate('learn');
    } else {
      navigate('learn-result');
    }
  }, isCorrect ? 800 : 1500);
}

function confirmExitLearn() {
  if (confirm('Thoát phiên học?')) navigate('study-set-detail');
}

// ============================================================
// SCREEN: LEARN RESULT
// ============================================================
function renderLearnResult() {
  const session = state.learnSession;
  if (!session) { navigate('study-set-detail'); return ''; }
  const total = session.cards.length;
  const correct = session.correctCount;
  const pct = Math.round((correct / total) * 100);

  let emoji, msg;
  if (pct >= 90) { emoji = '🏆'; msg = 'Xuất sắc!'; }
  else if (pct >= 70) { emoji = '🎉'; msg = 'Tốt lắm!'; }
  else if (pct >= 50) { emoji = '💪'; msg = 'Khá ổn!'; }
  else { emoji = '📚'; msg = 'Cần ôn thêm!'; }

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('study-set-detail')">${icons.back}</button>
      <h1>Hoàn thành</h1>
    </div>
    <div class="screen-content">
      <div class="result-hero">
        <div class="result-emoji">${emoji}</div>
        <div class="result-score">${pct}%</div>
        <div class="result-label">${correct}/${total} câu đúng</div>
        <div class="result-message">${msg}</div>
      </div>
      <button class="btn btn-primary btn-lg" style="width:100%;margin-bottom:12px" onclick="startLearn()">
        ${icons.refresh} Học lại
      </button>
      <button class="btn btn-secondary" style="width:100%" onclick="navigate('study-set-detail')">
        Về bộ thẻ
      </button>
    </div>
  </div>`;
}

// ============================================================
// SCREEN: TEST CONFIG
// ============================================================
function renderTestConfig() {
  const cards = state.currentStudySetCards;
  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('study-set-detail')">${icons.back}</button>
      <h1>Kiểm tra</h1>
    </div>
    <div class="screen-content">
      <div class="quiz-config-card">
        <div class="quiz-config-file">Bộ thẻ</div>
        <div class="quiz-config-name">${escapeHtml(state.currentStudySet?.title || '')}</div>
        <div class="quiz-config-meta">${cards.length} thẻ</div>
      </div>

      <div class="input-group" style="margin-bottom:20px">
        <label>Số câu hỏi: <strong id="testQCount">${Math.min(10, cards.length)}</strong></label>
        <input type="range" id="testQCountRange" min="5" max="${cards.length || 5}" value="${Math.min(10, cards.length || 5)}"
          oninput="document.getElementById('testQCount').textContent = this.value">
      </div>

      <div class="input-group" style="margin-bottom:20px">
        <label>Nguồn câu hỏi</label>
        <div class="chip-group">
          <button class="chip active" onclick="selectTestSource('all', this)">Tất cả</button>
          <button class="chip" onclick="selectTestSource('needs_review', this)">Cần ôn (${cards.filter(c => c.masteryLevel < 4).length})</button>
          <button class="chip" onclick="selectTestSource('starred', this)">Đã ghim (${cards.filter(c => c.isStarred).length})</button>
        </div>
      </div>

      <button class="btn btn-primary btn-lg" style="width:100%" onclick="startTest()">
        Bắt đầu kiểm tra
      </button>
    </div>
  </div>`;
}

function selectTestSource(source, btn) {
  state.testConfigSource = source;
  $$('.chip').forEach(c => c.classList.remove('active'));
  btn.classList.add('active');
}

function startTest() {
  const cards = state.currentStudySetCards;
  const count = parseInt($('#testQCountRange')?.value || 10);
  const source = state.testConfigSource || 'all';

  let pool = cards;
  if (source === 'needs_review') pool = cards.filter(c => c.masteryLevel < 4);
  else if (source === 'starred') pool = cards.filter(c => c.isStarred);
  if (pool.length === 0) pool = cards;
  pool = shuffle(pool);

  const selected = pool.slice(0, Math.min(count, pool.length));
  state.testSession = { cards: selected, current: 0, startTime: Date.now() };
  state.testAnswers = {};
  state.flaggedQuestions = new Set();
  navigate('test');
}

function renderTest() {
  const session = state.testSession;
  if (!session) { navigate('study-set-detail'); return ''; }
  const card = session.cards[session.current];
  const total = session.cards.length;
  const idx = session.current;
  const answered = state.testAnswers[idx] !== undefined;
  const progress = ((idx + 1) / total) * 100;
  const letters = ['A', 'B', 'C', 'D'];

  const otherDefs = state.currentStudySetCards.filter(c => c.id !== card.id).map(c => c.definition);
  const options = shuffle([card.definition, ...shuffle(otherDefs).slice(0, 3)]);

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="confirmExitTest()">${icons.back}</button>
      <div class="quiz-header-progress">
        <div class="quiz-header-count">Câu ${idx + 1}/${total}</div>
        <div class="progress-bar" style="margin-top:6px;height:4px">
          <div class="progress-bar-fill" style="width:${progress}%"></div>
        </div>
      </div>
      <button class="btn btn-icon" onclick="toggleTestFlag()" style="${state.flaggedQuestions.has(idx) ? 'border-color:#8B5CF6;color:#8B5CF6' : ''}">
        ${icons.flag}
      </button>
    </div>
    <div class="screen-content">
      <div class="question-card">
        <div class="question-number">Kiểm tra</div>
        <div class="question-text">${escapeHtml(card.term)}</div>
      </div>

      <div class="quiz-options">
        ${options.map((opt, i) => {
          const isSelected = state.testAnswers[idx] === i;
          return `
          <button class="quiz-option ${isSelected ? 'selected' : ''}" onclick="selectTestAnswer(${i})">
            <div class="quiz-option-letter">${letters[i]}</div>
            <div class="quiz-option-text">${escapeHtml(opt)}</div>
          </button>`;
        }).join('')}
      </div>

      <div class="palette" style="justify-content:center;margin-top:12px">
        ${session.cards.map((_, i) => {
          const isAnswered = state.testAnswers[i] !== undefined;
          const isFlagged = state.flaggedQuestions.has(i);
          return `<button class="palette-item ${i === idx ? 'current' : ''} ${isAnswered ? 'answered' : ''} ${isFlagged ? 'flagged' : ''}"
            onclick="jumpToTest(${i})">${i + 1}</button>`;
        }).join('')}
      </div>

      ${Object.keys(state.testAnswers).length === total ? `
      <button class="btn btn-primary btn-lg" style="width:100%;margin-top:16px" onclick="submitTest()">
        Nộp bài
      </button>
      ` : `
      <button class="btn btn-primary btn-lg" style="width:100%;margin-top:16px" onclick="nextTest()">
        ${idx < total - 1 ? 'Câu tiếp' : 'Xem kết quả'}
      </button>
      `}
    </div>
  </div>`;
}

async function selectTestAnswer(optIdx) {
  state.testAnswers[state.testSession.current] = optIdx;
  const card = state.testSession.cards[state.testSession.current];
  const otherDefs = state.currentStudySetCards.filter(c => c.id !== card.id).map(c => c.definition);
  const options = shuffle([card.definition, ...shuffle(otherDefs).slice(0, 3)]);
  const isCorrect = options[optIdx] === card.definition;
  card.timesReviewed = (card.timesReviewed || 0) + 1;
  if (isCorrect) card.timesCorrect = (card.timesCorrect || 0) + 1;
  card.masteryLevel = Math.min(5, Math.max(0, card.masteryLevel + (isCorrect ? 1 : -1)));
  card.lastReviewedAt = Date.now();
  await dbPut('flashcards', card);
  await incrementDailyCards();
  renderTest();
}

function nextTest() {
  if (state.testSession.current < state.testSession.cards.length - 1) {
    state.testSession.current++;
    navigate('test');
  } else {
    submitTest();
  }
}

function jumpToTest(idx) {
  state.testSession.current = idx;
  navigate('test');
}

function toggleTestFlag() {
  const idx = state.testSession.current;
  if (state.flaggedQuestions.has(idx)) state.flaggedQuestions.delete(idx);
  else state.flaggedQuestions.add(idx);
  renderTest();
}

function confirmExitTest() {
  if (confirm('Thoát bài kiểm tra?')) navigate('study-set-detail');
}

async function submitTest() {
  const session = state.testSession;
  const otherDefsMap = {};
  session.cards.forEach(card => {
    otherDefsMap[card.id] = shuffle([card.definition, ...shuffle(state.currentStudySetCards.filter(c => c.id !== card.id).map(c => c.definition)).slice(0, 3)]);
  });

  let correct = 0;
  const wrongCards = [];
  session.cards.forEach((card, i) => {
    const ans = state.testAnswers[i];
    if (ans !== undefined && otherDefsMap[card.id][ans] === card.definition) {
      correct++;
    } else {
      wrongCards.push(card);
    }
  });

  state.wrongCards = wrongCards;
  const pct = Math.round((correct / session.cards.length) * 100);
  const elapsed = Math.round((Date.now() - session.startTime) / 1000);
  const mins = Math.floor(elapsed / 60);
  const secs = elapsed % 60;

  const xpEarned = Math.round(correct * 10 + (pct >= 80 ? 20 : 0));
  await addXp(xpEarned);
  showToast(`+${xpEarned} XP`, 'success', 2000);

  state.testResult = { correct, total: session.cards.length, pct, mins, secs };
  navigate('test-result');
}

// ============================================================
// SCREEN: TEST RESULT
// ============================================================
function renderTestResult() {
  const r = state.testResult;
  if (!r) { navigate('study-set-detail'); return ''; }

  let emoji, msg;
  if (r.pct >= 90) { emoji = '🏆'; msg = 'Xuất sắc!'; }
  else if (r.pct >= 70) { emoji = '🎉'; msg = 'Tốt lắm!'; }
  else if (r.pct >= 50) { emoji = '💪'; msg = 'Khá ổn!'; }
  else { emoji = '📚'; msg = 'Cần ôn thêm!'; }

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('study-set-detail')">${icons.back}</button>
      <h1>Kết quả</h1>
    </div>
    <div class="screen-content">
      <div class="result-hero">
        <div class="result-emoji">${emoji}</div>
        <div class="result-score">${r.pct}%</div>
        <div class="result-label">${r.correct}/${r.total} câu đúng</div>
        <div class="result-message">${msg}</div>
        <div style="margin-top:8px;font-size:13px;color:rgba(255,255,255,0.6)">Thời gian: ${r.mins > 0 ? r.mins + 'p ' : ''}${r.secs}s</div>
      </div>

      <div class="stats-row" style="margin-bottom:20px">
        <div class="stat-card">
          <div class="value" style="color:#22C55E">${r.correct}</div>
          <div class="label">Đúng</div>
        </div>
        <div class="stat-card">
          <div class="value" style="color:#EF4444">${r.total - r.correct}</div>
          <div class="label">Sai</div>
        </div>
        <div class="stat-card">
          <div class="value">${r.mins > 0 ? r.mins + 'p ' : ''}${r.secs}s</div>
          <div class="label">Thời gian</div>
        </div>
      </div>

      ${state.wrongCards && state.wrongCards.length > 0 ? `
      <button class="btn btn-secondary" style="width:100%;margin-bottom:12px" onclick="startReviewWrong()">
        ${icons.refresh} Ôn lại ${state.wrongCards.length} câu sai
      </button>
      ` : ''}

      <button class="btn btn-primary btn-lg" style="width:100%;margin-bottom:12px" onclick="startTest()">
        ${icons.refresh} Làm lại
      </button>
      <button class="btn btn-secondary" style="width:100%" onclick="navigate('study-set-detail')">
        Về bộ thẻ
      </button>
    </div>
  </div>`;
}

function startReviewWrong() {
  if (!state.wrongCards || state.wrongCards.length === 0) return;
  state.smartReviewCards = state.wrongCards;
  state.currentCardIndex = 0;
  state.currentFlashcardFlipped = false;
  navigate('review-wrong');
}

// ============================================================
// SCREEN: REVIEW WRONG
// ============================================================
function renderReviewWrong() {
  const cards = state.smartReviewCards;
  if (!cards || cards.length === 0) { navigate('test-result'); return ''; }
  const card = cards[state.currentCardIndex];
  const flipped = state.currentFlashcardFlipped;

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('test-result')">${icons.back}</button>
      <div style="flex:1;text-align:center">
        <div style="font-size:13px;font-weight:700">${state.currentCardIndex + 1}/${cards.length}</div>
      </div>
    </div>
    <div class="screen-content">
      <div class="flashcard-container" onclick="flipReviewCard()">
        <div class="flashcard ${flipped ? 'flipped' : ''}">
          <div class="flashcard-face">
            <div class="flashcard-label">Thuật ngữ</div>
            <div class="flashcard-content">${escapeHtml(card.term)}</div>
          </div>
          <div class="flashcard-face flashcard-back">
            <div class="flashcard-label">Định nghĩa</div>
            <div class="flashcard-content">${escapeHtml(card.definition)}</div>
          </div>
        </div>
      </div>
      <div class="progress-bar" style="margin:20px 0">
        <div class="progress-bar-fill" style="width:${((state.currentCardIndex + 1) / cards.length) * 100}%"></div>
      </div>
      <div style="display:flex;gap:12px">
        <button class="btn btn-secondary" style="flex:1" onclick="prevReviewCard()" ${state.currentCardIndex === 0 ? 'disabled' : ''}>
          ${icons.back}
        </button>
        <button class="btn btn-primary" style="flex:1" onclick="nextReviewCard()">
          ${state.currentCardIndex === cards.length - 1 ? 'Xong' : 'Tiếp'}
        </button>
      </div>
    </div>
  </div>`;
}

function flipReviewCard() {
  state.currentFlashcardFlipped = !state.currentFlashcardFlipped;
  $$('.flashcard')[0]?.classList.toggle('flipped', state.currentFlashcardFlipped);
}

function prevReviewCard() {
  if (state.currentCardIndex > 0) {
    state.currentCardIndex--;
    state.currentFlashcardFlipped = false;
    renderReviewWrong();
  }
}

function nextReviewCard() {
  if (state.currentCardIndex < state.smartReviewCards.length - 1) {
    state.currentCardIndex++;
    state.currentFlashcardFlipped = false;
    renderReviewWrong();
  } else {
    showToast('Hoàn thành ôn tập!', 'success');
    navigate('test-result');
  }
}

// ============================================================
// SCREEN: SMART REVIEW CONFIG
// ============================================================
async function renderSmartReviewConfig() {
  const studySets = await dbGetAll('study_sets');
  const allCards = [];
  for (const ss of studySets) {
    const cards = await dbGetByIndex('flashcards', 'studySetId', ss.id);
    allCards.push(...cards);
  }
  const needsReview = allCards.filter(c => c.masteryLevel < 4);

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('home')">${icons.back}</button>
      <h1>Học thông minh</h1>
    </div>
    <div class="screen-content">
      <div class="card" style="margin-bottom:20px;text-align:center">
        <div style="font-size:14px;color:#6B7280">Thẻ cần ôn tập</div>
        <div style="font-size:40px;font-weight:800;color:#5B6CFF;margin:8px 0">${needsReview.length}</div>
        <div style="font-size:13px;color:#9CA3AF">Sử dụng spaced repetition để ôn tập hiệu quả</div>
      </div>

      <button class="btn btn-primary btn-lg" style="width:100%;margin-bottom:12px"
        onclick="startGlobalSmartReview()" ${needsReview.length === 0 ? 'disabled' : ''}>
        ${icons.brain} Bắt đầu học thông minh
      </button>

      ${studySets.length > 0 ? `
      <div class="divider"></div>
      <div class="section-header">
        <div class="section-title">Theo bộ thẻ</div>
      </div>
      ${studySets.map(ss => {
        const ssCards = allCards.filter(c => c.studySetId === ss.id);
        const ssNeeds = ssCards.filter(c => c.masteryLevel < 4);
        return `
        <div class="study-set-item" onclick="startSmartReviewForSet(${ss.id})" style="${ssNeeds.length === 0 ? 'opacity:0.5' : ''}">
          <div class="study-set-icon">${icons.book}</div>
          <div class="study-set-info">
            <div class="study-set-title">${escapeHtml(ss.title)}</div>
            <div class="study-set-meta">${ssNeeds.length} thẻ cần ôn</div>
          </div>
          ${icons.chevronRight}
        </div>`;
      }).join('')}
      ` : ''}
    </div>
    ${bottomNav()}
  </div>`;
}

async function startGlobalSmartReview() {
  const studySets = await dbGetAll('study_sets');
  const allCards = [];
  for (const ss of studySets) {
    const cards = await dbGetByIndex('flashcards', 'studySetId', ss.id);
    allCards.push(...cards);
  }
  const needsReview = shuffle(allCards.filter(c => c.masteryLevel < 4)).slice(0, 20);
  if (needsReview.length === 0) { showToast('Không có thẻ nào cần ôn!', 'success'); return; }
  state.smartReviewCards = needsReview;
  state.currentCardIndex = 0;
  state.currentFlashcardFlipped = false;
  navigate('smart-review');
}

async function startSmartReviewForSet(ssId) {
  const cards = await dbGetByIndex('flashcards', 'studySetId', ssId);
  const needsReview = shuffle(cards.filter(c => c.masteryLevel < 4)).slice(0, 20);
  if (needsReview.length === 0) { showToast('Không có thẻ nào cần ôn trong bộ này!', 'success'); return; }
  state.smartReviewCards = needsReview;
  state.currentCardIndex = 0;
  state.currentFlashcardFlipped = false;
  navigate('smart-review');
}

// ============================================================
// SCREEN: SMART REVIEW
// ============================================================
function renderSmartReview() {
  const cards = state.smartReviewCards;
  if (!cards || cards.length === 0) { navigate('smart-review-config'); return ''; }
  const card = cards[state.currentCardIndex];
  const flipped = state.currentFlashcardFlipped;

  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('smart-review-config')">${icons.back}</button>
      <div style="flex:1;text-align:center">
        <div style="font-size:13px;font-weight:700">${state.currentCardIndex + 1}/${cards.length}</div>
      </div>
    </div>
    <div class="screen-content">
      <div class="flashcard-container" onclick="flipSmartCard()">
        <div class="flashcard ${flipped ? 'flipped' : ''}">
          <div class="flashcard-face">
            <div class="flashcard-label">Thuật ngữ</div>
            <div class="flashcard-content">${escapeHtml(card.term)}</div>
            <div class="flashcard-hint">Nhấn để xem định nghĩa</div>
          </div>
          <div class="flashcard-face flashcard-back">
            <div class="flashcard-label">Định nghĩa</div>
            <div class="flashcard-content">${escapeHtml(card.definition)}</div>
            <div class="flashcard-hint">Đánh giá bên dưới</div>
          </div>
        </div>
      </div>

      <div class="progress-bar" style="margin:20px 0">
        <div class="progress-bar-fill" style="width:${((state.currentCardIndex + 1) / cards.length) * 100}%"></div>
      </div>

      <div style="display:flex;gap:8px">
        <button class="btn" style="flex:1;background:#FEECEC;color:#EF4444;font-weight:700" onclick="smartReviewRate(0)">
          Khó quá
        </button>
        <button class="btn" style="flex:1;background:#FFF4DB;color:#D97706;font-weight:700" onclick="smartReviewRate(1)">
          Nhớ được
        </button>
        <button class="btn" style="flex:1;background:#EAFBF0;color:#16A34A;font-weight:700" onclick="smartReviewRate(2)">
          Dễ quá
        </button>
      </div>
    </div>
  </div>`;
}

function flipSmartCard() {
  state.currentFlashcardFlipped = !state.currentFlashcardFlipped;
  $$('.flashcard')[0]?.classList.toggle('flipped', state.currentFlashcardFlipped);
}

async function smartReviewRate(rating) {
  const cards = state.smartReviewCards;
  const card = cards[state.currentCardIndex];
  const delta = rating === 2 ? 1 : rating === 1 ? 0 : -1;
  card.masteryLevel = Math.min(5, Math.max(0, card.masteryLevel + delta));
  card.timesReviewed = (card.timesReviewed || 0) + 1;
  if (rating === 2) card.timesCorrect = (card.timesCorrect || 0) + 1;
  card.lastReviewedAt = Date.now();
  await dbPut('flashcards', card);
  await incrementDailyCards();

  if (state.currentCardIndex < cards.length - 1) {
    state.currentCardIndex++;
    state.currentFlashcardFlipped = false;
    renderSmartReview();
  } else {
    showToast('Hoàn thành học thông minh!', 'success');
    navigate('smart-review-config');
  }
}

// ============================================================
// SCREEN: CREATE STUDY SET
// ============================================================
function renderCreateStudySet() {
  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('study-sets')">${icons.back}</button>
      <h1>Tạo bộ thẻ</h1>
    </div>
    <div class="screen-content">
      <div class="input-group" style="margin-bottom:16px">
        <label>Tiêu đề</label>
        <input type="text" class="input" id="newSetTitle" placeholder="Ví dụ: Từ vựng TOEIC Unit 1">
      </div>
      <div class="input-group" style="margin-bottom:16px">
        <label>Mô tả (tùy chọn)</label>
        <input type="text" class="input" id="newSetDesc" placeholder="Mô tả ngắn...">
      </div>
      <div class="input-group" style="margin-bottom:20px">
        <label>Thẻ (mỗi dòng: thuật ngữ | định nghĩa)</label>
        <textarea class="input" id="newSetCards" rows="10" placeholder="quack | tiếng vịt kêu
photosynthesis | quang hợp
mitochondria | ti thể
...Mỗi dòng: thuật ngữ | định nghĩa"></textarea>
      </div>
      <button class="btn btn-primary btn-lg" style="width:100%" onclick="createStudySet()">
        Tạo bộ thẻ
      </button>
    </div>
  </div>`;
}

async function createStudySet() {
  const title = $('#newSetTitle')?.value.trim();
  const desc = $('#newSetDesc')?.value.trim();
  const text = $('#newSetCards')?.value.trim();
  if (!title) { showToast('Nhập tiêu đề', 'error'); return; }
  if (!text) { showToast('Nhập ít nhất 1 thẻ', 'error'); return; }

  const lines = text.split('\n').filter(l => l.trim());
  const cards = [];
  for (const line of lines) {
    const parts = line.split('|').map(p => p.trim());
    if (parts.length >= 2 && parts[0] && parts[1]) {
      cards.push({ term: parts[0], definition: parts[1], itemType: 'term_definition', masteryLevel: 0, timesReviewed: 0, timesCorrect: 0, isStarred: false, createdAt: Date.now() });
    }
  }
  if (cards.length === 0) { showToast('Không đọc được thẻ nào. Dùng định dạng: thuật ngữ | định nghĩa', 'error'); return; }

  const studySet = {
    title, description: desc, cardCount: cards.length,
    sourceType: 'manual', studySetType: 'term_definition',
    isPinned: false, isFavorite: false, createdAt: Date.now(), updatedAt: Date.now(),
  };

  const ssId = await dbPut('study_sets', studySet);
  for (const card of cards) { card.studySetId = ssId; await dbPut('flashcards', card); }
  showToast(`Đã tạo bộ thẻ với ${cards.length} thẻ!`, 'success');
  navigate('study-sets');
}

// ============================================================
// SCREEN: ABOUT
// ============================================================
function renderAbout() {
  return `
  <div class="screen">
    <div class="screen-header">
      <button class="btn btn-icon" onclick="navigate('home')">${icons.back}</button>
      <h1>Giới thiệu</h1>
    </div>
    <div class="screen-content">
      <div style="text-align:center;padding:20px 0">
        <div style="width:80px;height:80px;border-radius:20px;background:linear-gradient(135deg,#5B6CFF,#8B95FF);display:flex;align-items:center;justify-content:center;margin:0 auto 12px;font-size:36px">📚</div>
        <div style="font-size:24px;font-weight:800;color:#5B6CFF">Kquiz</div>
        <div style="font-size:14px;color:#6B7280">Phiên bản Web (PWA)</div>
      </div>
      <div class="card">
        <p style="font-size:14px;line-height:1.7;color:#6B7280">
          Kquiz là app học quiz thông minh, hỗ trợ tạo câu hỏi từ file văn bản, hình ảnh, và PDF.
        </p>
        <div class="divider"></div>
        <p style="font-size:13px;color:#9CA3AF">
          Phiên bản PWA có thể cài đặt trên iPhone/Android, hoạt động offline, lưu dữ liệu cục bộ.
        </p>
      </div>
    </div>
  </div>`;
}

// ============================================================
// SCREEN: PROCESSING
// ============================================================
function renderProcessing() {
  return `
  <div class="screen">
    <div class="screen-header">
      <h1>Đang xử lý</h1>
    </div>
    <div class="screen-content">
      <div style="text-align:center;padding:40px 0">
        <div class="loading-dots"><span></span><span></span><span></span></div>
        <p style="color:#6B7280;margin-top:16px">Đang trích xuất nội dung...</p>
        <p style="font-size:13px;color:#9CA3AF;margin-top:8px">${escapeHtml(state.extractedContent?.fileName || '')}</p>
      </div>
    </div>
  </div>`;
}

// ============================================================
// EVENT LISTENERS
// ============================================================
function attachEventListeners() {
  const dropZone = $('#dropZone');
  if (dropZone) {
    dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('drag-over'); });
    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('drag-over'));
    dropZone.addEventListener('drop', handleFileDrop);
  }
}

// ============================================================
// INIT
// ============================================================
async function init() {
  try {
    await initDB();
    await loadUserData();
    await loadDailyStats();
    navigate('home');

    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('sw.js').catch(() => {});
    }
  } catch (err) {
    $('#app').innerHTML = `<div style="padding:40px;text-align:center;background:#F6F8FC;min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center">
      <div style="font-size:48px;margin-bottom:12px">⚠️</div>
      <h2 style="color:#1F2937;margin-bottom:8px">Lỗi khởi tạo</h2>
      <p style="color:#6B7280;margin-bottom:16px">${err.message}</p>
      <button onclick="location.reload()" style="padding:12px 24px;background:#5B6CFF;color:white;border:none;border-radius:12px;font-size:15px;font-weight:600;cursor:pointer">Thử lại</button>
    </div>`;
  }
}

init();
