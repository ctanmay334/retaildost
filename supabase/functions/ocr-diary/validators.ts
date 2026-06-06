import { z } from 'https://esm.sh/zod@3';

export const OcrDiaryInputSchema = z.object({
  image_path: z.string({ required_error: "image_path is required" }),
  bucket_name: z.string().default('retaildost-diary-images'),
  idempotency_key: z.string({ required_error: "idempotency_key is required" })
});

export type OcrDiaryInput = z.infer<typeof OcrDiaryInputSchema>;

export const ExtractedDiaryItemSchema = z.object({
  product_name: z.string({ required_error: "product_name is required" }).trim().min(1),
  quantity_sold: z.number().nonnegative().default(1),
  price: z.number().nonnegative().default(0),
  confidence: z.number().min(0).max(1).default(1.0),
  canonical_name: z.string().nullable().default(null),
  low_confidence: z.boolean().default(false)
});

export const OcrDiaryResponseSchema = z.object({
  items: z.array(ExtractedDiaryItemSchema)
});

export type OcrDiaryResponse = z.infer<typeof OcrDiaryResponseSchema>;
