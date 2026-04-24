const GPT_SRC = "https://securepubads.g.doubleclick.net/tag/js/gpt.js";

let gptLoading;

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

function normalizeAdUnitPath(config) {
  const unit = String(config?.adUnitPath || "").trim();
  if (!unit) return "";
  if (unit.startsWith("/")) return unit;
  const network = String(config?.networkCode || "").trim();
  return network ? `/${network}/${unit}` : unit;
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
      window.googletag.enableServices();
      window.googletag.display(rewardedSlot);

      setTimeout(() => done({ granted: false, reason: "timeout" }), Number(config.timeoutMs || 15000));
    });
  });
}
