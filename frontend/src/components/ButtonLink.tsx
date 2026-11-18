import Link from "next/link";
import type { ComponentProps } from "react";
import { type ButtonSize, type ButtonVariant, buttonStyles } from "@/lib/ui/button-styles";

type ButtonLinkProps = ComponentProps<typeof Link> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
};

// A navigation link that looks like a button. Navigation stays a link (right-click, open in new tab).
export default function ButtonLink({
  variant = "primary",
  size = "md",
  className = "",
  ...props
}: ButtonLinkProps) {
  return <Link className={buttonStyles({ variant, size, className })} {...props} />;
}
