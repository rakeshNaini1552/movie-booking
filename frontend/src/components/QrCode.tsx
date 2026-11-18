import { qrMatrix } from "@/lib/tickets/qr";

type QrCodeProps = {
  payload: string;
  label: string;
};

// Always dark squares on white with a quiet margin: scanners need that contrast, whatever the theme.
export default function QrCode({ payload, label }: QrCodeProps) {
  const { size, path } = qrMatrix(payload);
  const margin = 2;
  return (
    <svg
      role="img"
      aria-label={label}
      viewBox={`${-margin} ${-margin} ${size + margin * 2} ${size + margin * 2}`}
      shapeRendering="crispEdges"
      className="h-44 w-44 rounded-xl bg-white"
    >
      <path d={path} fill="#000" />
    </svg>
  );
}
