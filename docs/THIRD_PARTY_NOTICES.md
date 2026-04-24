# Third Party Notices

KQuiz Web is written from scratch in `docs/`. It does not copy the previous PWA code.

Optional advanced features lazy-load permissively licensed browser libraries:

- PDF.js / pdfjs-dist - Apache-2.0 - PDF text extraction.
- Tesseract.js - Apache-2.0 - OCR for image import.
- pdf-lib - MIT - PDF creation/export.
- qrcodejs - MIT - QR code generation.
- jsQR - Apache-2.0/MIT-distributed ecosystem references - QR scanning from camera/canvas.
- mammoth.js - BSD-2-Clause - DOCX text extraction.
- Google Publisher Tag - Google Ad Manager web rewarded ads integration.

The app does not embed private AI keys. AI Pro requires a user-configured proxy endpoint.
Rewarded ads require a user-configured Google Ad Manager ad unit path; no ad account secret is stored in this frontend.
