const GPT_SRC = "https://securepubads.g.doubleclick.net/tag/js/gpt.js";

let gptLoading;
let servicesEnabled = false;
let listenersAttached = false;
const slots = new Map();

function loadGpt() {
  if (window.googletag?.apiReady) return Promise.resolve();
  if (gptLoading) return gptLoading;
  gptLoading = new Promise((resolve, reject) => {
    window.googletag = window.googletag || { cmd: [] };
    const existing = document.querySelector(`script[src="${GPT_SRC}"]`);
    if (existing) {
      existing.addEventListener("load", resolve, { once: true });
      existing.addEventListener("error", reject, { once: true });
      return;
    }
    const script = document.createElement("script");
    script.async = true;
    script.src = GPT_SRC;
    script.onload = resolve;
    script.onerror = reject;
    document.head.appendChild(script);
  });
  return gptLoading;
}

function normalizeAdUnitPath(config, key = "adUnitPath") {
  const unit = String(config?.[key] || "").trim();
  if (!unit) return "";
  if (unit.startsWith("/")) return unit;
  const network = String(config?.networkCode || "").trim();
  return network ? `/${network}/${unit}` : unit;
}

function parseSizes(value) {
  if (Array.isArray(value)) return value;
  const raw = String(value || "320x50,300x250").trim();
  const sizes = raw
    .split(",")
    .map((item) => item.trim().toLowerCase())
    .filter(Boolean)
    .map((item) => {
      if (item === "fluid") return "fluid";
      const match = item.match(/^(\d{2,4})\s*x\s*(\d{2,4})$/);
      return match ? [Number(match[1]), Number(match[2])] : null;
    })
    .filter(Boolean);
  return sizes.length ? sizes : [[320, 50]];
}

function attachSlotListeners() {
  if (listenersAttached) return;
  listenersAttached = true;
  window.googletag.pubads().addEventListener("slotRenderEnded", (event) => {
    const id = event.slot?.getSlotElementId?.();
    if (!id) return;
    const node = document.getElementById(id);
    const wrapper = node?.closest?.(".kquiz-ad-slot");
    if (!node || !wrapper) return;
    wrapper.classList.toggle("is-empty", Boolean(event.isEmpty));
    wrapper.classList.toggle("is-filled", !event.isEmpty);
    node.dataset.adStatus = event.isEmpty ? "empty" : "filled";
  });
}

function markAdSlot(slotId, status, message = "") {
  const node = document.getElementById(slotId);
  if (!node) return;
  node.dataset.adStatus = status;
  const statusNode = node.querySelector(".ad-status");
  if (statusNode && message) statusNode.textContent = message;
}

export async function destroyAdSlots() {
  if (!slots.size || !window.googletag?.cmd) return;
  const existing = [...slots.values()].filter(Boolean);
  slots.clear();
  await new Promise((resolve) => {
    window.googletag.cmd.push(() => {
      try {
        if (existing.length) window.googletag.destroySlots(existing);
      } catch {}
      resolve();
    });
  });
}

export async function renderAdSlots(config, placements = []) {
  if (!config?.enabled || !config?.bannerEnabled) return [];
  const adUnitPath = normalizeAdUnitPath(config, "bannerAdUnitPath") || normalizeAdUnitPath(config, "adUnitPath");
  if (!adUnitPath) {
    placements.forEach((placement) => markAdSlot(placement.slotId, "missing-config", "Chưa cấu hình banner ad unit."));
    return placements.map((placement) => ({ slotId: placement.slotId, ok: false, reason: "missing-config" }));
  }
  await loadGpt();
  const sizes = parseSizes(config.bannerSizes);

  return Promise.all(placements.map((placement) => new Promise((resolve) => {
    window.googletag.cmd.push(() => {
      const node = document.getElementById(placement.slotId);
      if (!node) {
        resolve({ slotId: placement.slotId, ok: false, reason: "missing-div" });
        return;
      }
      try {
        if (slots.has(placement.slotId)) {
          window.googletag.destroySlots([slots.get(placement.slotId)]);
          slots.delete(placement.slotId);
        }
        attachSlotListeners();
        if (config.collapseEmptyDivs) window.googletag.pubads().collapseEmptyDivs();
        if (config.centerAds !== false) window.googletag.pubads().setCentering(true);
        const slot = window.googletag.defineSlot(adUnitPath, placement.sizes || sizes, placement.slotId);
        if (!slot) {
          markAdSlot(placement.slotId, "unsupported", "Không tạo được slot quảng cáo.");
          resolve({ slotId: placement.slotId, ok: false, reason: "unsupported-slot" });
          return;
        }
        slot.addService(window.googletag.pubads());
        if (placement.targeting) {
          Object.entries(placement.targeting).forEach(([key, value]) => slot.setTargeting(key, String(value)));
        }
        slots.set(placement.slotId, slot);
        if (!servicesEnabled) {
          window.googletag.enableServices();
          servicesEnabled = true;
        }
        markAdSlot(placement.slotId, "loading", "Đang tải quảng cáo...");
        window.googletag.display(placement.slotId);
        resolve({ slotId: placement.slotId, ok: true });
      } catch (error) {
        console.warn("KQuiz banner ad failed", error);
        markAdSlot(placement.slotId, "error", "Không tải được quảng cáo.");
        resolve({ slotId: placement.slotId, ok: false, reason: "error" });
      }
    });
  })));
}

export async function requestRewardedAd(config, context = {}) {
  const adUnitPath = normalizeAdUnitPath(config);
  if (!config?.enabled || !adUnitPath) {
    return { granted: false, reason: "missing-config" };
  }

  await loadGpt();

  return new Promise((resolve) => {
    let resolved = false;
    let granted = false;
    let rewardedSlot = null;
    const done = (result) => {
      if (resolved) return;
      resolved = true;
      try {
        if (rewardedSlot) window.googletag.destroySlots([rewardedSlot]);
      } catch {}
      resolve(result);
    };

    window.googletag.cmd.push(() => {
      const format = window.googletag.enums?.OutOfPageFormat?.REWARDED;
      rewardedSlot = window.googletag.defineOutOfPageSlot(adUnitPath, format);
      if (!rewardedSlot) {
        done({ granted: false, reason: "unsupported-page" });
        return;
      }

      rewardedSlot.addService(window.googletag.pubads());
      window.googletag.pubads().addEventListener("rewardedSlotReady", (event) => {
        if (event.slot !== rewardedSlot || resolved) return;
        window.dispatchEvent(new CustomEvent("kquiz:rewarded-ready", { detail: { context } }));
        event.makeRewardedVisible();
      });
      window.googletag.pubads().addEventListener("rewardedSlotGranted", (event) => {
        if (event.slot !== rewardedSlot || resolved) return;
        granted = true;
        window.dispatchEvent(new CustomEvent("kquiz:rewarded-granted", { detail: { context, payload: event.payload } }));
      });
      window.googletag.pubads().addEventListener("rewardedSlotClosed", (event) => {
        if (event.slot !== rewardedSlot || resolved) return;
        done({ granted, reason: granted ? "rewarded" : "closed" });
      });
      if (!servicesEnabled) {
        window.googletag.enableServices();
        servicesEnabled = true;
      }
      window.googletag.display(rewardedSlot);

      setTimeout(() => done({ granted: false, reason: "timeout" }), Number(config.timeoutMs || 15000));
    });
  });
}
