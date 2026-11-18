// What a form action returns when it did not succeed. `fields` holds one message per input name.
// A successful action redirects instead of returning.
export type FormState = {
  error?: string;
  fields?: Record<string, string>;
} | null;

// The same shape a form action has after the page has bound its leading arguments (ids).
export type FormAction = (previous: FormState, formData: FormData) => Promise<FormState>;
