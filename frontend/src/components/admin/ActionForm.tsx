"use client";

import { createContext, type ReactNode, useActionState, useContext } from "react";
import Button from "@/components/Button";
import type { FormAction, FormState } from "@/lib/admin/form-state";
import type { ButtonVariant } from "@/lib/ui/button-styles";

type Submitted = { state: FormState; values: Record<string, string> };

// Lets each <Field> inside the form read its own error and the value that was last submitted.
const SubmittedContext = createContext<Submitted>({ state: null, values: {} });

type ActionFormProps = {
  // A server action. The page passes it in, so this component imports no server code.
  action: FormAction;
  submitLabel: string;
  pendingLabel: string;
  submitVariant?: ButtonVariant;
  children?: ReactNode;
  className?: string;
};

// A form that runs a server action and shows what it returns. useActionState gives three things:
// the last result of the action, a function to put in <form action>, and whether it is running.
export default function ActionForm({
  action,
  submitLabel,
  pendingLabel,
  submitVariant = "primary",
  children,
  className = "flex flex-col gap-5",
}: ActionFormProps) {
  const [submitted, formAction, pending] = useActionState(
    async (_previous: Submitted, formData: FormData): Promise<Submitted> => {
      const state = await action(null, formData);
      // React clears a form after its action runs. Keeping what was typed lets the fields show
      // it again next to the error instead of coming back empty.
      const values: Record<string, string> = {};
      for (const [name, value] of formData) if (typeof value === "string") values[name] = value;
      return { state, values };
    },
    { state: null, values: {} },
  );

  return (
    <form action={formAction} className={className} noValidate>
      <SubmittedContext value={submitted}>{children}</SubmittedContext>
      {submitted.state?.error && (
        <p role="alert" className="text-sm text-red-300">
          {submitted.state.error}
        </p>
      )}
      <div>
        <Button type="submit" variant={submitVariant} disabled={pending}>
          {pending ? pendingLabel : submitLabel}
        </Button>
      </div>
    </form>
  );
}

type FieldProps = {
  name: string;
  label: string;
  type?: "text" | "number" | "date" | "datetime-local";
  defaultValue?: string | number | null;
  required?: boolean;
  hint?: string;
  // "textarea" for long text, "select" with options for a fixed list.
  as?: "input" | "textarea" | "select";
  options?: readonly { value: string; label: string }[];
  step?: string;
  min?: string;
  max?: string;
};

const controlClass =
  "border-line bg-night focus:border-brand-soft w-full rounded-xl border px-4 py-2.5 text-sm placeholder:text-zinc-500 aria-invalid:border-red-400";

// One labelled input. Must be used inside <ActionForm>.
export function Field({
  name,
  label,
  type = "text",
  defaultValue,
  required,
  hint,
  as = "input",
  options = [],
  step,
  min,
  max,
}: FieldProps) {
  const { state, values } = useContext(SubmittedContext);
  const error = state?.fields?.[name];
  const value = values[name] ?? (defaultValue == null ? "" : String(defaultValue));
  const describedBy =
    [hint && `${name}-hint`, error && `${name}-error`].filter(Boolean).join(" ") || undefined;
  const shared = {
    id: name,
    name,
    required,
    defaultValue: value,
    "aria-invalid": error ? true : undefined,
    "aria-describedby": describedBy,
    className: controlClass,
  };

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={name} className="text-sm font-medium">
        {label}
        {required && <span className="text-zinc-400"> (required)</span>}
      </label>
      {as === "textarea" && <textarea rows={4} {...shared} />}
      {as === "select" && (
        // key: a <select> only reads defaultValue when it is created, so it is recreated when the value changes.
        <select key={value} {...shared}>
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      )}
      {as === "input" && <input type={type} step={step} min={min} max={max} {...shared} />}
      {hint && (
        <p id={`${name}-hint`} className="text-xs text-zinc-400">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${name}-error`} className="text-xs text-red-300">
          {error}
        </p>
      )}
    </div>
  );
}
