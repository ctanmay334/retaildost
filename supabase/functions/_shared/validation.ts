/**
 * validation.ts - Centralized, production-grade validation, repair, and constraint engine 
 * for RetailDost AI outputs. Supports Zod, confidence evaluation, price repair, and label normalization.
 */

import { z } from 'https://esm.sh/zod@3';

// ==========================================
// 1. Custom Validation Exceptions
// ==========================================

export class AiValidationError extends Error {
  public details: any;
  public rawData: any;

  constructor(message: string, details: any, rawData: any) {
    super(message);
    this.name = "AiValidationError";
    this.details = details;
    this.rawData = rawData;
  }
}

export class AiLowConfidenceException extends Error {
  public confidence: number;
  public threshold: number;

  constructor(message: string, confidence: number, threshold: number) {
    super(message);
    this.name = "AiLowConfidenceException";
    this.confidence = confidence;
    this.threshold = threshold;
  }
}

// ==========================================
// 2. Shared Zod Schemas for All Models
// ==========================================

// --- OCR Invoice Schemas ---
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

// --- OCR Diary Schemas ---
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

// --- NLP Intent Schemas ---
export const KhataNlpResponseSchema = z.object({
  intent: z.enum(["debit", "credit", "query", "unknown"]).default("unknown"),
  customer_name: z.string().default("Unknown"),
  amount: z.number().nonnegative().default(0.0),
  confidence: z.number().min(0.0).max(1.0).default(1.0)
});

// --- Analytics Insight Schemas ---
export const AnalyticsAlertSchema = z.object({
  item_name: z.string(),
  current_stock: z.number().nonnegative(),
  minimum_required: z.number().nonnegative()
});

export const AnalyticsInsightResponseSchema = z.object({
  total_sales_trend: z.string(),
  average_margin: z.number().default(0.0),
  highest_margin_category: z.string().default("General"),
  optimization_tip: z.string(),
  low_stock_alerts: z.array(AnalyticsAlertSchema).default([]),
  confidence: z.number().min(0.0).max(1.0).default(1.0)
});

export type ExtractedItem = z.infer<typeof ExtractedItemSchema>;
export type OcrResponse = z.infer<typeof OcrResponseSchema>;
export type ExtractedDiaryItem = z.infer<typeof ExtractedDiaryItemSchema>;
export type OcrDiaryResponse = z.infer<typeof OcrDiaryResponseSchema>;
export type KhataNlpResponse = z.infer<typeof KhataNlpResponseSchema>;
export type AnalyticsInsightResponse = z.infer<typeof AnalyticsInsightResponseSchema>;

// ==========================================
// 3. Intelligent Recovery & Repair Rules
// ==========================================

/**
 * Normalizes unit labels to standard retail labels
 */
export function normalizeUnitLabel(unit: string | undefined | null): string {
  if (!unit) return "pcs";
  const cleaned = unit.trim().toLowerCase();
  
  if (["pcs", "pc", "piece", "pieces", "unit", "units"].includes(cleaned)) return "pcs";
  if (["kg", "kgs", "kilogram", "kilograms"].includes(cleaned)) return "kg";
  if (["g", "gm", "gms", "gram", "grams"].includes(cleaned)) return "g";
  if (["ml", "mls", "milliliters", "millilitres"].includes(cleaned)) return "ml";
  if (["l", "ltr", "ltrs", "liter", "liters", "litre", "litres"].includes(cleaned)) return "L";
  if (["pkt", "pkts", "packet", "packets", "pack", "packs"].includes(cleaned)) return "pkt";
  if (["bottle", "bottles", "bot"].includes(cleaned)) return "bottle";
  if (["box", "boxes"].includes(cleaned)) return "box";
  
  return cleaned;
}

/**
 * Repairs prices and quantities that were formatted as strings or have negative coordinates.
 */
export function repairPricingAndQuantity(item: any): any {
  const repaired = { ...item };

  // Helper to force float conversion
  const forceNumber = (val: any, defaultVal = 0): number => {
    if (typeof val === "number") return isNaN(val) ? defaultVal : val;
    if (typeof val === "string") {
      const parsed = parseFloat(val.replace(/[^\d.-]/g, ""));
      return isNaN(parsed) ? defaultVal : parsed;
    }
    return defaultVal;
  };

  // Coerce variables
  repaired.quantity = Math.max(1, Math.round(forceNumber(repaired.quantity, 1)));
  repaired.quantity_sold = Math.max(1, Math.round(forceNumber(repaired.quantity_sold, 1)));
  
  const rawPrice = forceNumber(repaired.price !== undefined ? repaired.price : 0);
  repaired.price = Math.abs(rawPrice);

  const rawMrp = forceNumber(repaired.mrp !== undefined ? repaired.mrp : 0);
  repaired.mrp = Math.abs(rawMrp);

  const rawCost = forceNumber(repaired.cost_price !== undefined ? repaired.cost_price : 0);
  repaired.cost_price = Math.abs(rawCost);

  // Business Constraint Check: MRP can never be lower than cost price
  if (repaired.cost_price > repaired.mrp) {
    console.warn(`[Validator] Business logic violated: cost_price (${repaired.cost_price}) > mrp (${repaired.mrp}). Enforcing MRP = cost_price.`);
    repaired.mrp = repaired.cost_price;
  }

  // Enforce confidence range defaults
  const conf = forceNumber(repaired.confidence !== undefined ? repaired.confidence : 1.0);
  repaired.confidence = Math.max(0.0, Math.min(1.0, conf));

  return repaired;
}

// ==========================================
// 4. Retry-Safe Parser & Validator Engine
// ==========================================

/**
 * Coerces, repairs, and validates AI response data against a target Zod schema.
 * Supports structural checks, business rules repair, and minimum confidence thresholds.
 */
export function validateAndRepairResponse<T>(
  schema: z.ZodSchema<T>,
  data: any,
  options: {
    minConfidenceThreshold?: number;
    tagLowConfidenceItems?: boolean;
  } = {}
): T {
  const minConfidence = options.minConfidenceThreshold ?? 0.60;
  
  if (!data || typeof data !== "object") {
    throw new AiValidationError("AI response content is not an object or array.", { root: "Expected JSON object" }, data);
  }

  let repairedData = { ...data };

  // Repair individual items in array payloads
  if (Array.isArray(repairedData.items)) {
    repairedData.items = repairedData.items.map((item: any) => {
      let repair = repairPricingAndQuantity(item);
      
      if (repair.unit_label !== undefined) {
        repair.unit_label = normalizeUnitLabel(repair.unit_label);
      }
      
      // Auto-tag low-confidence items if specified
      if (options.tagLowConfidenceItems && repair.confidence < minConfidence) {
        repair.low_confidence = true;
      }
      
      return repair;
    });
  } else {
    // If not an array, repair standard properties
    repairedData = repairPricingAndQuantity(repairedData);
  }

  // Parse against Zod schema
  const parsed = schema.safeParse(repairedData);
  if (!parsed.success) {
    throw new AiValidationError(
      "Zod schema validation failed after applying repair rules.",
      parsed.error.format(),
      data
    );
  }

  const result: any = parsed.data;

  // Confidence Validation Check (e.g. NLP Intent confidence threshold check)
  const confidence = result.confidence !== undefined ? result.confidence : 1.0;
  if (confidence < minConfidence && !options.tagLowConfidenceItems) {
    throw new AiLowConfidenceException(
      `AI confidence score of ${confidence.toFixed(2)} falls below acceptable threshold of ${minConfidence.toFixed(2)}.`,
      confidence,
      minConfidence
    );
  }

  return result;
}
