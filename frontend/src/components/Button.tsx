import type { ButtonHTMLAttributes } from "react";
import { type ButtonSize, type ButtonVariant, buttonStyles } from "@/lib/ui/button-styles";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
};

// type defaults to "button" so it never submits a form by accident; pass type="submit" for forms.
export default function Button({
  variant = "primary",
  size = "md",
  type = "button",
  className = "",
  ...props
}: ButtonProps) {
  return <button type={type} className={buttonStyles({ variant, size, className })} {...props} />;
}
