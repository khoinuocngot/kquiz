export const ROUTE_GROUPS = {
  home: ["home"],
  create: ["create-hub", "quick-import", "import-file", "processing", "quiz-config", "quiz", "result", "smart-scan", "import-studyset"],
  study: ["study-hub", "study-sets", "study-detail", "folders", "folder-detail"],
  review: ["review-hub", "smart-review", "flashcard", "learn", "learn-result", "test-config", "test", "test-result", "review-wrong", "history"],
  tools: ["tools-hub", "about"]
};

export const HUBS = [
  { key: "home", route: "home", label: "Home", icon: "home" },
  { key: "create", route: "create-hub", label: "Tạo", icon: "plus" },
  { key: "study", route: "study-hub", label: "Học", icon: "learn" },
  { key: "review", route: "review-hub", label: "Ôn", icon: "spark" },
  { key: "tools", route: "tools-hub", label: "Công cụ", icon: "gear" }
];

export const PRO_FEATURES = {
  smart_scan: {
    label: "Smart Scan Pro",
    description: "Xem quảng cáo có tặng thưởng để mở ghép nhiều ảnh thành PDF scan."
  },
  ai_pro: {
    label: "AI Pro",
    description: "Xem quảng cáo có tặng thưởng để dùng endpoint AI proxy cho file hiện tại."
  },
  pdf_pro: {
    label: "PDF Pro",
    description: "Xem quảng cáo có tặng thưởng để xuất đề PDF nâng cao."
  },
  review_wrong: {
    label: "Ôn sai siêu tốc Pro",
    description: "Xem quảng cáo có tặng thưởng để ôn lại các câu vừa làm sai."
  }
};
