// The look of a button, shared by <Button> (a real button) and <ButtonLink> (a link that looks like one).
export type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";
export type ButtonSize = "sm" | "md" | "lg";

const base =
  "inline-flex items-center justify-center gap-2 rounded-full font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50";

const variants: Record<ButtonVariant, string> = {
  primary: "bg-brand text-white shadow-lg shadow-brand/25 hover:bg-brand-strong",
  secondary: "border border-line bg-raised text-zinc-100 hover:border-zinc-500 hover:bg-line",
  ghost: "text-zinc-200 hover:bg-raised hover:text-white",
  danger: "border border-red-500/60 bg-red-950/40 text-red-200 hover:bg-red-900/60",
};

const sizes: Record<ButtonSize, string> = {
  sm: "px-3.5 py-1.5 text-sm",
  md: "px-5 py-2.5 text-sm",
  lg: "px-7 py-3.5 text-base",
};

export function buttonStyles({
  variant = "primary",
  size = "md",
  className = "",
}: { variant?: ButtonVariant; size?: ButtonSize; className?: string } = {}): string {
  return `${base} ${variants[variant]} ${sizes[size]} ${className}`.trim();
}
