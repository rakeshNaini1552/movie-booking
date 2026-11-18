import QRCode from "qrcode";

export type QrMatrix = { size: number; path: string };

// Turns the ticket's QR payload into one SVG path (a square per dark module). Drawing the path
// ourselves keeps it a normal React element: no HTML string from a library is injected into the page.
export function qrMatrix(payload: string): QrMatrix {
  const { modules } = QRCode.create(payload, { errorCorrectionLevel: "M" });
  const { size, data } = modules;
  let path = "";
  for (let row = 0; row < size; row++) {
    for (let column = 0; column < size; column++) {
      if (data[row * size + column]) path += `M${column} ${row}h1v1h-1z`;
    }
  }
  return { size, path };
}
