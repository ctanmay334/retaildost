import { z } from 'https://esm.sh/zod@3';

export const KhataNlpInputSchema = z.object({
  raw_input: z.string({ required_error: "raw_input is required" }).trim().min(1),
  idempotency_key: z.string().optional()
});

export type KhataNlpInput = z.infer<typeof KhataNlpInputSchema>;

export const KhataNlpResponseSchema = z.object({
  intent: z.enum(["debit", "credit", "query", "unknown"]).default("unknown"),
  customer_name: z.string().default("Unknown"),
  amount: z.number().nonnegative().default(0.0),
  confidence: z.number().min(0.0).max(1.0).default(1.0)
});

export type KhataNlpResponse = z.infer<typeof KhataNlpResponseSchema>;
