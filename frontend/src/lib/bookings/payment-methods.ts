// payment-service is a mock: it has no real card form. It accepts two fixed tokens, one that
// approves and one that declines. The browser only ever sends the method id ("approve" /
// "decline"); the token is looked up on the server (see actions.ts).
export const PAYMENT_METHODS = [
  { id: "approve", label: "Test card that approves", hint: "The payment goes through." },
  {
    id: "decline",
    label: "Test card that declines",
    hint: "Shows what a declined card looks like.",
  },
] as const;

export type PaymentMethodId = (typeof PAYMENT_METHODS)[number]["id"];

export const PAYMENT_METHOD_IDS = PAYMENT_METHODS.map((method) => method.id) as [
  PaymentMethodId,
  ...PaymentMethodId[],
];

// The mock gateway's contract (MockPaymentGateway in payment-service).
export const PAYMENT_TOKENS: Record<PaymentMethodId, string> = {
  approve: "tok_success",
  decline: "tok_decline",
};
