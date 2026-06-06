import { z } from 'https://esm.sh/zod@3';

export const OcrInputSchema = z.object({
  image_path: z.string({ required_error: "image_path is required" }),
  bucket_name: z.string().default('retaildost-invoice-images'),
  idempotency_key: z.string({ required_error: "idempotency_key is required" })
});

export type OcrInput = z.infer<typeof OcrInputSchema>;

export const ExtractedItemSchema = z.object({
  item_name: z.string({ required_error: "item_name is required" }).trim().min(1),
  quantity: z.number().nonnegative().default(1),
  unit_label: z.string().default('pcs'),
  mrp: z.number().nonnegative().default(0),
  cost_price: z.number().nonnegative().default(0),
  batch_no: z.string().nullable().default(null),
  expiry_date: z.string().nullable().default(null),
  confidence: z.number().min(0).max(1).default(1.0)
});

export const OcrResponseSchema = z.object({
  items: z.array(ExtractedItemSchema)
});

export type OcrResponse = z.infer<typeof OcrResponseSchema>;
